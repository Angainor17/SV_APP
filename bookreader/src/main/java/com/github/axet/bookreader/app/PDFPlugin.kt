package com.github.axet.bookreader.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.SparseArray
import com.github.axet.androidlibrary.app.Natives
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter
import com.github.axet.bookreader.widgets.FBReaderView
import com.github.axet.bookreader.widgets.ScrollWidget
import com.github.axet.pdfium.Config
import com.github.axet.pdfium.Pdfium
import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.fbreader.bookmodel.TOCTree
import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.fbreader.formats.BuiltinFormatPlugin
import org.geometerplus.zlibrary.core.encodings.EncodingCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.text.model.ZLTextMark
import org.geometerplus.zlibrary.text.model.ZLTextModel
import org.geometerplus.zlibrary.text.model.ZLTextParagraph
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage
import java.io.File
import java.io.IOException
import java.text.Normalizer
import java.util.Locale

class PDFPlugin(info: Storage.Info) : BuiltinFormatPlugin(info, EXT), Plugin {

    companion object {
        const val EXT = "pdf"
        val TAG: String = PDFPlugin::class.java.simpleName

        // Флаг: использовать Android PdfRenderer вместо нативного pdfium
        // PdfRenderer работает стабильно на всех устройствах, но имеет меньше функций
        // pdfium может крашиться на некоторых устройствах (особенно планшеты с большим экраном)
        private var usePdfRenderer: Boolean? = null

        @JvmStatic
        fun create(info: Storage.Info): PDFPlugin {
            Log.d(TAG, "create() called, Config.natives=${Config.natives}, usePdfRenderer=$usePdfRenderer")

            // Определяем, нужно ли использовать Android PdfRenderer
            if (usePdfRenderer == null) {
                val isTablet = isTabletDevice(info)
                Log.d(TAG, "Device type: isTablet=$isTablet")

                // На планшетах используем PdfRenderer (стабильный), pdfium крашится при открытии
                // На смартфонах используем pdfium (есть выделение текста, поиск)
                usePdfRenderer = isTablet
                Log.d(TAG, "usePdfRenderer=$usePdfRenderer (tablet=$isTablet)")

                // Загружаем нативные библиотеки для pdfium (для смартфонов или для текстовых операций)
                if (Config.natives) {
                    Log.d(TAG, "Loading native libraries: modpdfium, pdfiumjni")
                    try {
                        Natives.loadLibraries(info.context, "modpdfium", "pdfiumjni")
                        Config.natives = false
                        Log.d(TAG, "Native libraries loaded successfully")
                    } catch (e: Throwable) {
                        Log.e(TAG, "Failed to load native libraries", e)
                        // Если библиотеки не загрузились, используем PdfRenderer
                        usePdfRenderer = true
                    }
                }
            }

            return PDFPlugin(info)
        }

        /**
         * Определить, является ли устройство планшетом
         */
        private fun isTabletDevice(info: Storage.Info): Boolean {
            val dm = info.context.resources.displayMetrics
            val widthInches = dm.widthPixels / dm.xdpi
            val heightInches = dm.heightPixels / dm.ydpi
            val screenSize = kotlin.math.sqrt(widthInches * widthInches + heightInches * heightInches).toDouble()
            Log.d(TAG, "Screen size: $screenSize inches, width=$widthInches, height=$heightInches")
            return screenSize >= 7.0 // 7+ дюймов = планшет
        }

        /**
         * Проверить, используется ли Android PdfRenderer
         */
        fun isUsingPdfRenderer(): Boolean = usePdfRenderer == true
    }

    override fun create(fbook: Storage.FBook): Plugin.View {
        val file = BookUtil.fileByBook(fbook.book)
        Log.d(TAG, "create(fbook) file=${file.path}, usePdfRenderer=$usePdfRenderer")

        return if (isUsingPdfRenderer()) {
            Log.d(TAG, "Using NativeView (Android PdfRenderer)")
            NativeView(file)
        } else {
            Log.d(TAG, "Using PdfiumView (native library)")
            PdfiumView(file)
        }
    }

    @Throws(BookReadingException::class)
    override fun readMetainfo(book: AbstractBook) {
        val f = BookUtil.fileByBook(book)
        Log.d(TAG, "readMetainfo() file=${f.path}, exists=${f.exists()}, usePdfRenderer=$usePdfRenderer")

        // Проверяем размер файла
        val file = File(f.path)
        val fileSize = if (file.exists()) file.length() else -1L
        Log.d(TAG, "readMetainfo() fileSize=$fileSize bytes")

        if (!file.exists()) {
            Log.e(TAG, "readMetainfo() file does not exist!")
            throw IllegalStateException("File does not exist: ${f.path}")
        }

        if (fileSize == 0L) {
            Log.e(TAG, "readMetainfo() file is empty!")
            throw IllegalStateException("File is empty: ${f.path}")
        }

        if (isUsingPdfRenderer()) {
            readMetainfoWithPdfRenderer(book, file)
        } else {
            readMetainfoWithPdfium(book, file)
        }
    }

    @Throws(BookReadingException::class)
    private fun readMetainfoWithPdfium(book: AbstractBook, file: File) {
        Log.d(TAG, "readMetainfoWithPdfium() file=${file.path}")
        try {
            val doc = Pdfium()
            Log.d(TAG, "readMetainfoWithPdfium() opening file: ${file.path}")
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            Log.d(TAG, "readMetainfoWithPdfium() fd created, fd=$fd")

            if (fd.fileDescriptor == null) {
                Log.e(TAG, "readMetainfoWithPdfium() fileDescriptor is null")
                fd.close()
                throw IllegalStateException("FileDescriptor is null")
            }

            Log.d(TAG, "readMetainfoWithPdfium() calling doc.open()...")
            doc.open(fd.fileDescriptor)
            Log.d(TAG, "readMetainfoWithPdfium() doc opened successfully")

            book.addAuthor(doc.getMeta(Pdfium.META_AUTHOR))
            book.setTitle(doc.getMeta(Pdfium.META_TITLE))
            Log.d(TAG, "readMetainfoWithPdfium() meta read, closing doc")
            doc.close()
            fd.close()
            Log.d(TAG, "readMetainfoWithPdfium() completed successfully")
        } catch (e: IOException) {
            Log.e(TAG, "readMetainfoWithPdfium() IOException", e)
            throw IllegalStateException(e)
        } catch (e: Throwable) {
            Log.e(TAG, "readMetainfoWithPdfium() unexpected error: ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }
    }

    @Throws(BookReadingException::class)
    private fun readMetainfoWithPdfRenderer(book: AbstractBook, file: File) {
        Log.d(TAG, "readMetainfoWithPdfRenderer() file=${file.path}")
        try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            Log.d(TAG, "readMetainfoWithPdfRenderer() fd created, fd=$fd")

            val renderer = PdfRenderer(fd)
            Log.d(TAG, "readMetainfoWithPdfRenderer() renderer created, pages=${renderer.pageCount}")

            // PdfRenderer не предоставляет метаданные автора/названия
            // Используем имя файла как заголовок если не задано
            if (book.getTitle().isNullOrEmpty()) {
                val title = file.nameWithoutExtension
                book.setTitle(title)
                Log.d(TAG, "readMetainfoWithPdfRenderer() set title from filename: $title")
            }

            renderer.close()
            fd.close()
            Log.d(TAG, "readMetainfoWithPdfRenderer() completed successfully")
        } catch (e: IOException) {
            Log.e(TAG, "readMetainfoWithPdfRenderer() IOException", e)
            throw IllegalStateException(e)
        } catch (e: Throwable) {
            Log.e(TAG, "readMetainfoWithPdfRenderer() unexpected error: ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }
    }

    @Throws(BookReadingException::class)
    override fun readUids(book: AbstractBook) {
    }

    @Throws(BookReadingException::class)
    override fun detectLanguageAndEncoding(book: AbstractBook) {
    }

    override fun readCover(f: ZLFile): ZLImage {
        Log.d(TAG, "readCover() file=${f.path}, usePdfRenderer=$usePdfRenderer")

        return if (isUsingPdfRenderer()) {
            readCoverWithPdfRenderer(f)
        } else {
            readCoverWithPdfium(f)
        }
    }

    private fun readCoverWithPdfium(f: ZLFile): ZLImage {
        Log.d(TAG, "readCoverWithPdfium() file=${f.path}")
        val view = PdfiumView(f)
        Log.d(TAG, "readCoverWithPdfium() view created, scaling")
        view.current!!.scale(CacheImagesAdapter.COVER_SIZE, CacheImagesAdapter.COVER_SIZE)
        Log.d(TAG, "readCoverWithPdfium() creating bitmap: ${view.current!!.pageBox!!.w}x${view.current!!.pageBox!!.h}")
        val bm = Bitmap.createBitmap(view.current!!.pageBox!!.w, view.current!!.pageBox!!.h, Bitmap.Config.RGB_565)
        val canvas = Canvas(bm)
        Log.d(TAG, "readCoverWithPdfium() drawing wallpaper")
        view.drawWallpaper(canvas)
        Log.d(TAG, "readCoverWithPdfium() rendering page")
        view.draw(canvas, bm.width, bm.height, ZLViewEnums.PageIndex.current)
        Log.d(TAG, "readCoverWithPdfium() closing view")
        view.close()
        Log.d(TAG, "readCoverWithPdfium() completed")
        return ZLBitmapImage(bm)
    }

    private fun readCoverWithPdfRenderer(f: ZLFile): ZLImage {
        Log.d(TAG, "readCoverWithPdfRenderer() file=${f.path}")
        val view = NativeView(f)
        Log.d(TAG, "readCoverWithPdfRenderer() view created, scaling")
        view.current!!.scale(CacheImagesAdapter.COVER_SIZE, CacheImagesAdapter.COVER_SIZE)
        Log.d(TAG, "readCoverWithPdfRenderer() creating bitmap: ${view.current!!.pageBox!!.w}x${view.current!!.pageBox!!.h}")
        val bm = Bitmap.createBitmap(view.current!!.pageBox!!.w, view.current!!.pageBox!!.h, Bitmap.Config.RGB_565)
        val canvas = Canvas(bm)
        Log.d(TAG, "readCoverWithPdfRenderer() rendering page")
        view.draw(canvas, bm.width, bm.height, ZLViewEnums.PageIndex.current)
        Log.d(TAG, "readCoverWithPdfRenderer() closing view")
        view.close()
        Log.d(TAG, "readCoverWithPdfRenderer() completed")
        return ZLBitmapImage(bm)
    }

    override fun readAnnotation(file: ZLFile): String? = null

    override fun priority(): Int = 0

    override fun supportedEncodings(): EncodingCollection? = null

    @Throws(BookReadingException::class)
    override fun readModel(model: BookModel) {
        if (isUsingPdfRenderer()) {
            // Android PdfRenderer не предоставляет текстовую модель
            // Создаём простую модель на основе количества страниц
            readModelWithPdfRenderer(model)
        } else {
            val m = PDFTextModel(BookUtil.fileByBook(model.Book))
            model.setBookTextModel(m)
            val bookmarks = m.doc.toc
            loadTOC(0, 0, bookmarks, model.TOCTree)
        }
    }

    @Throws(BookReadingException::class)
    private fun readModelWithPdfRenderer(model: BookModel) {
        Log.d(TAG, "readModelWithPdfRenderer() creating simple model")
        val file = BookUtil.fileByBook(model.Book)
        try {
            val fd = ParcelFileDescriptor.open(File(file.path), ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val pagesCount = renderer.pageCount
            Log.d(TAG, "readModelWithPdfRenderer() pages=$pagesCount")

            // Создаём простую текстовую модель с количеством страниц
            val textModel = SimplePdfTextModel(pagesCount)
            model.setBookTextModel(textModel)

            // PdfRenderer не предоставляет TOC
            renderer.close()
            fd.close()
            Log.d(TAG, "readModelWithPdfRenderer() completed")
        } catch (e: Exception) {
            Log.e(TAG, "readModelWithPdfRenderer() error", e)
            throw IllegalStateException(e)
        }
    }

    /**
     * Простая текстовая модель для PDF без текстового содержимого
     */
    private class SimplePdfTextModel(private val pagesCount: Int) : ZLTextModel {
        override fun getId(): String? = null
        override fun getLanguage(): String? = null
        override fun getParagraphsNumber(): Int = pagesCount
        override fun getParagraph(index: Int): ZLTextParagraph = object : ZLTextParagraph {
            override fun iterator(): ZLTextParagraph.EntryIterator? = null
            override fun getKind(): Byte = ZLTextParagraph.Kind.END_OF_TEXT_PARAGRAPH
        }
        override fun removeAllMarks() {}
        override fun getFirstMark(): ZLTextMark? = null
        override fun getLastMark(): ZLTextMark? = null
        override fun getNextMark(position: ZLTextMark): ZLTextMark? = null
        override fun getPreviousMark(position: ZLTextMark): ZLTextMark? = null
        override fun getMarks(): List<ZLTextMark> = emptyList()
        override fun getTextLength(index: Int): Int = index
        override fun findParagraphByTextLength(length: Int): Int = 0
        override fun search(text: String, startIndex: Int, endIndex: Int, ignoreCase: Boolean): Int = 0
    }

    private fun loadTOC(pos: Int, level: Int, bb: Array<Pdfium.Bookmark>, tree: TOCTree): Int {
        var count = 0
        var i = pos
        var last: TOCTree? = null
        while (i < bb.size) {
            val b = bb[i]
            val tt = b.title
            if (tt.isNullOrEmpty()) {
                i++
                continue
            }
            if (b.level > level) {
                val c = loadTOC(i, b.level, bb, last!!)
                i += c
                count += c
            } else if (b.level < level) {
                break
            } else {
                val t = TOCTree(tree)
                t.text = tt
                t.setReference(null, b.page)
                last = t
                i++
                count++
            }
        }
        return count
    }

    class UL : Comparator<Rect> {
        override fun compare(o1: Rect, o2: Rect): Int {
            val r = o2.top.compareTo(o1.top)
            return if (r != 0) r else o1.left.compareTo(o2.left)
        }
    }

    class SelectionPage {
        var page: Int
        var ppage: Pdfium.Page
        var text: Pdfium.Text
        var index: Int = 0
        var count: Int
        var w: Int
        var h: Int
        var sorted: Array<Rect>

        constructor(s: SelectionPage) {
            page = s.page
            ppage = s.ppage
            text = s.text
            index = s.index
            count = s.count
            w = s.w
            h = s.h
            sorted = s.sorted
        }

        constructor(pdfium: Pdfium, selPage: Plugin.View.Selection.Page) : this(selPage.page, pdfium.openPage(selPage.page), selPage.w, selPage.h)

        constructor(pdfium: Pdfium, pageNum: Int) : this(pageNum, pdfium.openPage(pageNum), 0, 0)

        constructor(p: Int, page: Pdfium.Page, w: Int, h: Int) {
            this.page = p
            this.ppage = page
            this.text = page.open()
            this.count = text.count
            this.w = w
            this.h = h
            this.sorted = text.getBounds(0, count)
            sorted.sortWith(UL())
        }

        fun first(): Int {
            for (r in sorted) {
                val k = Rect(r)
                var idx: Int
                do {
                    idx = text.getIndex(k.left, k.centerY())
                } while (idx == -1 && ++k.left < k.right)
                if (idx != -1) return idx
            }
            return 0
        }

        fun close() {
            text.close()
            ppage.close()
        }
    }

    inner class Selection : Plugin.View.Selection {
        var pdfium: Pdfium
        var startPage: SelectionPage? = null
        var endPage: SelectionPage? = null
        var map: SparseArray<SelectionPage> = SparseArray()

        constructor(pdfium: Pdfium, page: SelectionPage, point: Point) {
            this.pdfium = pdfium
            map.put(page.page, page)
            val p = Point(page.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y))
            selectWord(page, p)
        }

        constructor(pdfium: Pdfium, start: ZLTextPosition, end: ZLTextPosition) {
            this.pdfium = pdfium
            this.startPage = openPageNum(start.paragraphIndex)
            this.startPage!!.index = start.elementIndex
            this.endPage = openPageNum(end.paragraphIndex)
            this.endPage!!.index = end.elementIndex
        }

        constructor(pdfium: Pdfium, page: Int) {
            this.pdfium = pdfium
            this.startPage = openPageNum(page)
            this.startPage!!.index = 0
            this.endPage = openPageNum(page)
            this.endPage!!.index = this.endPage!!.count
        }

        fun isEmpty(): Boolean {
            if (startPage == null || endPage == null) return true
            return startPage!!.index == -1 || endPage!!.index == -1
        }

        internal fun isWord(p: SelectionPage, i: Int): Boolean {
            var s = p.text.getText(i, 1) ?: return false
            if (s.length != 1) return false
            s = Normalizer.normalize(s, Normalizer.Form.NFC).lowercase(Locale.US)
            return isWord(s.toCharArray()[0])
        }

        internal fun openSelPage(selPage: Page): SelectionPage {
            var p = map.get(selPage.page)
            if (p != null) {
                p.w = selPage.w
                p.h = selPage.h
            }
            if (p == null) {
                p = SelectionPage(pdfium, selPage)
                map.put(p.page, p)
            }
            return SelectionPage(p)
        }

        internal fun openPageNum(pageNum: Int): SelectionPage {
            var p = map.get(pageNum)
            if (p == null) {
                p = SelectionPage(pdfium, pageNum)
                map.put(p.page, p)
            }
            return SelectionPage(p)
        }

        private fun selectWord(page: SelectionPage, point: Point) {
            startPage = page
            val idx = startPage!!.text.getIndex(point.x, point.y)
            if (idx < 0 || idx >= startPage!!.count) return
            var startIndex = idx
            while (startIndex >= 0 && isWord(startPage!!, startIndex)) {
                startPage!!.index = startIndex
                startIndex--
            }
            endPage = SelectionPage(page)
            var endIndex = idx
            while (endIndex < endPage!!.count && isWord(endPage!!, endIndex)) {
                endPage!!.index = endIndex
                endIndex++
            }
        }

        override fun setStart(page: Page, point: Point) {
            val sp = openSelPage(page)
            if (sp.count > 0) {
                val p = Point(sp.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y))
                val idx = sp.text.getIndex(p.x, p.y)
                if (idx == -1) return
                sp.index = idx
                startPage = sp
            }
        }

        override fun setEnd(page: Page, point: Point) {
            val ep = openSelPage(page)
            if (ep.count > 0) {
                val p = Point(ep.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y))
                val idx = ep.text.getIndex(p.x, p.y)
                if (idx == -1) return
                ep.index = idx
                endPage = ep
            }
        }

        override fun getText(): String {
            val b = SelectionBounds()
            val sb = StringBuilder()
            for (i in b.s.page..b.e.page)
                sb.append(getText(i))
            return sb.toString()
        }

        internal fun getText(i: Int): String {
            val b = SelectionBounds(i)
            return b.page.text.getText(b.ss, b.cc)
        }

        override fun getBoundsAll(page: Page): Array<Rect>? {
            val p = openSelPage(page)
            val rr = p.text.getBounds(0, p.count)
            for (i in rr.indices) {
                var r = rr[i]
                r = p.ppage.toDevice(0, 0, p.w, p.h, 0, r)
                rr[i] = r
            }
            return rr
        }

        override fun getBounds(p: Page): Bounds {
            val bounds = Bounds()
            val b = SelectionBounds(p)
            bounds.reverse = b.reverse
            bounds.start = b.first
            bounds.end = b.last
            bounds.rr = b.page.text.getBounds(b.ss, b.cc)
            for (i in bounds.rr!!.indices) {
                var r = bounds.rr!![i]
                r = b.page.ppage.toDevice(0, 0, b.page.w, b.page.h, 0, r)
                bounds.rr!![i] = r
            }
            return bounds
        }

        override fun inBetween(page: Page, start: Point, end: Point): Boolean? {
            val b = SelectionBounds(page)
            if (b.s.page < page.page && page.page < b.e.page) return true
            if (b.page.count > 0) {
                val p1 = Point(b.page.ppage.toPage(0, 0, page.w, page.h, 0, start.x, start.y))
                val i1 = b.page.text.getIndex(p1.x, p1.y)
                if (i1 == -1) return null
                val p2 = Point(b.page.ppage.toPage(0, 0, page.w, page.h, 0, end.x, end.y))
                val i2 = b.page.text.getIndex(p2.x, p2.y)
                if (i2 == -1) return null
                if (i2 < i1) return null
                return i1 <= b.ss && b.ss <= i2 || i1 <= b.ll && b.ll <= i2
            }
            return null
        }

        override fun isValid(page: Page, point: Point): Boolean {
            val b = SelectionBounds(page)
            if (b.page.count > 0) {
                val p = Point(b.page.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y))
                return b.page.text.getIndex(p.x, p.y) != -1
            }
            return false
        }

        override fun isSelected(page: Int): Boolean {
            val b = SelectionBounds(page)
            return b.s.page <= page && page <= b.e.page
        }

        override fun isAbove(page: Page, point: Point): Boolean? {
            val b = SelectionBounds(page)
            if (b.s.page < page.page) return true
            if (b.page.count > 0) {
                val p = Point(b.page.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y))
                val idx = b.page.text.getIndex(p.x, p.y)
                if (idx == -1) return null
                return b.ss < idx || b.ll < idx
            }
            return null
        }

        override fun isBelow(page: Page, point: Point): Boolean? {
            val b = SelectionBounds(page)
            if (b.e.page > page.page) return true
            if (b.page.count > 0) {
                val p = Point(b.page.ppage.toPage(0, 0, page.w, page.h, 0, point.x, point.y))
                val idx = b.page.text.getIndex(p.x, p.y)
                if (idx == -1) return null
                return idx < b.ss || idx < b.ll
            }
            return null
        }

        override fun close() {
            startPage?.let { it.close(); startPage = null }
            endPage?.let { it.close(); endPage = null }
            for (i in 0 until map.size()) map.valueAt(i).close()
            map.clear()
        }

        override fun getStart(): ZLTextPosition? = startPage?.let { ZLTextFixedPosition(it.page, it.index, 0) }

        override fun getEnd(): ZLTextPosition? = endPage?.let { ZLTextFixedPosition(it.page, it.index, 0) }

        inner class SelectionBounds {
            var page: SelectionPage
            var s: SelectionPage
            var e: SelectionPage
            var ss: Int = 0
            var ll: Int = 0
            var ee: Int = 0
            var cc: Int = 0
            var first: Boolean = false
            var last: Boolean = false
            var reverse: Boolean = false

            constructor(p: Page) : this(p.page) {
                startPage?.w = p.w
                startPage?.h = p.h
                endPage?.w = p.w
                endPage?.h = p.h
                page.w = p.w
                page.h = p.h
            }

            constructor(p: Int) : this() {
                if (s.page == e.page) {
                    page = s
                    ss = s.index
                    ee = e.index + 1
                    cc = ee - ss
                    first = true
                    last = true
                    if (reverse) ss++
                } else if (s.page == p) {
                    page = s
                    ss = s.index
                    ee = s.count
                    cc = ee - ss
                    first = true
                    if (reverse) ss++
                } else if (e.page == p) {
                    page = e
                    ss = e.first()
                    ee = e.index + 1
                    cc = ee - ss
                    last = true
                } else {
                    page = openPageNum(p)
                    ss = page.first()
                    ee = page.count
                    cc = ee - ss
                }
                ll = ee - 1
            }

            constructor() {
                // Null safety: selection может быть закрыт во время touch event
                val sp = startPage
                val ep = endPage
                if (sp == null || ep == null) {
                    // Создаем пустой/invalid bounds
                    page = openPageNum(0)
                    ss = 0
                    ee = 0
                    cc = 0
                    ll = 0
                    s = page
                    e = page
                    first = false
                    last = false
                    return
                }
                if (sp.page > ep.page) {
                    reverse = true
                    s = ep
                    e = sp
                } else if (sp.page == ep.page) {
                    if (sp.index > ep.index) {
                        reverse = true
                        s = ep
                        e = sp
                    } else {
                        s = sp
                        e = ep
                    }
                } else {
                    s = sp
                    e = ep
                }
                page = s
            }
        }
    }

    class SearchResult(val page: Int, val start: Int, val count: Int) {
        fun end(): Int = start + count
    }

    inner class PdfSearch(val pdfium: Pdfium, val str: String) : Plugin.View.Search() {
        var all: ArrayList<SearchResult> = ArrayList()
        var pages: SparseArray<ArrayList<SearchResult>> = SparseArray()
        var index: Int = -1
        var initialPage: Int = -1

        internal fun hasText(page: Int): Boolean {
            val p = pdfium.openPage(page) ?: return false
            val t = p.open()
            try {
                return t != null && t.count > 0
            } finally {
                t?.close()
                p.close()
            }
        }

        internal fun search(i: Int): ArrayList<SearchResult> {
            val pg = pdfium.openPage(i)
            val text = pg.open()
            val pattern = str.lowercase(Locale.US)
            val rr = ArrayList<SearchResult>()
            if (text.count > 0) {
                var txt = text.getText(0, text.count).lowercase(Locale.US)
                var idx = txt.indexOf(pattern)
                while (idx != -1) {
                    rr.add(SearchResult(i, idx, pattern.length))
                    idx = txt.indexOf(pattern, idx + 1)
                }
            }
            pages.put(i, rr)
            text.close()
            pg.close()
            return rr
        }

        override fun getBounds(page: Plugin.View.Selection.Page): Bounds? {
            val bounds = Bounds()
            val list = pages.get(page.page) ?: return null
            val p = pdfium.openPage(page.page)
            val t = p.open()
            val rr = ArrayList<Rect>()
            for (r in list) {
                val hh = ArrayList<Rect>()
                val bb = t.getBounds(r.start, r.count)
                for (b in bb) {
                    var rect = p.toDevice(0, 0, page.w, page.h, 0, b)
                    rr.add(rect)
                    hh.add(rect)
                }
                if (index >= 0 && r == all[index]) {
                    bounds.highlight = hh.toTypedArray()
                }
            }
            bounds.rr = rr.toTypedArray()
            t.close()
            p.close()
            return bounds
        }

        override fun getCount(): Int = all.size

        override fun next(): Int {
            if (all.isEmpty()) return -1
            if (index == -1 && initialPage != -1) {
                for (i in all.indices) {
                    if (all[i].page >= initialPage) {
                        index = i
                        return all[i].page
                    }
                }
            }
            index++
            if (index >= all.size) {
                for (i in all[index - 1].page + 1 until pdfium.pagesCount) {
                    all.addAll(search(i))
                    if (index < all.size) return all[index].page
                }
                index = all.size - 1
            }
            return all[index].page
        }

        override fun prev(): Int {
            if (all.isEmpty()) return -1
            if (index == -1 && initialPage != -1) {
                for (i in all.size - 1 downTo 0) {
                    if (all[i].page <= initialPage) {
                        var j = i
                        while (j >= 0 && all[j].page == initialPage) j--
                        index = j + 1
                        return all[index].page
                    }
                }
            }
            index--
            if (index < 0) {
                val r = all[0]
                for (i in r.page - 1 downTo 1) {
                    all.addAll(0, search(i))
                    index = all.indexOf(r) - 1
                    if (index >= 0) return all[index].page
                }
                index = 0
            }
            return all[index].page
        }

        override fun setPage(page: Int) {
            this.initialPage = page
            if (str.isEmpty()) return
            // Search all pages in the book (no limit)
            for (i in 0 until pdfium.pagesCount) {
                all.addAll(search(Plugin.View.Selection.odd(page, i, pdfium.pagesCount)))
            }
        }
    }

    inner class NativePage : Plugin.Page {
        var doc: PdfRenderer
        var page: PdfRenderer.Page? = null

        constructor(r: NativePage) : super(r) { doc = r.doc }

        constructor(r: NativePage, index: ZLViewEnums.PageIndex, w: Int, h: Int) : this(r) {
            this.w = w
            this.h = h
            loadPageIndex(index)
            loadPage()
            if (index == ZLViewEnums.PageIndex.current) {
                renderPage()
            }
        }

        constructor(d: PdfRenderer, page: Int, w: Int, h: Int) {
            doc = d
            this.w = w
            this.h = h
            pageNumber = page
            pageOffset = 0
            loadPage()
            renderPage()
        }

        constructor(d: PdfRenderer) { doc = d }

        override fun getPagesCount(): Int = doc.pageCount

        override fun load() {
            loadPage()
        }

        private fun loadPageIndex(index: ZLViewEnums.PageIndex) {
            when (index) {
                ZLViewEnums.PageIndex.current -> { /* pageNumber уже установлен */ }
                ZLViewEnums.PageIndex.previous -> pageNumber = getPrevPage()
                ZLViewEnums.PageIndex.next -> pageNumber = getNextPage()
            }
        }

        private fun loadPage() {
            try {
                page?.close()
                page = null
                page = doc.openPage(pageNumber)
                pageBox = Plugin.Box(0, 0, page!!.width, page!!.height)
                Log.d(TAG, "NativePage.loadPage() page=$pageNumber, size=${page!!.width}x${page!!.height}")
            } catch (e: Exception) {
                Log.e(TAG, "NativePage.loadPage() error for page $pageNumber", e)
                throw e
            }
        }

        private fun getPrevPage(): Int = if (pageNumber > 0) pageNumber - 1 else 0
        private fun getNextPage(): Int = if (pageNumber < getPagesCount() - 1) pageNumber + 1 else getPagesCount() - 1
    }

    inner class NativeView(f: ZLFile) : Plugin.View() {
        var doc: PdfRenderer
        var fd: ParcelFileDescriptor

        // Pdfium для текстовых операций (выделение, поиск) - ленивая инициализация
        private var textDoc: Pdfium? = null
        private var textFd: ParcelFileDescriptor? = null
        private val filePath: String = f.path

        init {
            try {
                fd = ParcelFileDescriptor.open(File(f.path), ParcelFileDescriptor.MODE_READ_ONLY)
                doc = PdfRenderer(fd)
                current = NativePage(doc)
                Log.d(TAG, "NativeView.init() opened ${f.path}, pages=${doc.pageCount}")
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }
        }

        /**
         * Инициализирует pdfium для текстовых операций
         */
        private fun initTextDoc(): Boolean {
            if (textDoc != null) return true

            try {
                Log.d(TAG, "NativeView.initTextDoc() initializing pdfium for text operations")
                textFd = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
                textDoc = Pdfium()
                textDoc!!.open(textFd!!.fileDescriptor)
                Log.d(TAG, "NativeView.initTextDoc() pdfium initialized successfully")
                return true
            } catch (e: Throwable) {
                Log.e(TAG, "NativeView.initTextDoc() failed to initialize pdfium", e)
                textDoc = null
                textFd?.close()
                textFd = null
                return false
            }
        }

        override fun close() {
            doc.close()
            try { fd.close() } catch (e: IOException) { /* ignore */ }

            // Закрываем pdfium для текста
            textDoc?.close()
            textDoc = null
            try { textFd?.close() } catch (e: IOException) { /* ignore */ }
            textFd = null
        }

        override fun getPageInfo(w: Int, h: Int, c: ScrollWidget.ScrollAdapter.PageCursor): Plugin.Page? {
            val page: Int = if (c.start == null) c.end.paragraphIndex - 1 else c.start.paragraphIndex
            return NativePage(doc, page, w, h)
        }

        override fun draw(bitmap: Canvas, w: Int, h: Int, index: ZLViewEnums.PageIndex, c: Bitmap.Config) {
            Log.d(TAG, "NativeView.draw() index=$index, w=$w, h=$h, config=$c")
            val curr = current as NativePage
            // gotoPosition() открывает страницу через loadPage() → doc.openPage().
            // PdfRenderer бросает IllegalStateException при повторном открытии той же страницы.
            // Закрываем перед созданием NativePage, который снова откроет её в loadPage().
            curr.page?.close()
            curr.page = null
            val r = NativePage(curr, index, w, h)
            if (index == ZLViewEnums.PageIndex.current) current!!.updatePage(r)
            r.scale(w, h)
            val render = r.renderRect()

            Log.d(TAG, "NativeView.draw() after scale: pageBox=${r.pageBox!!.w}x${r.pageBox!!.h} render.src=${render.src} render.dst=${render.dst}")

            // PdfRenderer требует ARGB_8888, не поддерживает RGB_565
            val config = Bitmap.Config.ARGB_8888
            Log.d(TAG, "NativeView.draw() creating bitmap ${r.pageBox!!.w}x${r.pageBox!!.h} with config=$config")
            val bm = Bitmap.createBitmap(r.pageBox!!.w, r.pageBox!!.h, config)
            bm.eraseColor(FBReaderView.PAGE_PAPER_COLOR)

            try {
                Log.d(TAG, "NativeView.draw() rendering page ${r.pageNumber}")
                r.page!!.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap.drawBitmap(bm, render.toRect(bm.width, bm.height), render.dst!!, paint)
            } finally {
                bm.recycle()
                // НЕ закрываем страницу здесь - она нужна для PagerWidget.getPageRect()
                // Страница будет закрыта при следующем вызове loadPage() или в close()
            }
        }

        override fun select(page: Selection.Page, point: Selection.Point): Selection? {
            if (!initTextDoc()) {
                Log.w(TAG, "NativeView.select() pdfium not available for text operations")
                return null
            }

            try {
                Log.d(TAG, "NativeView.select() creating selection at page=${page.page}, point=(${point.x}, ${point.y})")
                // Используем Selection из PdfiumView
                val selPage = SelectionPage(textDoc!!, page)
                if (selPage.count > 0) {
                    return Selection(textDoc!!, selPage, point)
                }
                selPage.close()
            } catch (e: Throwable) {
                Log.e(TAG, "NativeView.select() error creating selection", e)
            }
            return null
        }

        override fun select(start: ZLTextPosition, end: ZLTextPosition): Selection? {
            if (!initTextDoc()) {
                Log.w(TAG, "NativeView.select() pdfium not available for text operations")
                return null
            }

            try {
                Log.d(TAG, "NativeView.select() creating selection from $start to $end")
                return Selection(textDoc!!, start, end)
            } catch (e: Throwable) {
                Log.e(TAG, "NativeView.select() error creating selection", e)
            }
            return null
        }

        override fun select(page: Int): Selection? {
            if (!initTextDoc()) {
                Log.w(TAG, "NativeView.select() pdfium not available for text operations")
                return null
            }

            try {
                Log.d(TAG, "NativeView.select() creating selection for page $page")
                return Selection(textDoc!!, page)
            } catch (e: Throwable) {
                Log.e(TAG, "NativeView.select() error creating selection", e)
            }
            return null
        }

        override fun getLinks(page: Selection.Page): Array<Link>? {
            if (!initTextDoc()) return null

            try {
                val p = textDoc!!.openPage(page.page)
                val ll = p.links
                return Array(ll.size) { i ->
                    val l = ll[i]
                    Link(l.uri, l.index, p.toDevice(0, 0, page.w, page.h, 0, l.bounds))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "NativeView.getLinks() error", e)
            }
            return null
        }

        override fun search(text: String): Search? {
            if (!initTextDoc()) return null

            try {
                Log.d(TAG, "NativeView.search() searching for '$text'")
                return PdfSearch(textDoc!!, text)
            } catch (e: Throwable) {
                Log.e(TAG, "NativeView.search() error", e)
            }
            return null
        }

        /**
         * Получить полный текст страницы PDF
         * @param pageNum Номер страницы (0-indexed)
         * @return Текст страницы или null при ошибке
         */
        override fun getPageText(pageNum: Int): String? {
            if (!initTextDoc()) {
                Log.w(TAG, "NativeView.getPageText() pdfium not available")
                return null
            }

            try {
                Log.d(TAG, "NativeView.getPageText() getting text for page $pageNum")
                val page = textDoc!!.openPage(pageNum)
                val text = page.open()
                val result = text.getText(0, text.count)
                text.close()
                page.close()
                Log.d(TAG, "NativeView.getPageText() got ${result?.length ?: 0} chars")
                return result
            } catch (e: Throwable) {
                Log.e(TAG, "NativeView.getPageText() error getting text for page $pageNum", e)
            }
            return null
        }
    }

    class PdfiumPage : Plugin.Page {
        var doc: Pdfium

        constructor(r: PdfiumPage) : super(r) { doc = r.doc }

        constructor(r: PdfiumPage, index: ZLViewEnums.PageIndex, w: Int, h: Int) : this(r) {
            this.w = w
            this.h = h
            load(index)
            if (index == ZLViewEnums.PageIndex.current) {
                load()
                renderPage()
            }
        }

        constructor(d: Pdfium, page: Int, w: Int, h: Int) {
            doc = d
            this.w = w
            this.h = h
            pageNumber = page
            pageOffset = 0
            load()
            renderPage()
        }

        constructor(d: Pdfium) { doc = d; load() }

        override fun getPagesCount(): Int = doc.pagesCount

        override fun load() { load(pageNumber) }

        internal fun load(index: Int) {
            val s = doc.getPageSize(index)
            pageBox = Plugin.Box(0, 0, s.width, s.height)
            dpi = 72
        }
    }

    open inner class PdfiumView(f: ZLFile) : Plugin.View() {
        var doc: Pdfium
        var fd: ParcelFileDescriptor

        init {
            Log.d(TAG, "PdfiumView.init() file=${f.path}, exists=${f.exists()}")

            // Проверяем размер файла
            val file = File(f.path)
            val fileSize = if (file.exists()) file.length() else -1L
            Log.d(TAG, "PdfiumView.init() fileSize=$fileSize bytes")

            if (!file.exists()) {
                Log.e(TAG, "PdfiumView.init() file does not exist!")
                throw IllegalStateException("File does not exist: ${f.path}")
            }

            if (fileSize == 0L) {
                Log.e(TAG, "PdfiumView.init() file is empty!")
                throw IllegalStateException("File is empty: ${f.path}")
            }

            try {
                Log.d(TAG, "PdfiumView.init() creating Pdfium instance")
                doc = Pdfium()
                Log.d(TAG, "PdfiumView.init() opening ParcelFileDescriptor")
                fd = ParcelFileDescriptor.open(File(f.path), ParcelFileDescriptor.MODE_READ_ONLY)
                Log.d(TAG, "PdfiumView.init() fd=$fd")

                if (fd.fileDescriptor == null) {
                    Log.e(TAG, "PdfiumView.init() fileDescriptor is null")
                    fd.close()
                    throw IllegalStateException("FileDescriptor is null")
                }

                Log.d(TAG, "PdfiumView.init() calling doc.open()...")
                doc.open(fd.fileDescriptor)
                Log.d(TAG, "PdfiumView.init() doc opened, pages=${doc.pagesCount}")
                current = PdfiumPage(doc)
                Log.d(TAG, "PdfiumView.init() completed successfully")
            } catch (e: IOException) {
                Log.e(TAG, "PdfiumView.init() IOException", e)
                throw IllegalStateException(e)
            } catch (e: Throwable) {
                Log.e(TAG, "PdfiumView.init() unexpected error", e)
                throw e
            }
        }

        override fun close() {
            doc.close()
            try { fd.close() } catch (e: IOException) { throw IllegalStateException(e) }
        }

        override fun getPageInfo(w: Int, h: Int, c: ScrollWidget.ScrollAdapter.PageCursor): Plugin.Page? {
            val page: Int = if (c.start == null) c.end.paragraphIndex - 1 else c.start.paragraphIndex
            return PdfiumPage(doc, page, w, h)
        }

        override fun render(w: Int, h: Int, page: Int, c: Bitmap.Config): Bitmap? {
            // Ограничиваем масштаб на больших экранах, чтобы избежать краша pdfium
            // На планшетах w и h уже большие, не нужно удваивать
            val maxDimension = 2000
            val scaleMultiplier = if (w > maxDimension || h > maxDimension) 1 else 2

            Log.d(TAG, "render() page=$page, w=$w, h=$h, scaleMultiplier=$scaleMultiplier")
            val r = PdfiumPage(doc, page, w, h)
            r.scale(w * scaleMultiplier, h * scaleMultiplier)
            Log.d(TAG, "render() creating bitmap: ${r.pageBox!!.w}x${r.pageBox!!.h}")
            val bm = Bitmap.createBitmap(r.pageBox!!.w, r.pageBox!!.h, c)
            Log.d(TAG, "render() opening page $page")
            val p = doc.openPage(r.pageNumber)
            Log.d(TAG, "render() rendering page")
            p.render(bm, 0, 0, bm.width, bm.height)
            p.close()
            bm.density = r.dpi
            Log.d(TAG, "render() completed")
            return bm
        }

        override fun draw(bitmap: Canvas, w: Int, h: Int, index: ZLViewEnums.PageIndex, c: Bitmap.Config) {
            val curr = current as PdfiumPage
            val r = PdfiumPage(curr, index, w, h)
            if (index == ZLViewEnums.PageIndex.current) current!!.updatePage(r)
            r.scale(w, h)
            val render = r.renderRect()
            val p = doc.openPage(r.pageNumber)
            val bm = Bitmap.createBitmap(r.pageBox!!.w, r.pageBox!!.h, c)
            bm.eraseColor(FBReaderView.PAGE_PAPER_COLOR)
            p.render(bm, 0, 0, bm.width, bm.height)
            p.close()
            bitmap.drawBitmap(bm, render.toRect(bm.width, bm.height), render.dst!!, paint)
            bm.recycle()
        }

        override fun select(page: Plugin.View.Selection.Page, point: Plugin.View.Selection.Point): Plugin.View.Selection? {
            val start = SelectionPage(doc, page)
            if (start.count > 0) {
                val s = Selection(doc, start, point)
                if (s.isEmpty()) { s.close(); return null }
                return s
            }
            start.close()
            return null
        }

        override fun select(start: ZLTextPosition, end: ZLTextPosition): Plugin.View.Selection? {
            val s = Selection(doc, start, end)
            if (s.isEmpty()) { s.close(); return null }
            return s
        }

        override fun select(page: Int): Plugin.View.Selection? {
            val s = Selection(doc, page)
            if (s.isEmpty()) { s.close(); return null }
            return s
        }

        override fun getLinks(page: Plugin.View.Selection.Page): Array<Link>? {
            val p = doc.openPage(page.page)
            val ll = p.links
            return Array(ll.size) { i ->
                val l = ll[i]
                Link(l.uri, l.index, p.toDevice(0, 0, page.w, page.h, 0, l.bounds))
            }
        }

        override fun search(text: String): Search? {
            val s = PdfSearch(doc, text)
            for (i in 0 until doc.pagesCount) {
                if (s.hasText(i)) return s
            }
            s.close()
            return null
        }

        /**
         * Получить полный текст страницы PDF
         * @param pageNum Номер страницы (0-indexed)
         * @return Текст страницы или null при ошибке
         */
        override fun getPageText(pageNum: Int): String? {
            try {
                Log.d(TAG, "PdfiumView.getPageText() getting text for page $pageNum")
                val page = doc.openPage(pageNum)
                val text = page.open()
                val result = text.getText(0, text.count)
                text.close()
                page.close()
                Log.d(TAG, "PdfiumView.getPageText() got ${result?.length ?: 0} chars")
                return result
            } catch (e: Throwable) {
                Log.e(TAG, "PdfiumView.getPageText() error getting text for page $pageNum", e)
            }
            return null
        }
    }

    inner class PDFTextModel(f: ZLFile) : PdfiumView(f), ZLTextModel {
        protected fun finalize() { close() }
        override fun getId(): String? = null
        override fun getLanguage(): String? = null
        override fun getParagraphsNumber(): Int = doc.pagesCount
        override fun getParagraph(index: Int): ZLTextParagraph = object : ZLTextParagraph {
            override fun iterator(): ZLTextParagraph.EntryIterator? = null
            override fun getKind(): Byte = ZLTextParagraph.Kind.END_OF_TEXT_PARAGRAPH
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
