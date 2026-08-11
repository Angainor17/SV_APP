package com.github.axet.bookreader.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.SparseArray
import androidx.core.graphics.createBitmap
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter
import com.github.axet.bookreader.widgets.FBReaderView
import com.github.axet.bookreader.widgets.ScrollWidget
import io.legere.pdfiumandroid.PdfDocument
import io.legere.pdfiumandroid.PdfPage
import io.legere.pdfiumandroid.PdfTextPage
import io.legere.pdfiumandroid.PdfiumCore
import io.legere.pdfiumandroid.api.AlreadyClosedBehavior
import io.legere.pdfiumandroid.api.Bookmark
import io.legere.pdfiumandroid.api.Config
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

/**
 * PDF-плагин на базе io.legere:pdfiumandroid (современная сборка Google PDFium,
 * собрана с 16KB page alignment). Раньше использовался com.github.axet:pdfium,
 * чья нативная libmodpdfium.so собрана с 4KB alignment и падает с SIGTRAP
 * на устройствах с 16KB page size (см. docs/technical-debt.md).
 */
class PDFPlugin(info: Storage.Info) : BuiltinFormatPlugin(info, EXT), Plugin {

    companion object {
        const val EXT = "pdf"
        val TAG: String = PDFPlugin::class.java.simpleName

        // PDF-страницы измеряются в points (1/72 дюйма) — здесь это просто единица
        // масштабирования на входе в Plugin.Page.scale(), не связана с DPI экрана.
        private const val POINTS_DPI = 72

        // Допуск (в points) при определении символа под точкой касания.
        private const val CHAR_INDEX_TOLERANCE = 10.0

        // Selection/SelectionPage кэширует и переиспользует открытые PdfPage/PdfTextPage
        // (см. map/openPageNum/openSelPage) — одна и та же страница может быть закрыта
        // несколько раз (startPage/endPage — копии записи из map). Старый com.github.axet:pdfium
        // такое молча игнорировал, io.legere по умолчанию кидает исключение — возвращаем
        // старое поведение.
        private val PDFIUM_CONFIG = Config(alreadyClosedBehavior = AlreadyClosedBehavior.IGNORE)

        @JvmStatic
        fun create(info: Storage.Info): PDFPlugin = PDFPlugin(info)
    }

    override fun create(fbook: Storage.FBook): Plugin.View {
        val file = BookUtil.fileByBook(fbook.book)
        return PdfiumView(file)
    }

    @Throws(BookReadingException::class)
    override fun readMetainfo(book: AbstractBook) {
        val f = BookUtil.fileByBook(book)
        val file = File(f.path)
        if (!file.exists()) throw IllegalStateException("File does not exist: ${f.path}")
        if (file.length() == 0L) throw IllegalStateException("File is empty: ${f.path}")
        try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val document = PdfiumCore(config = PDFIUM_CONFIG).newDocument(fd)
            val meta = document.getDocumentMeta()
            book.addAuthor(meta.author)
            book.setTitle(meta.title)
            document.close()
            fd.close()
        } catch (e: IOException) {
            Log.e(TAG, "readMetainfo() failed for ${f.path}", e)
            throw IllegalStateException(e)
        }
    }

    @Throws(BookReadingException::class)
    override fun readUids(book: AbstractBook) {
    }

    @Throws(BookReadingException::class)
    override fun detectLanguageAndEncoding(book: AbstractBook) {
    }

    override fun readCover(f: ZLFile): ZLImage {
        val view = PdfiumView(f)
        view.current!!.scale(CacheImagesAdapter.COVER_SIZE, CacheImagesAdapter.COVER_SIZE)
        val bm = createBitmap(
            view.current!!.pageBox!!.w,
            view.current!!.pageBox!!.h,
            Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bm)
        view.drawWallpaper(canvas)
        view.draw(canvas, bm.width, bm.height, ZLViewEnums.PageIndex.current)
        view.close()
        return ZLBitmapImage(bm)
    }

    override fun readAnnotation(file: ZLFile): String? = null

    override fun priority(): Int = 0

    override fun supportedEncodings(): EncodingCollection? = null

    @Throws(BookReadingException::class)
    override fun readModel(model: BookModel) {
        val m = PDFTextModel(BookUtil.fileByBook(model.Book))
        model.setBookTextModel(m)
        loadTOC(m.document.getTableOfContents(), model.TOCTree)
    }

    /**
     * Новый Bookmark уже дерево (children), в отличие от старого плоского массива с level —
     * рекурсия по дереву напрямую, без индексов/уровней.
     */
    private fun loadTOC(bookmarks: List<Bookmark>, tree: TOCTree) {
        for (b in bookmarks) {
            val title = b.title
            if (title.isNullOrEmpty()) {
                // Без названия элемент в TOCTree не добавляем, но детей не теряем —
                // подвешиваем их на том же уровне, что и пропущенный узел.
                loadTOC(b.children, tree)
                continue
            }
            val t = TOCTree(tree)
            t.text = title
            t.setReference(null, b.pageIdx.toInt())
            loadTOC(b.children, t)
        }
    }

    class UL : Comparator<Rect> {
        override fun compare(o1: Rect, o2: Rect): Int {
            val r = o2.top.compareTo(o1.top)
            return if (r != 0) r else o1.left.compareTo(o2.left)
        }
    }

    class SelectionPage {
        var page: Int
        var ppage: PdfPage
        var text: PdfTextPage
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

        constructor(document: PdfDocument, selPage: PluginView.Selection.Page) :
                this(selPage.page, document.openPage(selPage.page)!!, selPage.w, selPage.h)

        constructor(document: PdfDocument, pageNum: Int) :
                this(pageNum, document.openPage(pageNum)!!, 0, 0)

        constructor(p: Int, page: PdfPage, w: Int, h: Int) {
            this.page = p
            this.ppage = page
            this.text = page.openTextPage()
            this.count = text.textPageCountChars()
            this.w = w
            this.h = h
            val n = text.textPageCountRects(0, count)
            this.sorted = Array(n) { i -> text.textPageGetRect(i)!!.toIntRect() }
            sorted.sortWith(UL())
        }

        fun first(): Int {
            for (r in sorted) {
                val k = Rect(r)
                var idx: Int
                do {
                    idx = text.textPageGetCharIndexAtPos(
                        k.left.toDouble(),
                        k.centerY().toDouble(),
                        CHAR_INDEX_TOLERANCE,
                        CHAR_INDEX_TOLERANCE,
                    )
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

    inner class Selection : PluginView.Selection {
        var document: PdfDocument
        var startPage: SelectionPage? = null
        var endPage: SelectionPage? = null
        var map: SparseArray<SelectionPage> = SparseArray()

        constructor(document: PdfDocument, page: SelectionPage, point: Point) {
            this.document = document
            map.put(page.page, page)
            val p = page.ppage.mapDeviceCoordsToPage(0, 0, page.w, page.h, 0, point.x, point.y)
            selectWord(page, p)
        }

        constructor(document: PdfDocument, start: ZLTextPosition, end: ZLTextPosition) {
            this.document = document
            this.startPage = openPageNum(start.paragraphIndex)
            this.startPage!!.index = start.elementIndex
            this.endPage = openPageNum(end.paragraphIndex)
            this.endPage!!.index = end.elementIndex
        }

        constructor(document: PdfDocument, page: Int) {
            this.document = document
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
            var s = p.text.textPageGetText(i, 1) ?: return false
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
                p = SelectionPage(document, selPage)
                map.put(p.page, p)
            }
            return SelectionPage(p)
        }

        internal fun openPageNum(pageNum: Int): SelectionPage {
            var p = map.get(pageNum)
            if (p == null) {
                p = SelectionPage(document, pageNum)
                map.put(p.page, p)
            }
            return SelectionPage(p)
        }

        private fun selectWord(page: SelectionPage, point: PointF) {
            startPage = page
            val idx = startPage!!.text.textPageGetCharIndexAtPos(
                point.x.toDouble(), point.y.toDouble(), CHAR_INDEX_TOLERANCE, CHAR_INDEX_TOLERANCE,
            )
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
                val p = sp.ppage.mapDeviceCoordsToPage(0, 0, page.w, page.h, 0, point.x, point.y)
                val idx = sp.text.textPageGetCharIndexAtPos(
                    p.x.toDouble(),
                    p.y.toDouble(),
                    CHAR_INDEX_TOLERANCE,
                    CHAR_INDEX_TOLERANCE
                )
                if (idx == -1) return
                sp.index = idx
                startPage = sp
            }
        }

        override fun setEnd(page: Page, point: Point) {
            val ep = openSelPage(page)
            if (ep.count > 0) {
                val p = ep.ppage.mapDeviceCoordsToPage(0, 0, page.w, page.h, 0, point.x, point.y)
                val idx = ep.text.textPageGetCharIndexAtPos(
                    p.x.toDouble(),
                    p.y.toDouble(),
                    CHAR_INDEX_TOLERANCE,
                    CHAR_INDEX_TOLERANCE
                )
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
            return b.page.text.textPageGetText(b.ss, b.cc) ?: ""
        }

        override fun getBoundsAll(page: Page): Array<Rect>? {
            val p = openSelPage(page)
            val n = p.text.textPageCountRects(0, p.count)
            return Array(n) { i ->
                val r = p.text.textPageGetRect(i)!!
                p.ppage.mapRectToDevice(0, 0, p.w, p.h, 0, r)
            }
        }

        override fun getBounds(p: Page): Bounds {
            val bounds = Bounds()
            val b = SelectionBounds(p)
            bounds.reverse = b.reverse
            bounds.start = b.first
            bounds.end = b.last
            val n = b.page.text.textPageCountRects(b.ss, b.cc)
            bounds.rr = Array(n) { i ->
                val r = b.page.text.textPageGetRect(i)!!
                b.page.ppage.mapRectToDevice(0, 0, b.page.w, b.page.h, 0, r)
            }
            return bounds
        }

        override fun inBetween(page: Page, start: Point, end: Point): Boolean? {
            val b = SelectionBounds(page)
            if (b.s.page < page.page && page.page < b.e.page) return true
            if (b.page.count > 0) {
                val p1 =
                    b.page.ppage.mapDeviceCoordsToPage(0, 0, page.w, page.h, 0, start.x, start.y)
                val i1 = b.page.text.textPageGetCharIndexAtPos(
                    p1.x.toDouble(),
                    p1.y.toDouble(),
                    CHAR_INDEX_TOLERANCE,
                    CHAR_INDEX_TOLERANCE
                )
                if (i1 == -1) return null
                val p2 = b.page.ppage.mapDeviceCoordsToPage(0, 0, page.w, page.h, 0, end.x, end.y)
                val i2 = b.page.text.textPageGetCharIndexAtPos(
                    p2.x.toDouble(),
                    p2.y.toDouble(),
                    CHAR_INDEX_TOLERANCE,
                    CHAR_INDEX_TOLERANCE
                )
                if (i2 == -1) return null
                if (i2 < i1) return null
                return i1 <= b.ss && b.ss <= i2 || i1 <= b.ll && b.ll <= i2
            }
            return null
        }

        override fun isValid(page: Page, point: Point): Boolean {
            val b = SelectionBounds(page)
            if (b.page.count > 0) {
                val p =
                    b.page.ppage.mapDeviceCoordsToPage(0, 0, page.w, page.h, 0, point.x, point.y)
                return b.page.text.textPageGetCharIndexAtPos(
                    p.x.toDouble(),
                    p.y.toDouble(),
                    CHAR_INDEX_TOLERANCE,
                    CHAR_INDEX_TOLERANCE
                ) != -1
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
                val p =
                    b.page.ppage.mapDeviceCoordsToPage(0, 0, page.w, page.h, 0, point.x, point.y)
                val idx = b.page.text.textPageGetCharIndexAtPos(
                    p.x.toDouble(),
                    p.y.toDouble(),
                    CHAR_INDEX_TOLERANCE,
                    CHAR_INDEX_TOLERANCE
                )
                if (idx == -1) return null
                return b.ss < idx || b.ll < idx
            }
            return null
        }

        override fun isBelow(page: Page, point: Point): Boolean? {
            val b = SelectionBounds(page)
            if (b.e.page > page.page) return true
            if (b.page.count > 0) {
                val p =
                    b.page.ppage.mapDeviceCoordsToPage(0, 0, page.w, page.h, 0, point.x, point.y)
                val idx = b.page.text.textPageGetCharIndexAtPos(
                    p.x.toDouble(),
                    p.y.toDouble(),
                    CHAR_INDEX_TOLERANCE,
                    CHAR_INDEX_TOLERANCE
                )
                if (idx == -1) return null
                return idx < b.ss || idx < b.ll
            }
            return null
        }

        override fun close() {
            // startPage/endPage — shallow-копии (или тот же объект) записи из map: их
            // ppage/text — те же самые native-объекты, что и в map. Закрываем каждый
            // native ppage/text РОВНО ОДИН РАЗ через map, иначе двойной FPDF_ClosePage()
            // на одном указателе — heap corruption/SIGBUS (io.legere.pdfiumandroid не
            // терпит double-close так же терпимо, как старый com.github.axet:pdfium).
            startPage = null
            endPage = null
            for (i in 0 until map.size()) map.valueAt(i).close()
            map.clear()
        }

        override fun getStart(): ZLTextPosition? =
            startPage?.let { ZLTextFixedPosition(it.page, it.index, 0) }

        override fun getEnd(): ZLTextPosition? =
            endPage?.let { ZLTextFixedPosition(it.page, it.index, 0) }

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

    inner class PdfSearch(val document: PdfDocument, val str: String) : PluginView.Search() {
        var all: ArrayList<SearchResult> = ArrayList()
        var pages: SparseArray<ArrayList<SearchResult>> = SparseArray()
        var matchIndex: Int = -1
        var initialPage: Int = -1

        internal fun hasText(page: Int): Boolean {
            val p = document.openPage(page) ?: return false
            val t = p.openTextPage()
            try {
                return t.textPageCountChars() > 0
            } finally {
                t.close()
                p.close()
            }
        }

        internal fun search(i: Int): ArrayList<SearchResult> {
            val pg = document.openPage(i)!!
            val text = pg.openTextPage()
            val pattern = str.lowercase(Locale.US)
            val rr = ArrayList<SearchResult>()
            val count = text.textPageCountChars()
            if (count > 0) {
                val txt = (text.textPageGetText(0, count) ?: "").lowercase(Locale.US)
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

        override fun getBounds(page: PluginView.Selection.Page): Bounds? {
            val bounds = Bounds()
            val list = pages.get(page.page) ?: return null
            val p = document.openPage(page.page)!!
            val t = p.openTextPage()
            val rr = ArrayList<Rect>()
            for (r in list) {
                val hh = ArrayList<Rect>()
                val n = t.textPageCountRects(r.start, r.count)
                for (i in 0 until n) {
                    val b = t.textPageGetRect(i)!!
                    val rect = p.mapRectToDevice(0, 0, page.w, page.h, 0, b)
                    rr.add(rect)
                    hh.add(rect)
                }
                if (matchIndex >= 0 && r == all[matchIndex]) {
                    bounds.highlight = hh.toTypedArray()
                }
            }
            bounds.rr = rr.toTypedArray()
            t.close()
            p.close()
            return bounds
        }

        override fun getCount(): Int = all.size

        /**
         * Разрешает индекс текущего результата (первого совпадения на initialPage
         * или ближайшего после неё), не сдвигая позицию, если она уже известна.
         */
        private fun resolveIndex() {
            if (matchIndex == -1 && initialPage != -1) {
                for (i in all.indices) {
                    if (all[i].page >= initialPage) {
                        matchIndex = i
                        return
                    }
                }
                matchIndex = all.size - 1
            }
        }

        override fun getIndex(): Int {
            if (all.isEmpty()) return -1
            resolveIndex()
            return matchIndex
        }

        override fun next(): Int {
            if (all.isEmpty()) return -1
            if (matchIndex == -1 && initialPage != -1) {
                resolveIndex()
                return all[matchIndex].page
            }
            matchIndex++
            if (matchIndex >= all.size) {
                for (i in all[matchIndex - 1].page + 1 until document.getPageCount()) {
                    all.addAll(search(i))
                    if (matchIndex < all.size) return all[matchIndex].page
                }
                matchIndex = all.size - 1
            }
            return all[matchIndex].page
        }

        override fun prev(): Int {
            if (all.isEmpty()) return -1
            if (matchIndex == -1 && initialPage != -1) {
                for (i in all.size - 1 downTo 0) {
                    if (all[i].page <= initialPage) {
                        var j = i
                        while (j >= 0 && all[j].page == initialPage) j--
                        matchIndex = j + 1
                        return all[matchIndex].page
                    }
                }
            }
            matchIndex--
            if (matchIndex < 0) {
                val r = all[0]
                for (i in r.page - 1 downTo 1) {
                    all.addAll(0, search(i))
                    matchIndex = all.indexOf(r) - 1
                    if (matchIndex >= 0) return all[matchIndex].page
                }
                matchIndex = 0
            }
            return all[matchIndex].page
        }

        override fun setPage(page: Int) {
            this.initialPage = page
            if (str.isEmpty()) return
            // Search all pages in the book (no limit)
            for (i in 0 until document.getPageCount()) {
                all.addAll(search(PluginView.Selection.odd(page, i, document.getPageCount())))
            }
            // pages are visited in "odd" order (near current page first), so
            // sort results back into book order for correct navigation/indexing
            all.sortWith(compareBy({ it.page }, { it.start }))
        }
    }

    class PdfiumPage : Plugin.Page {
        var document: PdfDocument

        constructor(r: PdfiumPage) : super(r) {
            document = r.document
        }

        constructor(r: PdfiumPage, index: ZLViewEnums.PageIndex, w: Int, h: Int) : this(r) {
            this.w = w
            this.h = h
            load(index)
            if (index == ZLViewEnums.PageIndex.current) {
                load()
                renderPage()
            }
        }

        constructor(d: PdfDocument, page: Int, w: Int, h: Int) : super() {
            document = d
            this.w = w
            this.h = h
            pageNumber = page
            pageOffset = 0
            load()
            renderPage()
        }

        constructor(d: PdfDocument) : super() {
            document = d
            load()
        }

        override fun getPagesCount(): Int = document.getPageCount()

        override fun load() {
            load(pageNumber)
        }

        internal fun load(index: Int) {
            val p = document.openPage(index)!!
            pageBox = Plugin.Box(0, 0, p.getPageWidthPoint(), p.getPageHeightPoint())
            p.close()
            dpi = POINTS_DPI
        }
    }

    open inner class PdfiumView(f: ZLFile) : Plugin.View() {
        var document: PdfDocument
        var fd: ParcelFileDescriptor

        init {
            val file = File(f.path)
            if (!file.exists()) throw IllegalStateException("File does not exist: ${f.path}")
            if (file.length() == 0L) throw IllegalStateException("File is empty: ${f.path}")
            try {
                fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                document = PdfiumCore(config = PDFIUM_CONFIG).newDocument(fd)
                current = PdfiumPage(document)
            } catch (e: IOException) {
                Log.e(TAG, "PdfiumView.init() failed for ${f.path}", e)
                throw IllegalStateException(e)
            }
        }

        override fun close() {
            document.close()
            try {
                fd.close()
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }
        }

        override fun getPageInfo(
            w: Int,
            h: Int,
            c: ScrollWidget.ScrollAdapter.PageCursor
        ): Plugin.Page? {
            val page: Int =
                if (c.start == null) c.end.paragraphIndex - 1 else c.start.paragraphIndex
            return PdfiumPage(document, page, w, h)
        }

        override fun render(w: Int, h: Int, page: Int, c: Bitmap.Config): Bitmap? {
            // Ограничиваем масштаб на больших экранах, чтобы избежать чрезмерного потребления памяти
            val maxDimension = 2000
            val scaleMultiplier = if (w > maxDimension || h > maxDimension) 1 else 2

            val r = PdfiumPage(document, page, w, h)
            r.scale(w * scaleMultiplier, h * scaleMultiplier)
            val bm = createBitmap(r.pageBox!!.w, r.pageBox!!.h, c)
            val p = document.openPage(r.pageNumber)!!
            p.renderPageBitmap(bm, 0, 0, bm.width, bm.height, renderAnnot = false, textMask = false)
            p.close()
            bm.density = r.dpi
            return bm
        }

        override fun draw(
            bitmap: Canvas,
            w: Int,
            h: Int,
            index: ZLViewEnums.PageIndex,
            c: Bitmap.Config
        ) {
            val curr = current as PdfiumPage
            val r = PdfiumPage(curr, index, w, h)
            if (index == ZLViewEnums.PageIndex.current) current!!.updatePage(r)
            r.scale(w, h)
            val render = r.renderRect()
            val p = document.openPage(r.pageNumber)!!
            val bm = createBitmap(r.pageBox!!.w, r.pageBox!!.h, c)
            bm.eraseColor(FBReaderView.PAGE_PAPER_COLOR)
            p.renderPageBitmap(bm, 0, 0, bm.width, bm.height, renderAnnot = false, textMask = false)
            p.close()
            bitmap.drawBitmap(bm, render.toRect(bm.width, bm.height), render.dst!!, paint)
            bm.recycle()
        }

        override fun select(
            page: PluginView.Selection.Page,
            point: PluginView.Selection.Point
        ): PluginView.Selection? {
            val start = SelectionPage(document, page)
            if (start.count > 0) {
                val s = Selection(document, start, point)
                if (s.isEmpty()) {
                    s.close(); return null
                }
                return s
            }
            start.close()
            return null
        }

        override fun select(start: ZLTextPosition, end: ZLTextPosition): PluginView.Selection? {
            val s = Selection(document, start, end)
            if (s.isEmpty()) {
                s.close(); return null
            }
            return s
        }

        override fun select(page: Int): PluginView.Selection? {
            val s = Selection(document, page)
            if (s.isEmpty()) {
                s.close(); return null
            }
            return s
        }

        override fun getLinks(page: PluginView.Selection.Page): Array<Link>? {
            val p = document.openPage(page.page)!!
            val ll = p.getPageLinks()
            val links = Array(ll.size) { i ->
                val l = ll[i]
                Link(
                    l.uri,
                    l.destPageIdx ?: -1,
                    p.mapRectToDevice(0, 0, page.w, page.h, 0, l.bounds)
                )
            }
            p.close()
            return links
        }

        override fun search(text: String): Search? {
            val s = PdfSearch(document, text)
            for (i in 0 until document.getPageCount()) {
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
                val page = document.openPage(pageNum) ?: return null
                val text = page.openTextPage()
                val result = text.textPageGetText(0, text.textPageCountChars())
                text.close()
                page.close()
                return result
            } catch (e: Throwable) {
                Log.e(TAG, "PdfiumView.getPageText() error getting text for page $pageNum", e)
            }
            return null
        }
    }

    inner class PDFTextModel(f: ZLFile) : PdfiumView(f), ZLTextModel {
        protected fun finalize() {
            close()
        }

        override fun getId(): String? = null
        override fun getLanguage(): String? = null
        override fun getParagraphsNumber(): Int = document.getPageCount()
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
        override fun search(
            text: String,
            startIndex: Int,
            endIndex: Int,
            ignoreCase: Boolean
        ): Int = 0
    }
}

private fun RectF.toIntRect(): Rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
