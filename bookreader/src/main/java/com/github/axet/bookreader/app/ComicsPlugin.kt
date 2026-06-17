package com.github.axet.bookreader.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.ParcelFileDescriptor
import android.util.Log
import com.github.axet.androidlibrary.app.RarSAF
import com.github.axet.androidlibrary.app.ZipSAF
import com.github.axet.androidlibrary.services.StorageProvider
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter
import com.github.axet.bookreader.widgets.ScrollWidget
import de.innosystec.unrar.Archive
import de.innosystec.unrar.NativeStorage
import de.innosystec.unrar.exception.RarException
import net.lingala.zip4j.ZipFile
import org.apache.commons.io.IOUtils
import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.fbreader.bookmodel.TOCTree
import org.geometerplus.fbreader.formats.BuiltinFormatPlugin
import org.geometerplus.zlibrary.core.encodings.Encoding
import org.geometerplus.zlibrary.core.encodings.EncodingCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.text.model.ZLTextMark
import org.geometerplus.zlibrary.text.model.ZLTextModel
import org.geometerplus.zlibrary.text.model.ZLTextParagraph
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections
import java.util.regex.Pattern

/**
 * Плагин для чтения комиксов (CBZ/CBR форматы).
 */
class ComicsPlugin(info: Storage.Info) : BuiltinFormatPlugin(info, CBZ), Plugin {

    companion object {
        const val CBZ = "cbz"
        const val CBR = "cbr"
        val TAG: String = ComicsPlugin::class.java.simpleName

        /**
         * Проверяет, является ли файл изображением.
         */
        @JvmStatic
        fun isImage(a: ArchiveFile): Boolean {
            val f = File(a.path)
            return CacheImagesAdapter.isImage(f.name)
        }

        /**
         * Получает размер изображения.
         */
        @JvmStatic
        fun getImageSize(`is`: InputStream): Plugin.Box? {
            return try {
                val size = CacheImagesAdapter.getImageSize(`is`)
                `is`.close()
                if (size == null) null
                else Plugin.Box(0, 0, size.width(), size.height())
            } catch (e: IOException) {
                Log.d(TAG, "unable to close is", e)
                null
            }
        }
    }

    /**
     * Создаёт View для отображения комикса.
     */
    override fun create(fbook: Storage.FBook): Plugin.View {
        return ComicsView(BookUtil.fileByBook(fbook.book))
    }

    override fun readMetainfo(book: AbstractBook) {}

    override fun readUids(book: AbstractBook) {}

    override fun detectLanguageAndEncoding(book: AbstractBook) {}

    /**
     * Читает обложку комикса.
     */
    override fun readCover(file: ZLFile): ZLImage {
        val view = ComicsView(file)
        val m = Math.max(view.current!!.pageBox!!.w, view.current!!.pageBox!!.h)
        val ratio = CacheImagesAdapter.COVER_SIZE.toDouble() / m
        val w = (view.current!!.pageBox!!.w * ratio).toInt()
        val h = (view.current!!.pageBox!!.h * ratio).toInt()
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        val canvas = Canvas(bm)
        view.drawWallpaper(canvas)
        view.draw(canvas, bm.width, bm.height, ZLViewEnums.PageIndex.current)
        view.close()
        return ZLBitmapImage(bm)
    }

    override fun readAnnotation(file: ZLFile): String? = null

    override fun priority(): Int = 0

    override fun supportedEncodings(): EncodingCollection = object : EncodingCollection() {
        override fun encodings(): List<Encoding> = emptyList()
        override fun getEncoding(alias: String): Encoding? = null
        override fun getEncoding(code: Int): Encoding? = null
    }

    /**
     * Читает модель книги.
     */
    override fun readModel(model: BookModel) {
        val m = ComicsTextModel(BookUtil.fileByBook(model.book))
        model.setBookTextModel(m)
        if (m.doc!!.toc == null)
            return
        loadTOC(0, 0, m.doc!!.toc!!, model.tocTree)
    }

    /**
     * Загружает оглавление.
     */
    internal fun loadTOC(pos: Int, level: Int, bb: ArrayList<ArchiveToc>, tree: TOCTree): Int {
        var count = 0
        var last: TOCTree? = null
        var i = pos
        while (i < bb.size) {
            val b = bb[i]
            val tt = b.name
            if (tt == null || tt.isEmpty())
                continue
            if (b.level > level) {
                val c = loadTOC(i, b.level, bb, last!!)
                i += c
                count += c
            } else if (b.level < level) {
                break
            } else {
                val t = TOCTree(tree)
                t.setText(tt)
                t.setReference(null, b.page)
                last = t
                i++
                count++
            }
        }
        return count
    }

    /**
     * Интерфейс файла архива.
     */
    interface ArchiveFile {
        val path: String
        @Throws(IOException::class)
        fun open(): InputStream
        @Throws(IOException::class)
        fun copy(os: OutputStream)
        val length: Long
        val rect: Plugin.Box?
    }

    /**
     * Элемент оглавления архива.
     */
    class ArchiveToc(
        val name: String,
        val page: Int,
        val level: Int
    )

    /**
     * Компаратор для сортировки по имени.
     */
    class SortByName : Comparator<ArchiveFile> {
        override fun compare(o1: ArchiveFile, o2: ArchiveFile): Int {
            return o1.path.compareTo(o2.path)
        }
    }

    /**
     * Базовый класс декодера архива.
     */
    open class Decoder {
        var toc: ArrayList<ArchiveToc>? = null
        var pages: ArrayList<ArchiveFile>? = null

        /**
         * Рендерит страницу.
         */
        open fun render(p: Int, c: Bitmap.Config): Bitmap? {
            val f = pages!![p]
            return try {
                val `is` = f.open()
                val op = BitmapFactory.Options()
                op.inPreferredConfig = c
                val bm = BitmapFactory.decodeStream(`is`, null, op)
                `is`.close()
                bm
            } catch (e: IOException) {
                Log.d(TAG, "closing stream", e)
                null
            }
        }

        internal fun load(file: File) {
            pages = list(file)
            if (pages!!.isEmpty())
                throw RuntimeException("no comics found!")
            Collections.sort(pages!!, SortByName())
            loadTOC()
        }

        internal open fun list(file: File): ArrayList<ArchiveFile>? = null

        internal fun loadTOC() {
            var last = ""
            val toc = ArrayList<ArchiveToc>()
            for (i in pages!!.indices) {
                val p = pages!![i]
                val f = File(p.path)
                val n = f.parentFile
                if (n != null) {
                    val fn = n.name
                    val level = n.path.split(Pattern.quote(File.separator)).dropLastWhile { it.isEmpty() }.size - 1
                    if (last != fn) {
                        toc.add(ArchiveToc(fn, i, level))
                        last = fn
                    }
                }
            }
            if (toc.size > 1)
                this.toc = toc
        }

        open fun clear() {}

        open fun close() {}
    }

    /**
     * Декодер RAR-архивов.
     */
    class RarDecoder(file: File) : Decoder() {
        val aa = ArrayList<Archive>()

        init {
            load(file)
        }

        override fun list(file: File): ArrayList<ArchiveFile> {
            return try {
                val ff = ArrayList<ArchiveFile>()
                val archive = Archive(NativeStorage(file))
                val list = archive.fileHeaders
                for (h in list) {
                    if (h.isDirectory)
                        continue
                    val header = h
                    val a = object : ArchiveFile {
                        private var r: Plugin.Box? = null

                        override val path: String = RarSAF.getRarFileName(header)

                        override fun open(): InputStream {
                            return ParcelFileDescriptor.AutoCloseInputStream(
                                object : StorageProvider.ParcelInputStream() {
                                    override fun copy(os: OutputStream) {
                                        try {
                                            archive.extractFile(header, os)
                                        } catch (e: RarException) {
                                            throw IOException(e)
                                        }
                                    }

                                    override fun getStatSize(): Long = header.fullUnpackSize
                                }
                            )
                        }

                        override fun copy(os: OutputStream) {
                            try {
                                archive.extractFile(header, os)
                            } catch (e: RarException) {
                                throw IOException(e)
                            }
                        }

                        override val length: Long = header.fullUnpackSize

                        override val rect: Plugin.Box?
                            get() {
                                return try {
                                    if (r == null)
                                        r = getImageSize(open())
                                    r
                                } catch (e: IOException) {
                                    throw RuntimeException(e)
                                }
                            }
                    }
                    if (isImage(a))
                        ff.add(a)
                }
                ff
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        override fun clear() {
            try {
                for (a in aa)
                    a.close()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
            aa.clear()
        }
    }

    /**
     * Декодер ZIP-архивов.
     */
    class ZipDecoder(file: File) : Decoder() {
        val aa = ArrayList<ZipFile>()

        init {
            load(file)
        }

        override fun list(file: File): ArrayList<ArchiveFile> {
            return try {
                val ff = ArrayList<ArchiveFile>()
                val zip = ZipFile(net.lingala.zip4j.NativeStorage(file))
                aa.add(zip)
                val list = zip.fileHeaders
                for (o in list) {
                    val zipEntry = o as net.lingala.zip4j.model.FileHeader
                    if (zipEntry.isDirectory)
                        continue
                    val a = object : ArchiveFile {
                        private var r: Plugin.Box? = null

                        override val path: String = zipEntry.fileName

                        override fun open(): InputStream {
                            return try {
                                ZipSAF.ZipInputStreamSafe(zip.getInputStream(zipEntry))
                            } catch (e: Exception) {
                                throw RuntimeException(e)
                            }
                        }

                        override fun copy(os: OutputStream) {
                            try {
                                val `is` = zip.getInputStream(zipEntry)
                                IOUtils.copy(`is`, os)
                            } catch (e: Exception) {
                                throw RuntimeException(e)
                            }
                        }

                        override val length: Long = zipEntry.uncompressedSize

                        override val rect: Plugin.Box?
                            get() {
                                if (r == null)
                                    r = getImageSize(open())
                                return r
                            }
                    }
                    if (isImage(a))
                        ff.add(a)
                }
                ff
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        override fun clear() {
            aa.clear()
        }
    }

    /**
     * Страница комикса.
     */
    class ComicsPage : Plugin.Page {
        var doc: Decoder? = null

        constructor(r: ComicsPage) : super(r) {
            doc = r.doc
        }

        constructor(r: ComicsPage, index: ZLViewEnums.PageIndex, w: Int, h: Int) : super(r) {
            this.w = w
            this.h = h
            load(index)
            if (index == ZLViewEnums.PageIndex.current) {
                load()
                renderPage()
            }
        }

        constructor(d: Decoder, page: Int, w: Int, h: Int) {
            this.doc = d
            this.w = w
            this.h = h
            pageNumber = page
            pageOffset = 0
            load()
            renderPage()
        }

        constructor(d: Decoder) {
            doc = d
            load()
        }

        override fun load() {
            val f = doc!!.pages!![pageNumber]
            pageBox = f.rect
            if (pageBox == null)
                pageBox = Plugin.Box(0, 0, 100, 100)
            dpi = 72
        }

        override fun getPagesCount(): Int = doc!!.pages!!.size
    }

    /**
     * View для отображения комикса.
     */
    open class ComicsView(f: ZLFile) : Plugin.View() {
        var doc: Decoder? = null

        init {
            val file = File(f.path)
            if (file.path.lowercase().endsWith(".$CBZ"))
                doc = ZipDecoder(file)
            if (file.path.lowercase().endsWith(".$CBR"))
                doc = RarDecoder(file)
            current = ComicsPage(doc!!)
        }

        override fun getPageInfo(w: Int, h: Int, c: ScrollWidget.ScrollAdapter.PageCursor): Plugin.Page {
            val page = if (c.start == null)
                c.end.paragraphIndex - 1
            else
                c.start.paragraphIndex
            return ComicsPage(doc!!, page, w, h)
        }

        override fun render(w: Int, h: Int, page: Int, c: Bitmap.Config): Bitmap? {
            val r = ComicsPage(doc!!, page, w, h)
            val bm = doc!!.render(r.pageNumber, c)!!
            bm.density = r.dpi
            return bm
        }

        override fun draw(canvas: Canvas, w: Int, h: Int, index: ZLViewEnums.PageIndex, c: Bitmap.Config) {
            val r = ComicsPage(current as ComicsPage, index, w, h)
            if (index == ZLViewEnums.PageIndex.current)
                current!!.updatePage(r)
            val render = r.renderRect()
            val bm = doc!!.render(r.pageNumber, c)
            if (bm != null && render.dst != null) {
                canvas.drawBitmap(bm, render.toRect(bm.width, bm.height), render.dst!!, paint)
                bm.recycle()
            }
        }
    }

    /**
     * Текстовая модель для комиксов.
     */
    class ComicsTextModel(f: ZLFile) : ComicsView(f), ZLTextModel {

        @Suppress("DEPRECATION")
        @Throws(Throwable::class)
        protected fun finalize() {
            doc!!.close()
        }

        override fun getId(): String? = null
        override fun getLanguage(): String? = null
        override fun getParagraphsNumber(): Int = doc!!.pages!!.size

        override fun getParagraph(index: Int): ZLTextParagraph {
            return object : ZLTextParagraph {
                override fun iterator(): ZLTextParagraph.EntryIterator? = null
                override fun getKind(): Byte = ZLTextParagraph.Kind.END_OF_TEXT_PARAGRAPH
            }
        }

        override fun removeAllMarks() {}
        override fun getFirstMark(): ZLTextMark? = null
        override fun getLastMark(): ZLTextMark? = null
        override fun getNextMark(position: ZLTextMark): ZLTextMark? = null
        override fun getPreviousMark(position: ZLTextMark): ZLTextMark? = null
        override fun getMarks(): List<ZLTextMark> = ArrayList()

        override fun getTextLength(index: Int): Int = index
        override fun findParagraphByTextLength(length: Int): Int = 0
        override fun search(text: String, startIndex: Int, endIndex: Int, ignoreCase: Boolean): Int = 0
    }
}
