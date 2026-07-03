package com.github.axet.bookreader.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
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

        @JvmStatic
        fun create(info: Storage.Info): PDFPlugin {
            if (Config.natives) {
                Natives.loadLibraries(info.context, "modpdfium", "pdfiumjni")
                Config.natives = false
            }
            return PDFPlugin(info)
        }
    }

    override fun create(fbook: Storage.FBook): Plugin.View {
        return PdfiumView(BookUtil.fileByBook(fbook.book))
    }

    @Throws(BookReadingException::class)
    override fun readMetainfo(book: AbstractBook) {
        val f = BookUtil.fileByBook(book)
        try {
            val doc = Pdfium()
            val fd = ParcelFileDescriptor.open(File(f.path), ParcelFileDescriptor.MODE_READ_ONLY)
            doc.open(fd.fileDescriptor)
            book.addAuthor(doc.getMeta(Pdfium.META_AUTHOR))
            book.setTitle(doc.getMeta(Pdfium.META_TITLE))
            doc.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
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
        val bm = Bitmap.createBitmap(view.current!!.pageBox!!.w, view.current!!.pageBox!!.h, Bitmap.Config.RGB_565)
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
        val bookmarks = m.doc.toc
        loadTOC(0, 0, bookmarks, model.TOCTree)
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
            for (i in 0 until pdfium.pagesCount) {
                all.addAll(search(Plugin.View.Selection.odd(page, i, pdfium.pagesCount)))
                if (all.isNotEmpty()) return
            }
        }
    }

    class NativePage : Plugin.Page {
        var doc: PdfRenderer
        var page: PdfRenderer.Page? = null

        constructor(r: NativePage) : super(r) { doc = r.doc }

        constructor(r: NativePage, index: ZLViewEnums.PageIndex, w: Int, h: Int) : this(r) {
            this.w = w
            this.h = h
            load(index)
            if (index == ZLViewEnums.PageIndex.current) {
                load()
                renderPage()
            }
        }

        constructor(d: PdfRenderer) { doc = d }

        override fun getPagesCount(): Int = doc.pageCount

        override fun load() {
            page?.close()
            page = doc.openPage(pageNumber)
            pageBox = Plugin.Box(0, 0, page!!.width, page!!.height)
        }
    }

    class NativeView(f: ZLFile) : Plugin.View() {
        var doc: PdfRenderer

        init {
            try {
                val fd = ParcelFileDescriptor.open(File(f.path), ParcelFileDescriptor.MODE_READ_ONLY)
                doc = PdfRenderer(fd)
                current = NativePage(doc)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        override fun close() { doc.close() }

        override fun draw(bitmap: Canvas, w: Int, h: Int, index: ZLViewEnums.PageIndex, c: Bitmap.Config) {
            val curr = current as NativePage
            val r = NativePage(curr, index, w, h)
            if (index == ZLViewEnums.PageIndex.current) current!!.updatePage(r)
            r.scale(w, h)
            val render = r.renderRect()
            val bm = Bitmap.createBitmap(r.pageBox!!.w, r.pageBox!!.h, c)
            bm.eraseColor(FBReaderView.PAGE_PAPER_COLOR)
            r.page!!.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap.drawBitmap(bm, render.toRect(bm.width, bm.height), render.dst!!, paint)
            bm.recycle()
            r.page!!.close()
            r.page = null
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
            try {
                doc = Pdfium()
                fd = ParcelFileDescriptor.open(File(f.path), ParcelFileDescriptor.MODE_READ_ONLY)
                doc.open(fd.fileDescriptor)
                current = PdfiumPage(doc)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        override fun close() {
            doc.close()
            try { fd.close() } catch (e: IOException) { throw RuntimeException(e) }
        }

        override fun getPageInfo(w: Int, h: Int, c: ScrollWidget.ScrollAdapter.PageCursor): Plugin.Page? {
            val page: Int = if (c.start == null) c.end.paragraphIndex - 1 else c.start.paragraphIndex
            return PdfiumPage(doc, page, w, h)
        }

        override fun render(w: Int, h: Int, page: Int, c: Bitmap.Config): Bitmap? {
            val r = PdfiumPage(doc, page, w, h)
            r.scale(w * 2, h * 2)
            val bm = Bitmap.createBitmap(r.pageBox!!.w, r.pageBox!!.h, c)
            val p = doc.openPage(r.pageNumber)
            p.render(bm, 0, 0, bm.width, bm.height)
            p.close()
            bm.density = r.dpi
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
