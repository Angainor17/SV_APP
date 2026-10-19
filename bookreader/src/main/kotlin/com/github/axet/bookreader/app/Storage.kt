package com.github.axet.bookreader.app

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.github.axet.androidlibrary.app.AlarmManager
import com.github.axet.androidlibrary.app.FileTypeDetector
import com.github.axet.androidlibrary.app.RarSAF
import com.github.axet.androidlibrary.app.Storage as AxetStorage
import com.github.axet.androidlibrary.net.HttpClient
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter
import com.github.axet.androidlibrary.widgets.WebViewCustom
import com.github.axet.bookreader.R
import com.github.axet.bookreader.widgets.FBReaderView
import com.github.axet.wget.SpeedInfo
import de.innosystec.unrar.Archive
import de.innosystec.unrar.NativeStorage
import de.innosystec.unrar.rarfile.FileHeader
import org.apache.commons.io.IOUtils
import org.geometerplus.fbreader.book.Book as FbBook
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.fbreader.FBView
import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.fbreader.formats.FormatPlugin
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.image.ZLFileImageProxy
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.image.ZLStreamImage
import org.geometerplus.zlibrary.core.util.SystemInfo
import org.geometerplus.zlibrary.core.view.ZLPaintContext
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.HashMap
import java.util.Locale
import java.util.TreeMap
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Storage(context: Context) : AxetStorage(context) {
    companion object {
        const val MD5_SIZE = 32
        const val JSON_EXT = "json"
        const val ZIP_EXT = "zip"

        @JvmField
        var TAG: String = Storage::class.java.name

        @JvmField
        val SAF_RW: Int = AxetStorage.SAF_RW

        /**
         * Storage permissions for read-write access.
         * Updated for Android 13+ (API 33+) granular media permissions.
         */
        @JvmField
        val PERMISSIONS_RW: Array<String> = PermissionHelper.STORAGE_PERMISSIONS_RW

        /**
         * Storage permissions for read-only access.
         * Updated for Android 13+ (API 33+) granular media permissions.
         */
        @JvmField
        val PERMISSIONS_RO: Array<String> = PermissionHelper.STORAGE_PERMISSIONS_RO

        /**
         * Check if storage permissions are granted.
         * Updated for Android 13+ (API 33+) granular media permissions.
         */
        // Not @JvmStatic: superclass already declares a static permitted(...) with the same
        // signature, so @JvmStatic would trigger an "accidental override" error. There are no
        // current Java callers, so the companion function is sufficient.
        fun permitted(context: Context, permissions: Array<String>): Boolean {
            return PermissionHelper.hasStoragePermissions(context, false)
        }

        @JvmStatic
        fun supported(): Array<FileTypeDetector.Detector> {
            return arrayOf<FileTypeDetector.Detector>(
                FileTypeDetector.FileFB2(), FileTypeDetector.FileFB2Zip(),
                FileTypeDetector.FileEPUB(), FileTypeDetector.FileHTML(), FileTypeDetector.FileHTMLZip(),
                FileTypeDetector.FilePDF(), FileTypeDetector.FileDjvu(), FileTypeDetector.FileRTF(),
                FileTypeDetector.FileRTFZip(), FileTypeDetector.FileDoc(), FileTypeDetector.FileMobi(),
                FileTypeDetector.FileTxt(), FileTypeDetector.FileTxtZip(), FileCbz(), FileCbr()
            )
        }

        @JvmStatic
        fun getPlugin(info: Info, b: FBook): FormatPlugin {
            val c = PluginCollection.Instance(info)
            val f = BookUtil.fileByBook(b.book!!)
            return when (f.getExtension()) {
                PDFPlugin.EXT -> PDFPlugin.create(info)
                DjvuPlugin.EXT -> DjvuPlugin.create(info)
                ComicsPlugin.CBZ, ComicsPlugin.CBR -> ComicsPlugin(info)
                else -> try {
                    BookUtil.getPlugin(c, b.book!!)
                } catch (e: BookReadingException) {
                    throw RuntimeException(e)
                }
            }
        }

        @JvmStatic
        fun getAndroidId(context: Context): String {
            var id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (id == null || id.isEmpty())
                id = Build.SERIAL
            return id
        }

        @JvmStatic
        fun renderView(v: View): Bitmap {
            val m = v.context.resources.displayMetrics
            val w = (720 * m.density / 2).toInt()
            val h = (1280 * m.density / 2).toInt()
            val ws = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY)
            val hs = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
            v.measure(ws, hs)
            v.layout(0, 0, v.measuredWidth, v.measuredHeight)
            val bm = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            val c = Canvas(bm)
            v.draw(c)
            return bm
        }

        @JvmStatic
        fun getTitle(book: Book, fbook: FBook): String? {
            var t: String? = fbook.book!!.getTitle()
            if (t == book.md5)
                t = null
            return t
        }

        @JvmStatic
        fun getTitle(info: RecentInfo): String {
            var s = ""
            if (info.authors != null && info.authors!!.isNotEmpty())
                s += info.authors!!
            if (info.title != null && info.title!!.isNotEmpty()) {
                if (s.isNotEmpty())
                    s += " - "
                s += info.title!!
            }
            return s
        }

        @JvmStatic
        fun coverFile(context: Context, book: Book): File {
            return CacheImagesAdapter.cacheUri(context, book.url!!)
        }

        @JvmStatic
        fun recentFile(book: Book): File {
            val f = AxetStorage.getFile(book.url!!)
            val p = f.parentFile
            return File(p, book.md5!! + "." + JSON_EXT)
        }

        @JvmStatic
        fun loadPosition(s: String?): ZLTextPosition? {
            if (s == null || s.isEmpty())
                return null
            return try {
                val o = JSONArray(s)
                loadPosition(o)
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
        }

        @JvmStatic
        fun loadPosition(a: JSONArray?): ZLTextPosition? {
            if (a == null || a.length() == 0)
                return null
            return ZLTextFixedPosition(a.getInt(0), a.getInt(1), a.getInt(2))
        }

        @JvmStatic
        fun savePosition(position: ZLTextPosition?): JSONArray? {
            if (position == null)
                return null
            val a = JSONArray()
            a.put(position.paragraphIndex)
            a.put(position.elementIndex)
            a.put(position.charIndex)
            return a
        }

        // Wrappers for inherited static methods of com.github.axet.androidlibrary.app.Storage.
        // Regular companion functions (NOT @JvmStatic): Java resolves the inherited statics
        // directly, and @JvmStatic would clash with them ("accidental override").

        fun getFile(uri: Uri): File = AxetStorage.getFile(uri)

        fun exists(context: Context, uri: Uri): Boolean = AxetStorage.exists(context, uri)

        fun getName(context: Context, uri: Uri): String = AxetStorage.getName(context, uri)

        fun takePersistableUriPermission(context: Context, uri: Uri, modeFlags: Int) {
            AxetStorage.takePersistableUriPermission(context, uri, modeFlags)
        }

        fun getTypeByExt(ext: String): String = AxetStorage.getTypeByExt(ext)

        fun list(context: Context, uri: Uri): ArrayList<AxetStorage.Node> = AxetStorage.list(context, uri)
    }

    fun recentUri(book: Book): Uri {
        val s = book.url!!.scheme
        return if (s == ContentResolver.SCHEME_CONTENT) {
            val id = book.md5!! + "." + JSON_EXT
            val doc = AxetStorage.getDocumentParent(context, book.url!!)
            AxetStorage.child(context, doc, id)
        } else if (s == ContentResolver.SCHEME_FILE) {
            Uri.fromFile(recentFile(book))
        } else {
            throw AxetStorage.UnknownUri()
        }
    }

    fun recentUris(book: Book): List<Uri> {
        val list = ArrayList<Uri>()
        val storage = storagePath
        val s = storage.scheme
        if (s == ContentResolver.SCHEME_CONTENT) {
            val contentResolver = context.contentResolver
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(storage, DocumentsContract.getTreeDocumentId(storage))
            val childCursor = contentResolver.query(childrenUri, null, null, null, null)
            if (childCursor != null) {
                try {
                    while (childCursor.moveToNext()) {
                        val idIdx = childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val id = if (idIdx >= 0) childCursor.getString(idIdx) else ""
                        val tIdx = childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val t = if (tIdx >= 0) childCursor.getString(tIdx) else ""
                        val e = AxetStorage.getExt(t).lowercase(Locale.ROOT)
                        if (t.startsWith(book.md5!!) && e == JSON_EXT) {
                            val k = DocumentsContract.buildDocumentUriUsingTree(storage, id)
                            list.add(k)
                        }
                    }
                } finally {
                    childCursor.close()
                }
            }
        } else if (s == ContentResolver.SCHEME_FILE) {
            val dir = AxetStorage.getFile(storage)
            val ff = dir.listFiles { _, name ->
                val e = AxetStorage.getExt(name).lowercase(Locale.ROOT)
                name.startsWith(book.md5!!) && e == JSON_EXT
            }
            if (ff != null) {
                for (f in ff)
                    list.add(Uri.fromFile(f))
            }
        } else {
            throw AxetStorage.UnknownUri()
        }
        return list
    }

    fun save(book: Book) {
        book.info!!.last = System.currentTimeMillis()
        val u = recentUri(book)
        val s = u.scheme
        if (s == ContentResolver.SCHEME_CONTENT) {
            val root = AxetStorage.getDocumentTreeUri(u)
            val id = DocumentsContract.getTreeDocumentId(u)
            val path: String
            if (!id.contains(AxetStorage.COLON))
                path = AxetStorage.getDocumentName(context, u)
            else
                path = AxetStorage.getDocumentChildPath(u)
            val o = AxetStorage.createFile(context, root, path)
            val resolver = context.contentResolver
            val fd: ParcelFileDescriptor
            try {
                fd = resolver.openFileDescriptor(o, "rw")!!
            } catch (e: FileNotFoundException) {
                throw RuntimeException(e)
            }
            val out = fd.fileDescriptor
            try {
                val json = book.info!!.save(context).toString(2)
                val w = FileWriter(out)
                IOUtils.write(json, w)
                w.close()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        } else if (s == ContentResolver.SCHEME_FILE) {
            try {
                val f = AxetStorage.getFile(u)
                val json = book.info!!.save(context).toString(2)
                val w = FileWriter(f)
                IOUtils.write(json, w)
                w.close()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        } else {
            throw AxetStorage.UnknownUri()
        }
    }

    fun load(uri: Uri): Book {
        return load(uri, null)
    }

    fun load(uri: Uri, progress: Progress?): Book {
        val book: Book
        var contentDisposition: String? = null
        val s = uri.scheme
        if (s == ContentResolver.SCHEME_CONTENT) {
            val resolver = context.contentResolver
            try {
                val meta = resolver.query(uri, null, null, null, null)
                if (meta != null) {
                    try {
                        if (meta.moveToFirst()) {
                            val displayNameIdx = meta.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                            if (displayNameIdx >= 0) {
                                contentDisposition = meta.getString(displayNameIdx)
                            }
                            contentDisposition = AxetStorage.getNameNoExt(contentDisposition)
                        }
                    } finally {
                        meta.close()
                    }
                }
                val fd = resolver.openAssetFileDescriptor(uri, "r")!!
                var stream: InputStream = AssetFileDescriptor.AutoCloseInputStream(fd)
                val len = fd.length
                stream = BufferedInputStream(stream)
                if (progress != null)
                    stream = ProgresInputstream(stream, len, progress)
                book = load(stream, uri)
                stream.close()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        } else if (s!!.startsWith(WebViewCustom.SCHEME_HTTP)) {
            try {
                val client = HttpClient()
                val w = client.getResponse(null, uri.toString())
                val error = w.error
                if (error != null)
                    throw RuntimeException(error + ": " + uri)
                val disposition = w.contentDisposition
                if (disposition != null) {
                    val cp = Pattern.compile("filename=[\"]*([^\"]*)[\"]*")
                    val cm = cp.matcher(disposition)
                    if (cm.find()) {
                        contentDisposition = cm.group(1)
                        contentDisposition = AxetStorage.getNameNoExt(contentDisposition)
                    }
                }
                var stream: InputStream = BufferedInputStream(w.inputStream)
                if (progress != null)
                    stream = ProgresInputstream(stream, w.contentLength, progress)
                book = load(stream, uri)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        } else {
            val f = AxetStorage.getFile(uri)
            try {
                val fis = FileInputStream(f)
                var stream: InputStream = fis
                stream = BufferedInputStream(stream)
                if (progress != null)
                    stream = ProgresInputstream(stream, fis.channel.size(), progress)
                book = load(stream, Uri.fromFile(f))
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        val r = recentUri(book)
        if (AxetStorage.exists(context, r)) {
            try {
                book.info = RecentInfo(context, r)
            } catch (e: RuntimeException) {
                Timber.tag(TAG).e(e, "Unable to load info")
            }
        }
        if (book.info == null) {
            book.info = RecentInfo()
            book.info!!.created = System.currentTimeMillis()
        }
        load(book)
        val info = book.info!!
        if (info.title == null || info.title!!.isEmpty() || info.title == book.md5) {
            if (contentDisposition != null && contentDisposition!!.isNotEmpty())
                info.title = contentDisposition
            else
                info.title = AxetStorage.getNameNoExt(uri.lastPathSegment)
        }
        if (!AxetStorage.exists(context, r))
            save(book)
        return book
    }

    fun getCache(): File {
        val external = context.externalCacheDir
        return if (external == null || !AxetStorage.canWrite(external)) context.cacheDir else external
    }

    fun createTempBook(ext: String): File {
        return File.createTempFile("book", "." + ext, getCache())
    }

    fun load(inputStream: InputStream, u: Uri): Book {
        val storage = storagePath

        val s = storage.scheme

        if (s == ContentResolver.SCHEME_CONTENT && DocumentsContract.isDocumentUri(context, u)) {
            if (DocumentsContract.getDocumentId(u).startsWith(DocumentsContract.getTreeDocumentId(storage))) // else we can't get from content://storage to real path
                return Book(context, DocumentsContract.buildDocumentUriUsingTree(storage, DocumentsContract.getDocumentId(u)))
        }
        if (s == ContentResolver.SCHEME_FILE && AxetStorage.relative(storage.path, u.path) != null)
            return Book(context, u)

        var tmp = false
        var file: File? = null

        val book = Book()
        try {
            var os: OutputStream? = null

            if (u.scheme == ContentResolver.SCHEME_FILE) {
                file = AxetStorage.getFile(u)
            } else {
                file = createTempBook("tmp")
                os = BufferedOutputStream(FileOutputStream(file!!))
                tmp = true
            }

            val dd = supported()

            book.md5 = FileTypeDetector.detecting(context, dd, inputStream, os, u)

            for (d in dd) {
                if (d.detected) {
                    book.ext = d.ext
                    if (d is FileTypeDetector.FileTypeDetectorZipExtract.Handler) {
                        if (!tmp) { // !tmp
                            val z = file!!
                            file = createTempBook("tmp")
                            book.md5 = d.extract(z, file!!)
                            tmp = true // force to delete 'fbook.file'
                        } else { // tmp
                            val tt = createTempBook("tmp")
                            book.md5 = d.extract(file!!, tt)
                            file!!.delete() // delete old
                            file = tt // tmp = true
                        }
                    }
                    break // priority first - more imporant
                }
            }

            if (book.ext == null)
                throw RuntimeException("Unsupported format")

            if (book.ext == ComicsPlugin.CBR) { // handling cbz solid archives
                var cbz: File? = null
                try {
                    val archive = Archive(NativeStorage(file!!))
                    if (archive.mainHeader.isSolid) {
                        cbz = createTempBook("tmp")
                        val out = ZipOutputStream(BufferedOutputStream(FileOutputStream(cbz!!)))
                        val list = archive.fileHeaders
                        for (h in list) {
                            if (h.isDirectory)
                                continue

                            val entry = ZipEntry(RarSAF.getRarFileName(h))
                            out.putNextEntry(entry)

                            archive.extractFile(h, out)
                        }
                        out.close()
                        if (tmp)
                            file!!.delete()
                        book.ext = ComicsPlugin.CBZ
                        file = cbz
                        tmp = true
                    }
                } catch (e: Exception) {
                    cbz?.delete()
                    throw RuntimeException("unsupported rar", e)
                }
            }

            if (s == ContentResolver.SCHEME_CONTENT) {
                val contentResolver = context.contentResolver
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(storage, DocumentsContract.getTreeDocumentId(storage))
                val childCursor = contentResolver.query(childrenUri, null, null, null, null)
                if (childCursor != null) {
                    while (childCursor.moveToNext()) {
                        val idIdx = childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val id = if (idIdx >= 0) childCursor.getString(idIdx) else ""
                        val tIdx = childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val t = if (tIdx >= 0) childCursor.getString(tIdx) else ""
                        val n = AxetStorage.getNameNoExt(t)
                        val e = AxetStorage.getExt(t)
                        if (n == book.md5 && e != JSON_EXT) { // delete all but book and json
                            val k = DocumentsContract.buildDocumentUriUsingTree(childrenUri, id)
                            try {
                                AxetStorage.delete(context, k)
                            } catch (e1: RuntimeException) {
                                Timber.tag(TAG).w(e1)
                            }
                        }
                    }
                }
                val id = book.md5!! + "." + book.ext!!
                val o = AxetStorage.createFile(context, storage, id)
                val resolver = context.contentResolver

                val fd = resolver.openFileDescriptor(o, "rw")!!

                val out = fd.fileDescriptor
                val fis = FileInputStream(file!!)
                val fos = BufferedOutputStream(FileOutputStream(out))
                IOUtils.copy(fis, fos)
                fis.close()
                fos.close()

                book.url = o

                if (tmp)
                    file!!.delete()
            } else if (s == ContentResolver.SCHEME_FILE) {
                val f = AxetStorage.getFile(storage)
                val ff = f.listFiles { _, name -> name.startsWith(book.md5!!) }
                if (ff != null) {
                    for (k in ff) {
                        if (!AxetStorage.getExt(k).equals(JSON_EXT, ignoreCase = true))
                            k.delete()
                    }
                }
                val to = File(f, book.md5!! + "." + book.ext!!)
                if (tmp)
                    AxetStorage.move(file!!, to)
                else
                    AxetStorage.copy(file!!, to)
                book.url = Uri.fromFile(to)
            } else {
                throw AxetStorage.UnknownUri()
            }
        } catch (e: RuntimeException) {
            if (tmp && file != null)
                file!!.delete()
            throw e
        } catch (e: Exception) {
            if (tmp && file != null)
                file!!.delete()
            throw RuntimeException(e)
        }
        return book
    }

    fun loadCover(book: FBook): ZLImage? {
        return try {
            val plugin = getPlugin(Info(context), book)
            val file = BookUtil.fileByBook(book.book!!)
            plugin.readCover(file)
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun load(book: Book) {
        if (book.info == null) {
            val r = recentUri(book)
            if (AxetStorage.exists(context, r))
                try {
                    book.info = RecentInfo(context, r)
                } catch (e: RuntimeException) {
                    Timber.tag(TAG).e(e, "Unable to load info")
                }
        }
        if (book.info == null) {
            book.info = RecentInfo()
            book.info!!.created = System.currentTimeMillis()
        }
        var fbook: FBook? = null
        val info = book.info!!
        if (info.authors == null || info.authors!!.isEmpty()) {
            if (fbook == null)
                fbook = read(book)
            info.authors = fbook!!.book!!.authorsString(", ")
        }
        if (info.title == null || info.title!!.isEmpty() || info.title == book.md5) {
            if (fbook == null)
                fbook = read(book)
            info.title = getTitle(book, fbook!!)
        }
        fbook?.close()
    }

    fun createCover(fbook: FBook, cover: File) {
        var image = loadCover(fbook)
        if (image != null) {
            var bm: Bitmap? = null
            if (image is ZLFileImageProxy) {
                if (!image.isSynchronized)
                    image.synchronize()
                image = image.realImage
            }
            if (image is ZLStreamImage) {
                bm = CacheImagesAdapter.createScaled(image.inputStream())
            }
            if (image is ZLBitmapImage) {
                bm = image.getBitmap()
            }
            val authors = fbook.book!!.authors()
            val a = authors != null && authors.isNotEmpty()
            val title = fbook.book!!.getTitle()
            val t = title != null && title.isNotEmpty()
            if (bm == null && (a || t)) {
                val inflater = LayoutInflater.from(context)
                val v = inflater.inflate(R.layout.cover_generate, null)
                val aa = v.findViewById<TextView>(R.id.author)
                aa.text = fbook.book!!.authorsString(", ")
                val tt = v.findViewById<TextView>(R.id.title)
                tt.text = fbook.book!!.getTitle()
                bm = renderView(v)
            }
            if (bm == null) {
                val fb = FBReaderView(context)
                fb.loadBook(fbook)
                bm = renderView(fb)
            }
            if (bm == null)
                return
            try {
                bm = CacheImagesAdapter.createScaled(bm)
                val os = BufferedOutputStream(FileOutputStream(cover))
                bm.compress(Bitmap.CompressFormat.PNG, 100, os)
                os.close()
                bm.recycle()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    fun list(list: ArrayList<Book>, storage: File?) {
        if (storage == null)
            return
        val ff = storage.listFiles { _, name ->
            val n = AxetStorage.getNameNoExt(name)
            val e = AxetStorage.getExt(name).lowercase(Locale.ROOT)
            if (n.length != MD5_SIZE)
                return@listFiles false
            for (d in supported()) {
                if (e == d.ext)
                    return@listFiles true
            }
            false
        }
        if (ff == null)
            return
        for (f in ff) {
            val b = Book()
            b.md5 = AxetStorage.getNameNoExt(f)
            b.url = Uri.fromFile(f)
            val cover = coverFile(context, b)
            if (cover.exists())
                b.cover = cover
            val r = recentFile(b)
            if (r.exists()) {
                try {
                    b.info = RecentInfo(context, r)
                } catch (e: RuntimeException) {
                    Timber.tag(TAG).d(e, "Unable to load info")
                }
            }
            if (b.info == null) {
                b.info = RecentInfo()
                b.info!!.created = System.currentTimeMillis()
            }
            list.add(b)
        }
    }

    fun list(): ArrayList<Book> {
        val uri = storagePath
        val list = ArrayList<Book>()
        val s = uri.scheme
        if (s == ContentResolver.SCHEME_CONTENT) {
            val contentResolver = context.contentResolver
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            val childCursor = contentResolver.query(childrenUri, null, null, null, null)
            if (childCursor != null) {
                try {
                    while (childCursor.moveToNext()) {
                        val idIdx = childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val id = if (idIdx >= 0) childCursor.getString(idIdx) else ""
                        val tIdx = childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        var t = if (tIdx >= 0) childCursor.getString(tIdx) else ""
                        val sizeIdx = childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                        val size = if (sizeIdx >= 0) childCursor.getLong(sizeIdx) else 0L
                        if (size > 0) {
                            t = t.lowercase(Locale.ROOT)
                            val n = AxetStorage.getNameNoExt(t)
                            if (n.length != MD5_SIZE) // prevent scan *.fb2 and other books but only sync related files
                                continue
                            for (d in supported()) {
                                if (t.endsWith("." + d.ext)) {
                                    val k = DocumentsContract.buildDocumentUriUsingTree(uri, id)
                                    val b = Book()
                                    b.md5 = AxetStorage.getNameNoExt(context, k)
                                    b.url = k
                                    val cover = coverFile(context, b)
                                    if (cover.exists())
                                        b.cover = cover
                                    val r = recentUri(b)
                                    if (AxetStorage.exists(context, r)) {
                                        try {
                                            b.info = RecentInfo(context, r)
                                        } catch (e: RuntimeException) {
                                            Timber.tag(TAG).e(e, "Unable to load info")
                                        }
                                    }
                                    if (b.info == null) {
                                        b.info = RecentInfo()
                                        b.info!!.created = System.currentTimeMillis()
                                    }
                                    list.add(b)
                                    break // break dd
                                }
                            }
                        }
                    }
                } finally {
                    childCursor.close()
                }
            }
        } else if (s == ContentResolver.SCHEME_FILE) {
            val dir = AxetStorage.getFile(uri)
            list(list, dir)
        } else {
            throw AxetStorage.UnknownUri()
        }
        return list
    }

    fun delete(book: Book) {
        AxetStorage.delete(context, book.url!!)
        book.cover?.delete()
        try {
            AxetStorage.delete(context, recentUri(book))
        } catch (e: RuntimeException) {
            Timber.tag(TAG).e(e, "failed to delete json") // not exists? IllegalArgument if not exists
        }
        // delete all md5.* files (old, cover images, and sync conflicts files)
        val storage = storagePath
        val s = storage.scheme
        if (s == ContentResolver.SCHEME_CONTENT) {
            val contentResolver = context.contentResolver
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(storage, DocumentsContract.getTreeDocumentId(storage))
            val childCursor = contentResolver.query(childrenUri, null, null, null, null)
            if (childCursor != null) {
                try {
                    while (childCursor.moveToNext()) {
                        val idIdx = childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val id = if (idIdx >= 0) childCursor.getString(idIdx) else ""
                        val tIdx = childCursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val t = if (tIdx >= 0) childCursor.getString(tIdx) else ""
                        if (t.startsWith(book.md5!!)) { // delete all but json
                            val k = DocumentsContract.buildDocumentUriUsingTree(storage, id)
                            AxetStorage.delete(context, k)
                        }
                    }
                } finally {
                    childCursor.close()
                }
            }
        } else if (s == ContentResolver.SCHEME_FILE) {
            val dir = AxetStorage.getFile(storage)
            val ff = dir.listFiles { _, name -> name.startsWith(book.md5!!) }
            if (ff != null) {
                for (f in ff) {
                    f.delete()
                }
            }
        } else {
            throw AxetStorage.UnknownUri()
        }
    }

    fun read(b: Book): FBook {
        try {
            val fbook = FBook()
            if (b.info != null)
                fbook.info = RecentInfo(b.info!!)

            var file: File

            val s = b.url!!.scheme
            if (s == ContentResolver.SCHEME_CONTENT) {
                val ext = AxetStorage.getExt(context, b.url!!)
                fbook.tmp = createTempBook(ext)
                val os = BufferedOutputStream(FileOutputStream(fbook.tmp!!))
                val resolver = context.contentResolver
                val inputStream = resolver.openInputStream(b.url!!)!!
                IOUtils.copy(inputStream, os)
                file = fbook.tmp!!
                inputStream.close()
                os.close()
            } else if (s == ContentResolver.SCHEME_FILE) {
                file = AxetStorage.getFile(b.url!!)
            } else {
                throw AxetStorage.UnknownUri()
            }

            val ext = AxetStorage.getExt(file).lowercase(Locale.ROOT)
            if (ext == ZIP_EXT) { // handle zip files manually, better perfomance
                val dd = supported()
                try {
                    val inputStream = FileInputStream(file)
                    FileTypeDetector.detecting(context, dd, inputStream, null, Uri.fromFile(file))
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
                for (d in dd) {
                    if (d.detected) {
                        if (d is FileTypeDetector.FileTypeDetectorZipExtract.Handler) {
                            if (fbook.tmp == null) { // !tmp
                                val z = file
                                file = createTempBook(d.ext)
                                d.extract(z, file)
                                fbook.tmp = file
                            } else { // tmp
                                val tt = createTempBook(d.ext)
                                d.extract(file, tt)
                                file.delete() // delete old
                                fbook.tmp = tt
                                file = tt
                            }
                        }
                        break // priority first - more imporant
                    }
                }
            }

            fbook.book = FbBook(-1L, file.path, null, null, null)
            val plugin = getPlugin(Info(context), fbook)
            try {
                plugin.readMetainfo(fbook.book!!)
            } catch (e: BookReadingException) {
                throw RuntimeException(e)
            }

            return fbook
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    val storagePath: Uri
        get() {
            val shared = PreferenceManager.getDefaultSharedPreferences(context)
            val path = shared.getString(ReaderPreferences.PREFERENCE_STORAGE, null)
            return if (path == null)
                Uri.fromFile(getLocalStorage())
            else
                getStoragePath(path)
        }

    override fun migrateLocalStorage() {
        migrateLocalStorage(getLocalInternal())
        migrateLocalStorage(getLocalExternal())
    }

    fun migrateLocalStorage(l: File?) {
        if (l == null)
            return

        if (!AxetStorage.canWrite(l))
            return

        val path = storagePath

        val s = path.scheme
        if (s == ContentResolver.SCHEME_FILE) {
            val p = AxetStorage.getFile(path)
            if (!AxetStorage.canWrite(p))
                return
            if (l == p) // same storage path
                return
        }

        val u = Uri.fromFile(l)
        if (u == path) // same storage path
            return

        val ff = l.listFiles()

        if (ff == null)
            return

        for (f in ff) {
            if (!f.isFile)
                continue
            var m = false
            val e = AxetStorage.getExt(f).lowercase(Locale.ROOT)
            if (e == JSON_EXT)
                m = true
            else {
                for (d in supported()) {
                    if (e == d.ext) {
                        m = true
                        break
                    }
                }
            }
            if (m)
                AxetStorage.migrate(context, f, path)
        }
    }

    fun move(u: Uri, dir: Uri): Uri {
        return try {
            var n = AxetStorage.getNextFile(context, storagePath, AxetStorage.getName(context, u), JSON_EXT)
            val inputStream: InputStream
            val os: OutputStream
            val s = u.scheme
            if (s == ContentResolver.SCHEME_CONTENT) {
                val resolver = context.contentResolver
                inputStream = resolver.openInputStream(u)!!
                n = AxetStorage.createFile(context, dir, AxetStorage.getDocumentChildPath(n))
                os = resolver.openOutputStream(n)!!
            } else if (s == ContentResolver.SCHEME_FILE) {
                inputStream = FileInputStream(AxetStorage.getFile(u))
                os = BufferedOutputStream(FileOutputStream(AxetStorage.getFile(n)))
            } else {
                throw AxetStorage.UnknownUri()
            }
            IOUtils.copy(inputStream, os)
            inputStream.close()
            os.close()
            AxetStorage.delete(context, u)
            n
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    open class Info : SystemInfo {
        @JvmField
        var context: Context

        constructor(context: Context) {
            this.context = context
        }

        override fun tempDirectory(): String? = context.cacheDir.path

        override fun networkCacheDirectory(): String? = context.cacheDir.path
    }

    open class Progress {
        @JvmField
        var info: SpeedInfo = SpeedInfo()

        @JvmField
        var last: Long = 0

        init {
            info.start(0L)
        }

        open fun update(read: Long, total: Long) {
            val time = System.currentTimeMillis()
            if (last + AlarmManager.SEC1 < time) {
                info.step(read)
                last = time
                progress(read, total)
            }
        }

        open fun progress(read: Long, total: Long) {
        }
    }

    open class ProgresInputstream(inputStream: InputStream, total: Long, progress: Progress) : InputStream() {
        private var read = 0L
        private val total: Long = total
        private val inputStream: InputStream = inputStream
        private val progress: Progress = progress

        init {
            this.progress.update(0, total)
        }

        override fun read(): Int {
            read++
            progress.update(read, total)
            return inputStream.read()
        }
    }

    open class FileCbz : FileTypeDetector.FileZip(ComicsPlugin.CBZ) { // we not treating all zip archives as comics, ext must be cbz
    }

    open class FileCbr : FileTypeDetector.FileRar(ComicsPlugin.CBR) { // we not treating all rar archives as comics, ext must be cbr
    }

    open class FBook {
        @JvmField
        var tmp: File? = null

        @JvmField
        var book: FbBook? = null

        @JvmField
        var info: RecentInfo? = null

        open fun close() {
            if (tmp != null) {
                tmp!!.delete()
                tmp = null
            }
            book = null
        }
    }

    open class Book {
        @JvmField
        var url: Uri? = null

        @JvmField
        var ext: String? = null

        @JvmField
        var md5: String? = null // can be filename if user renamed file

        @JvmField
        var info: RecentInfo? = null

        @JvmField
        var cover: File? = null

        constructor()

        constructor(context: Context, u: Uri) {
            val name = AxetStorage.getName(context, u)
            url = u
            md5 = AxetStorage.getNameNoExt(name)
            ext = AxetStorage.getExt(name)
        }
    }

    open class RecentInfo {
        @JvmField
        var created: Long = 0 // date added to the my readings

        @JvmField
        var last: Long = 0 // last write time

        @JvmField
        var position: ZLTextPosition? = null

        @JvmField
        var authors: String? = null

        @JvmField
        var title: String? = null

        @JvmField
        var coverUrl: String? = null // путь к обложке книги

        @JvmField
        var bookFileUri: String? = null // URI файла книги для навигации

        @JvmField
        var scales: MutableMap<String, ZLPaintContext.ScalingType> = HashMap() // individual scales

        @JvmField
        var scale: Enum<*>? = null // all images (FBView.ImageFitting; package-private enum, exposed as Enum<*> for Java interop)

        @JvmField
        var fontsize: Int? = null // FBView size or Reflow / 100

        @JvmField
        var fontsizes: MutableMap<String, Int> = TreeMap() // per device fontsize

        @JvmField
        var bookmarks: Bookmarks? = null

        constructor()

        constructor(info: RecentInfo) {
            created = info.created
            last = info.last
            if (info.position != null)
                position = ZLTextFixedPosition(info.position!!)
            authors = info.authors
            title = info.title
            coverUrl = info.coverUrl
            bookFileUri = info.bookFileUri
            scale = info.scale
            scales = HashMap(info.scales)
            fontsize = info.fontsize
            if (info.bookmarks != null)
                bookmarks = Bookmarks(info.bookmarks!!)
        }

        constructor(context: Context, f: File) {
            try {
                val inputStream = FileInputStream(f)
                load(context, inputStream)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        constructor(context: Context, u: Uri) {
            try {
                val resolver = context.contentResolver
                val inputStream: InputStream
                val s = u.scheme
                if (s == ContentResolver.SCHEME_CONTENT) {
                    inputStream = resolver.openInputStream(u)!!
                } else if (s == ContentResolver.SCHEME_FILE) {
                    inputStream = FileInputStream(AxetStorage.getFile(u))
                } else {
                    throw AxetStorage.UnknownUri()
                }
                load(context, inputStream)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        constructor(context: Context, o: JSONObject) {
            load(context, o)
        }

        fun load(context: Context, inputStream: InputStream) {
            val json = IOUtils.toString(inputStream, Charset.defaultCharset())
            val j = JSONObject(json)
            load(context, j)
            inputStream.close()
        }

        fun load(context: Context, o: JSONObject) {
            created = o.optLong("created", 0)
            last = o.getLong("last")
            authors = o.optString("authors", null)
            title = o.optString("title", null)
            position = loadPosition(o.optJSONArray("position"))
            coverUrl = o.optString("coverUrl", null)
            bookFileUri = o.optString("bookFileUri", null)
            val scaleStr = o.optString("scale")
            if (scaleStr != null && scaleStr.isNotEmpty())
                scale = imageFittingValueOf(scaleStr)
            val scalesObj = o.opt("scales")
            if (scalesObj != null) {
                val map = WebViewCustom.toMap(scalesObj as JSONObject)
                for (key in map.keys) {
                    val v = map[key] as String
                    scales[key] = ZLPaintContext.ScalingType.valueOf(v)
                }
            }
            val fontsizeId = "fontsize_" + getAndroidId(context)
            fontsize = o.optInt(fontsizeId, -1)
            if (fontsize == -1)
                fontsize = null
            val kk = o.keys()
            while (kk.hasNext()) {
                val k = kk.next()
                if (k.startsWith("fontsize_") && k != fontsizeId)
                    fontsizes[k] = o.optInt(k)
            }
            val b = o.optJSONArray("bookmarks")
            if (b != null && b.length() > 0)
                bookmarks = Bookmarks(b)
        }

        fun save(context: Context): JSONObject {
            val o = JSONObject()
            o.put("created", created)
            o.put("last", last)
            o.put("authors", authors)
            o.put("title", title)
            if (coverUrl != null)
                o.put("coverUrl", coverUrl)
            if (bookFileUri != null)
                o.put("bookFileUri", bookFileUri)
            val p = savePosition(position)
            if (p != null)
                o.put("position", p)
            if (scale != null)
                o.put("scale", scale!!.name)
            if (!scales.isEmpty())
                o.put("scales", WebViewCustom.toJSON(scales))
            if (fontsize != null)
                o.put("fontsize_" + getAndroidId(context), fontsize)
            for (k in fontsizes.keys)
                o.put(k, fontsizes[k])
            if (bookmarks != null && bookmarks!!.size > 0)
                o.put("bookmarks", bookmarks!!.save())
            return o
        }

        fun merge(info: RecentInfo) {
            if (created > info.created)
                created = info.created
            if (position == null || last < info.last)
                position = ZLTextFixedPosition(info.position!!)
            if (authors == null || last < info.last)
                authors = info.authors
            if (title == null || last < info.last)
                title = info.title
            if (scale == null || last < info.last)
                scale = info.scale
            for (k in info.scales.keys) {
                val v = info.scales[k]!!
                if (last < info.last) // replace with new values
                    scales[k] = v
                else if (!scales.containsKey(k)) // only add non existent values to the list
                    scales[k] = v
            }
            if (fontsize == null || last < info.last)
                fontsize = info.fontsize
            merge(info.fontsizes, info.last)
            if (bookmarks == null) {
                bookmarks = info.bookmarks
            } else if (info.bookmarks != null) {
                for (b in info.bookmarks!!) {
                    var found = false
                    for (i in 0 until bookmarks!!.size) {
                        val m = bookmarks!![i]
                        if (b.start!!.samePositionAs(m.start!!) && b.end!!.samePositionAs(m.end!!) && m.last < b.last) {
                            found = true
                            bookmarks!![i] = b
                        }
                    }
                    if (!found)
                        bookmarks!!.add(b)
                }
            }
        }

        fun merge(fontsizes: MutableMap<String, Int>, last: Long) {
            for (k in fontsizes.keys) {
                if (!this.fontsizes.containsKey(k) || this.last < last)
                    this.fontsizes[k] = fontsizes[k]!!
            }
        }
    }

    open class Bookmark {
        @JvmField
        var last: Long = 0 // last change event

        @JvmField
        var name: String? = null

        @JvmField
        var text: String? = null

        @JvmField
        var color: Int = 0

        @JvmField
        var start: ZLTextPosition? = null

        @JvmField
        var end: ZLTextPosition? = null

        @JvmField
        var coverUrl: String? = null // обложка книги на момент создания закладки

        @JvmField
        var bookFileUri: String? = null // URI файла книги для навигации

        @JvmField
        var sentenceBefore: String? = null // текст предложения до заметки (для контекста)

        @JvmField
        var sentenceAfter: String? = null // текст предложения после заметки (для контекста)

        constructor()

        constructor(b: Bookmark) {
            last = b.last
            name = b.name
            text = b.text
            color = b.color
            start = b.start
            end = b.end
            coverUrl = b.coverUrl
            bookFileUri = b.bookFileUri
            sentenceBefore = b.sentenceBefore
            sentenceAfter = b.sentenceAfter
        }

        constructor(t: String, s: ZLTextPosition, e: ZLTextPosition) {
            last = System.currentTimeMillis()
            text = t
            start = s
            end = e
        }

        constructor(j: JSONObject) {
            load(j)
        }

        fun load(j: JSONObject) {
            last = j.optLong("last")
            name = j.optString("name")
            text = j.optString("text")
            color = j.optInt("color")
            start = loadPosition(j.optJSONArray("start"))
            end = loadPosition(j.optJSONArray("end"))
            coverUrl = j.optString("coverUrl", null)
            bookFileUri = j.optString("bookFileUri", null)
            sentenceBefore = j.optString("sentenceBefore", null)
            sentenceAfter = j.optString("sentenceAfter", null)
        }

        fun save(): JSONObject {
            val j = JSONObject()
            j.put("last", last)
            if (name != null)
                j.put("name", name)
            j.put("text", text)
            j.put("color", color)
            val s = savePosition(start)
            if (s != null)
                j.put("start", s)
            val e = savePosition(end)
            if (e != null)
                j.put("end", e)
            if (coverUrl != null)
                j.put("coverUrl", coverUrl)
            if (bookFileUri != null)
                j.put("bookFileUri", bookFileUri)
            if (sentenceBefore != null)
                j.put("sentenceBefore", sentenceBefore)
            if (sentenceAfter != null)
                j.put("sentenceAfter", sentenceAfter)
            return j
        }
    }

    open class Bookmarks : ArrayList<Bookmark> {
        constructor() : super()

        constructor(bb: Bookmarks) : super() {
            for (b in bb)
                add(Bookmark(b))
        }

        constructor(a: JSONArray) : super() {
            load(a)
        }

        fun save(): JSONArray {
            val a = JSONArray()
            for (b in this)
                a.put(b.save())
            return a
        }

        fun load(json: JSONArray) {
            for (i in 0 until json.length())
                add(Bookmark(json.getJSONObject(i)))
        }

        fun getBookmarks(page: PluginView.Selection.Page): ArrayList<Bookmark> {
            val list = ArrayList<Bookmark>()
            for (b in this) {
                if (b.start!!.paragraphIndex == page.page || b.end!!.paragraphIndex == page.page)
                    list.add(b)
            }
            return list
        }
    }
}

// FBView.ImageFitting — это публичный enum, вложенный в package-private класс ZLTextViewBase.
// Java ссылается на него как FBView.ImageFitting (наследование вложенного типа), но Kotlin так не умеет,
// а ZLTextViewBase недоступен извне пакета. Поэтому храним значение как Enum<*>, а восстанавливаем по имени
// через рефлексию по бинарному имени класса.
private fun imageFittingValueOf(name: String): Enum<*>? {
    val clazz = Class.forName("org.geometerplus.zlibrary.text.view.ZLTextViewBase\$ImageFitting")
    @Suppress("UNCHECKED_CAST")
    return (clazz as Class<out Enum<*>>).getEnumConstants()?.firstOrNull { it.name == name }
}
