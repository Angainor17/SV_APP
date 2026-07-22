package com.github.axet.bookreader.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.SparseArray
import com.github.axet.androidlibrary.app.Natives
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter
import com.github.axet.bookreader.widgets.FBReaderView
import com.github.axet.bookreader.widgets.ScrollWidget
import com.github.axet.djvulibre.Config
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
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.util.Locale
import com.github.axet.djvulibre.DjvuLibre as LibDjvu

class DjvuPlugin(info: Storage.Info) : BuiltinFormatPlugin(info, EXT), Plugin {

    companion object {
        const val EXT = "djvu"
        val TAG: String = DjvuPlugin::class.java.simpleName

        @JvmField
        val TYPES = intArrayOf(
            LibDjvu.ZONE_CHARACTER, LibDjvu.ZONE_WORD, LibDjvu.ZONE_LINE,
            LibDjvu.ZONE_PARAGRAPH, LibDjvu.ZONE_REGION, LibDjvu.ZONE_COLUMN,
            LibDjvu.ZONE_PAGE
        )

        @JvmStatic
        fun create(info: Storage.Info): DjvuPlugin {
            if (Config.natives) {
                Natives.loadLibraries(info.context, "djvu", "djvulibrejni")
                Config.natives = false
            }
            return DjvuPlugin(info)
        }

        @JvmStatic
        fun toPage(info: LibDjvu.Page, w: Int, h: Int, point: Plugin.View.Selection.Point): Plugin.View.Selection.Point {
            return Plugin.View.Selection.Point(
                point.x * info.width / w,
                info.height - point.y * info.height / h
            )
        }

        @JvmStatic
        fun toDevice(info: LibDjvu.Page, w: Int, h: Int, point: Plugin.View.Selection.Point): Plugin.View.Selection.Point {
            return Plugin.View.Selection.Point(
                point.x * w / info.width,
                (info.height - point.y) * h / info.height
            )
        }

        @JvmStatic
        fun toDevice(info: LibDjvu.Page, w: Int, h: Int, rect: Rect): Rect {
            val p1 = toDevice(info, w, h, Plugin.View.Selection.Point(rect.left, rect.top))
            val p2 = toDevice(info, w, h, Plugin.View.Selection.Point(rect.right, rect.bottom))
            return Rect(p1.x, p2.y, p2.x, p1.y)
        }
    }

    override fun create(fbook: Storage.FBook): Plugin.View {
        return DjvuView(BookUtil.fileByBook(fbook.book))
    }

    @Throws(BookReadingException::class)
    override fun readMetainfo(book: AbstractBook) {
        val f = BookUtil.fileByBook(book)
        try {
            val `is` = FileInputStream(f.path)
            val doc = DjvuLibre(`is`.fd)
            book.setTitle(doc.getMeta(LibDjvu.META_TITLE))
            book.addAuthor(doc.getMeta(LibDjvu.META_AUTHOR))
            doc.close()
            `is`.close()
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }

    @Throws(BookReadingException::class)
    override fun readUids(book: AbstractBook) {}

    @Throws(BookReadingException::class)
    override fun detectLanguageAndEncoding(book: AbstractBook) {}

    override fun readCover(file: ZLFile): ZLImage {
        val view = DjvuView(file)
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
        val m = DjvuTextModel(BookUtil.fileByBook(model.Book))
        model.setBookTextModel(m)
        val bookmarks = m.doc.bookmarks ?: return
        loadTOC(0, 0, bookmarks, model.TOCTree)
    }

    internal fun loadTOC(pos: Int, level: Int, bb: Array<LibDjvu.Bookmark>, tree: TOCTree): Int {
        var count = 0
        var last: TOCTree? = null
        var i = pos
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

    class DjvuLibre(fd: FileDescriptor) : LibDjvu(fd) {
        private val cache = SparseArray<Page>()

        override fun getPageInfo(index: Int): Page {
            var p = cache.get(index)
            if (p == null) {
                p = super.getPageInfo(index)
                cache.put(index, p)
            }
            return p
        }
    }

    class SelectionPage {
        var info: LibDjvu.Page? = null
        var page: Int = 0
        var w: Int = 0
        var h: Int = 0
        var index: Int = -1
        var text: LibDjvu.Text? = null

        constructor()

        constructor(p: SelectionPage) {
            this.page = p.page
            this.info = p.info
            this.w = p.w
            this.h = p.h
            this.index = p.index
            this.text = p.text
        }

        fun getText(b: Int): String = text!!.text[b]

        fun find(point: Plugin.View.Selection.Point): Int {
            if (text == null) return -1
            for (i in text!!.bounds.indices) {
                val b = text!!.bounds[i]
                if (b.contains(point.x, point.y)) return i
            }
            return -1
        }

        fun first(): Int = 0
        fun last(): Int = text!!.bounds.size - 1
    }

    inner class Selection : Plugin.View.Selection {
        var doc: DjvuLibre
        var start: SelectionPage? = null
        var end: SelectionPage? = null
        val map = SparseArray<SelectionPage>()

        constructor(doc: DjvuLibre, page: Page, point: Point) {
            this.doc = doc
            val p = open(page)
            val convertedPoint = toPage(p.info!!, page.w, page.h, point)
            selectWord(p, convertedPoint)
        }

        constructor(doc: DjvuLibre, start: ZLTextPosition, end: ZLTextPosition) {
            this.doc = doc
            this.start = open(start.paragraphIndex)
            this.start!!.index = start.elementIndex
            this.end = open(end.paragraphIndex)
            this.end!!.index = end.elementIndex
        }

        constructor(doc: DjvuLibre, page: Int) {
            this.doc = doc
            this.start = open(page)
            this.start!!.index = 0
            this.end = open(page)
            this.end!!.index = this.end!!.text!!.text.size
        }

        fun open(page: Page): SelectionPage {
            val pp = open(page.page)
            pp.w = page.w
            pp.h = page.h
            return SelectionPage(pp)
        }

        fun open(page: Int): SelectionPage {
            var pp = map.get(page)
            if (pp == null) {
                pp = SelectionPage()
                map.put(page, pp)
                pp.page = page
                pp.info = doc.getPageInfo(page)
                for (type in TYPES) {
                    pp.text = doc.getText(page, type)
                    if (pp.text != null && pp.text!!.bounds.isNotEmpty()) break
                }
            }
            return SelectionPage(pp)
        }

        fun isEmpty(): Boolean = start == null || end == null

        internal fun isWord(pp: SelectionPage, start: Int, b: Int): Boolean {
            if (start == -1) {
                val s = pp.getText(b)
                for (c in s.toCharArray()) {
                    if (isWord(c)) return true
                }
            } else {
                val s = pp.getText(b)
                for (c in s.toCharArray()) {
                    if (!isWord(c)) return false
                }
            }
            return false
        }

        internal fun selectWord(pp: SelectionPage, point: Point) {
            val b = pp.find(point)
            if (b == -1) return
            val startPage = SelectionPage(pp)
            var s = b
            while (s != -1 && isWord(startPage, startPage.index, s)) {
                startPage.index = s
                s++
            }
            val endPage = SelectionPage(pp)
            var e = b
            while (e != -1 && isWord(endPage, endPage.index, e)) {
                endPage.index = e
                e++
            }
            if (startPage.index == -1 || endPage.index == -1) return
            this.start = startPage
            this.end = endPage
        }

        override fun setStart(page: Page, point: Point) {
            val pp = open(page)
            val convertedPoint = toPage(pp.info!!, page.w, page.h, point)
            val b = pp.find(convertedPoint)
            if (b == -1) return
            pp.index = b
            start = pp
        }

        override fun setEnd(page: Page, point: Point) {
            val pp = open(page)
            val convertedPoint = toPage(pp.info!!, page.w, page.h, point)
            val b = pp.find(convertedPoint)
            if (b == -1) return
            pp.index = b
            end = pp
        }

        override fun getText(): String {
            val b = SelectionBounds()
            val sb = StringBuilder()
            for (i in b.s.page..b.e.page) {
                sb.append(getText(i))
            }
            return sb.toString()
        }

        internal fun getText(i: Int): String {
            val b = SelectionBounds(i)
            return b.getText()
        }

        override fun getBoundsAll(page: Page): Array<Rect> {
            val pp = open(page)
            val rr = arrayOfNulls<Rect>(pp.text!!.bounds.size)
            for (i in rr.indices) {
                rr[i] = toDevice(pp.info!!, page.w, page.h, pp.text!!.bounds[i])
            }
            @Suppress("UNCHECKED_CAST")
            return rr as Array<Rect>
        }

        override fun getBounds(p: Page): Bounds {
            val bounds = Bounds()
            val b = SelectionBounds(p)
            bounds.reverse = b.reverse
            bounds.start = b.first
            bounds.end = b.last
            val rr = ArrayList<Rect>()
            var i = b.ss
            while (i != b.ee) {
                rr.add(toDevice(b.page!!.info!!, b.page!!.w, b.page!!.h, b.page!!.text!!.bounds[i]))
                i++
            }
            bounds.rr = rr.toTypedArray()
            return bounds
        }

        override fun inBetween(page: Page, start: Point, end: Point): Boolean? {
            val b = SelectionBounds(page)
            if (b.s.page < page.page && page.page < b.e.page) return true
            val p1 = toPage(b.page!!.info!!, page.w, page.h, start)
            val i1 = b.page!!.find(p1)
            if (i1 == -1) return null
            val p2 = toPage(b.page!!.info!!, page.w, page.h, end)
            val i2 = b.page!!.find(p2)
            if (i2 == -1) return null
            if (i2 < i1) return null
            return i1 <= b.ss && b.ss <= i2 || i1 <= b.ll && b.ll <= i2
        }

        override fun isValid(page: Page, point: Point): Boolean {
            val pp = open(page)
            val convertedPoint = toPage(pp.info!!, page.w, page.h, point)
            return pp.find(convertedPoint) != -1
        }

        override fun isSelected(page: Int): Boolean {
            val b = SelectionBounds(page)
            return b.s.page <= page && page <= b.e.page
        }

        override fun isAbove(page: Page, point: Point): Boolean? {
            val b = SelectionBounds(page)
            if (b.s.page < page.page) return true
            val convertedPoint = toPage(b.page!!.info!!, page.w, page.h, point)
            val index = b.page!!.find(convertedPoint)
            if (index == -1) return null
            return b.ss < index || b.ll < index
        }

        override fun isBelow(page: Page, point: Point): Boolean? {
            val b = SelectionBounds(page)
            if (b.e.page > page.page) return true
            val convertedPoint = toPage(b.page!!.info!!, page.w, page.h, point)
            val index = b.page!!.find(convertedPoint)
            if (index == -1) return null
            return index < b.ss || index < b.ll
        }

        override fun close() {}

        override fun getStart(): ZLTextPosition? = start?.let { ZLTextFixedPosition(it.page, it.index, 0) }
        override fun getEnd(): ZLTextPosition? = end?.let { ZLTextFixedPosition(it.page, it.index, 0) }

        inner class SelectionBounds {
            var page: SelectionPage? = null
            var s: SelectionPage
            var e: SelectionPage
            var ss: Int = 0
            var ee: Int = 0
            var ll: Int = 0
            var first: Boolean = false
            var last: Boolean = false
            var reverse: Boolean = false

            constructor(p: Page) : this(p.page) {
                end?.w = p.w
                end?.h = p.h
                start?.w = p.w
                start?.h = p.h
                page?.w = p.w
                page?.h = p.h
            }

            constructor(p: Int) : this() {
                if (s.page == e.page) {
                    page = s
                    ss = s.index
                    ee = e.index
                    first = true
                    last = true
                    if (reverse) ss++
                } else if (s.page == p) {
                    page = s
                    ss = s.index
                    ee = s.last()
                    first = true
                    if (reverse) ss++
                } else if (e.page == p) {
                    page = e
                    ss = e.first()
                    ee = e.index
                    last = true
                } else {
                    page = SelectionPage(open(p))
                    ss = page!!.first()
                    ee = page!!.last()
                }
                ll = ee
                ee++
            }

            constructor() {
                val sp = end!!
                val ep = start!!
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
            }

            fun getText(): String {
                val bb = StringBuilder()
                for (b in ss until ee) {
                    bb.append(page!!.getText(b))
                }
                return bb.toString()
            }
        }
    }

    class DjvuSearchPage {
        var page: Int = 0
        var text: LibDjvu.Text? = null
        var info: LibDjvu.Page? = null
        var rr: ArrayList<DjvuSearchResult> = ArrayList()
        var map: ArrayList<DjvuSearchMap> = ArrayList()

        fun find(i: Int): Int {
            for (p in map) {
                if (i >= p.start && i < p.end) return p.index
            }
            return -1
        }
    }

    class DjvuSearchResult(val page: Int, val start: Int, val end: Int)

    class DjvuSearchMap(val index: Int, val start: Int, val end: Int)

    inner class DjvuSearch(val doc: DjvuLibre, val str: String) : Plugin.View.Search() {
        var all: ArrayList<DjvuSearchResult> = ArrayList()
        var index: Int = -1
        var initialPage: Int = -1
        val pages = SparseArray<DjvuSearchPage>()

        internal fun hasText(page: Int): Boolean {
            for (type in TYPES) {
                val text = doc.getText(page, type)
                if (text != null && text.bounds.isNotEmpty()) return true
            }
            return false
        }

        internal fun search(page: Int): DjvuSearchPage {
            var pp = pages.get(page)
            if (pp != null) return pp
            pp = DjvuSearchPage()
            pages.put(page, pp)
            pp.page = page
            pp.info = doc.getPageInfo(page)
            for (type in TYPES) {
                pp.text = doc.getText(page, type)
                if (pp.text != null && pp.text!!.bounds.isNotEmpty()) break
            }
            if (pp.text == null) return pp
            val find = str.lowercase(Locale.US)
            val b = StringBuilder()
            for (i in pp.text!!.text.indices) {
                val s = b.length
                b.append(pp.text!!.text[i])
                val e = b.length
                pp.map.add(DjvuSearchMap(i, s, e))
            }
            var txt = b.toString().lowercase(Locale.US)
            var start = txt.indexOf(find)
            while (start != -1) {
                val end = start + find.length
                pp.rr.add(DjvuSearchResult(page, pp.find(start), pp.find(end) + 1))
                start = txt.indexOf(find, start + 1)
            }
            return pp
        }

        override fun getBounds(page: Plugin.View.Selection.Page): Bounds? {
            val bounds = Bounds()
            val p = pages.get(page.page) ?: return null
            val rr = ArrayList<Rect>()
            for (r in p.rr) {
                val hh = ArrayList<Rect>()
                for (k in r.start until r.end) {
                    val b = toDevice(p.info!!, page.w, page.h, p.text!!.bounds[k])
                    rr.add(b)
                    hh.add(b)
                }
                if (index >= 0 && r == all[index]) {
                    bounds.highlight = hh.toTypedArray()
                }
            }
            bounds.rr = rr.toTypedArray()
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
                for (i in all[index - 1].page + 1 until doc.pagesCount) {
                    all.addAll(search(i).rr)
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
                    all.addAll(0, search(i).rr)
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
            for (i in 0 until doc.pagesCount) {
                all.addAll(search(Plugin.View.Selection.odd(page, i, doc.pagesCount)).rr)
            }
        }
    }

    class DjvuPage : Plugin.Page {
        var doc: DjvuLibre

        constructor(r: DjvuPage) : super(r) { doc = r.doc }

        constructor(r: DjvuPage, index: ZLViewEnums.PageIndex, w: Int, h: Int) : this(r) {
            this.w = w
            this.h = h
            load(index)
            if (index == ZLViewEnums.PageIndex.current) {
                load()
                renderPage()
            }
        }

        constructor(d: DjvuLibre, page: Int, w: Int, h: Int) {
            doc = d
            this.w = w
            this.h = h
            pageNumber = page
            pageOffset = 0
            load()
            renderPage()
        }

        constructor(d: DjvuLibre) { doc = d; load() }

        override fun load() {
            val p = doc.getPageInfo(pageNumber)
            pageBox = Plugin.Box(0, 0, p.width, p.height)
            dpi = p.dpi
        }

        override fun getPagesCount(): Int = doc.pagesCount
    }

    open inner class DjvuView(f: ZLFile) : Plugin.View() {
        var doc: DjvuLibre
        var `is`: FileInputStream

        init {
            try {
                `is` = FileInputStream(File(f.path))
                doc = DjvuLibre(`is`.fd)
                current = DjvuPage(doc)
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }
        }

        override fun getPageInfo(w: Int, h: Int, c: ScrollWidget.ScrollAdapter.PageCursor): Plugin.Page? {
            val page: Int = if (c.start == null) c.end.paragraphIndex - 1 else c.start.paragraphIndex
            return DjvuPage(doc, page, w, h)
        }

        override fun render(w: Int, h: Int, page: Int, c: Bitmap.Config): Bitmap? {
            val r = DjvuPage(doc, page, w, h)
            r.scale(w * 2, h * 2)
            val bm = Bitmap.createBitmap(r.pageBox!!.w, r.pageBox!!.h, c)
            doc.renderPage(bm, r.pageNumber, 0, 0, r.pageBox!!.w, r.pageBox!!.h, 0, 0, r.pageBox!!.w, r.pageBox!!.h)
            bm.density = r.dpi
            return bm
        }

        override fun draw(canvas: Canvas, w: Int, h: Int, index: ZLViewEnums.PageIndex, c: Bitmap.Config) {
            val curr = current as DjvuPage
            val r = DjvuPage(curr, index, w, h)
            if (index == ZLViewEnums.PageIndex.current) current!!.updatePage(r)
            r.scale(w, h)
            val render = r.renderRect()
            val bm = Bitmap.createBitmap(r.pageBox!!.w, r.pageBox!!.h, c)
            bm.eraseColor(FBReaderView.PAGE_PAPER_COLOR)
            doc.renderPage(bm, r.pageNumber, 0, 0, r.pageBox!!.w, r.pageBox!!.h, render.x, render.y, render.w, render.h)
            canvas.drawBitmap(bm, render.src!!, render.dst!!, paint)
            bm.recycle()
        }

        override fun select(page: Plugin.View.Selection.Page, point: Plugin.View.Selection.Point): Plugin.View.Selection? {
            val s = Selection(doc, page, point)
            if (s.isEmpty()) return null
            return s
        }

        override fun select(start: ZLTextPosition, end: ZLTextPosition): Plugin.View.Selection? {
            val s = Selection(doc, start, end)
            if (s.isEmpty()) return null
            return s
        }

        override fun select(page: Int): Plugin.View.Selection? {
            val s = Selection(doc, page)
            if (s.isEmpty()) return null
            return s
        }

        override fun search(text: String): Search? {
            val s = DjvuSearch(doc, text)
            for (i in 0 until doc.pagesCount) {
                if (s.hasText(i)) return s
            }
            s.close()
            return null
        }
    }

    inner class DjvuTextModel(f: ZLFile) : DjvuView(f), ZLTextModel {
        protected fun finalize() {
            doc.close()
            `is`.close()
        }

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
