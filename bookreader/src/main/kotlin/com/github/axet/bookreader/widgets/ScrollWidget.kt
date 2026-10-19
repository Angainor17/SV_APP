package com.github.axet.bookreader.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.PowerManager
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.axet.androidlibrary.widgets.ThemeUtils
import com.github.axet.androidlibrary.widgets.TopAlwaysSmoothScroller
import com.github.axet.bookreader.app.Plugin
import com.github.axet.bookreader.app.PluginPage
import com.github.axet.bookreader.app.PluginView
import com.github.axet.bookreader.app.Reflow
import com.github.axet.bookreader.app.Storage
import org.geometerplus.fbreader.fbreader.FBView
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions
import org.geometerplus.zlibrary.core.view.ZLView
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.core.view.ZLViewWidget
import org.geometerplus.zlibrary.text.view.ZLTextElementAreaVector
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import org.geometerplus.zlibrary.text.view.ZLTextRegion
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidPaintContext
import java.util.ArrayList
import java.util.HashSet
import java.util.Locale
import java.util.TreeMap

class ScrollWidget(view: FBReaderView) : RecyclerView(view.context), ZLViewWidget {

    private val fb: FBReaderView = view

    lateinit var lm: LinearLayoutManager

    @JvmField
    val adapter: ScrollAdapter

    @JvmField
    val gesturesListener: Gestures

    init {
        adapter = ScrollAdapter()
        gesturesListener = Gestures()

        lm = object : LinearLayoutManager(fb.context) {
            var idley: Int = 0
            val idle = object : Runnable {
                override fun run() {
                    if (idley >= 0) {
                        val page = findLastPage()
                        val next = page + 1
                        if (next < adapter.pages.size) {
                            val h = findViewHolderForAdapterPosition(next)
                            h?.itemView?.draw(Canvas())
                        }
                    } else {
                        val page = findFirstPage()
                        val prev = page - 1
                        if (prev >= 0) {
                            val h = findViewHolderForAdapterPosition(prev)
                            h?.itemView?.draw(Canvas())
                        }
                    }
                }
            }

            override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
                val off = super.scrollVerticallyBy(dy, recycler, state)
                if (fb.pluginview != null)
                    updateOverlays()
                idley = dy
                fb.removeCallbacks(idle)
                fb.tts?.scrollVerticallyBy(dy)
                return off
            }

            override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
                val pm = fb.context.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (pm.isPowerSaveMode) {
                    scrollToPositionWithOffset(position, 0)
                    idley = position - findFirstPage()
                    onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE)
                } else {
                    val smoothScroller = TopAlwaysSmoothScroller(recyclerView.context)
                    smoothScroller.targetPosition = position
                    startSmoothScroll(smoothScroller)
                }
            }

            override fun onScrollStateChanged(state: Int) {
                super.onScrollStateChanged(state)
                fb.removeCallbacks(idle)
                fb.postDelayed(idle, 1000)
            }

            override fun onLayoutCompleted(state: RecyclerView.State) {
                super.onLayoutCompleted(state)
                if (fb.pluginview != null)
                    updateOverlays()
            }

            override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
                return getMainAreaHeight()
            }

            override fun onDetachedFromWindow(view: RecyclerView, recycler: RecyclerView.Recycler) {
                super.onDetachedFromWindow(view, recycler)
                fb.removeCallbacks(idle)
            }
        }

        layoutManager = lm
        (this as RecyclerView).adapter = adapter

        val dividerItemDecoration = DividerItemDecoration(fb.context, DividerItemDecoration.VERTICAL)
        addItemDecoration(dividerItemDecoration)

        setPadding(0, 0, 0, height - getMainAreaHeight()) // footer height

        itemAnimator = null

        fb.config.setValue(fb.app.PageTurningOptions.fingerScrolling, PageTurningOptions.FingerScrollingType.byFlick)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (gesturesListener.onTouchEvent(e))
            return true
        return super.onTouchEvent(e)
    }

    fun findView(e: MotionEvent): ScrollAdapter.PageView? {
        return findView(e.x, e.y)
    }

    fun findView(x: Float, y: Float): ScrollAdapter.PageView? {
        for (i in 0 until lm.childCount) {
            val view = lm.getChildAt(i) as ScrollAdapter.PageView
            if (view.left < view.right && view.top < view.bottom && x >= view.left && x < view.right && y >= view.top && y < view.bottom)
                return view
        }
        return null
    }

    fun findRegionView(soul: ZLTextRegion.Soul): ScrollAdapter.PageView? {
        for (h in adapter.holders) {
            val view = h.page
            if (view.text?.getRegion(soul) != null)
                return view
        }
        return null
    }

    fun findViewPage(c: ScrollAdapter.PageCursor): ScrollAdapter.PageView? {
        for (h in adapter.holders) {
            val pos = h.adapterPosition
            val p = adapter.pages[pos]
            if (p.equals(c))
                return h.page
        }
        return null
    }

    fun findUnion(bm: Storage.Bookmark): Rect? {
        var union: Rect? = null
        for (i in 0 until lm.childCount) {
            val view = lm.getChildAt(i) as ScrollAdapter.PageView
            val text = view.text
            if (text != null) {
                val r = FBReaderView.findUnion(text.areas(), bm)
                if (r != null) {
                    r.offset(view.left, view.top)
                    if (union == null)
                        union = r
                    else
                        union!!.union(r)
                }
            }
        }
        return union
    }

    override fun reset() {
        postInvalidate()
    }

    override fun repaint() {
    }

    fun getViewPercent(view: View): Int {
        var h = 0
        val b = getMainAreaHeight()
        if (view.bottom > 0)
            h = view.bottom // visible height
        if (b < view.bottom)
            h -= view.bottom - b
        if (view.top > 0)
            h -= view.top
        return h * 100 / view.height
    }

    fun findFirstPage(): Int {
        val hp15 = TreeMap<Int, View>()
        val hp100 = TreeMap<Int, View>()
        val hp0 = TreeMap<Int, View>()
        for (i in 0 until lm.childCount) {
            val view = lm.getChildAt(i)!!
            val hp = getViewPercent(view)
            if (hp > 15) // add only views atleast 15% visible
                hp15[view.top] = view
            if (hp == 100)
                hp100[view.top] = view
            if (hp > 0)
                hp0[view.top] = view
        }
        var v: View? = null
        for (key in hp100.keys) {
            v = hp15[key]
            break
        }
        if (v == null) {
            for (key in hp15.keys) {
                v = hp15[key]
                break
            }
        }
        if (v == null) {
            for (key in hp15.keys) {
                v = hp0[key]
                break
            }
        }
        if (v != null)
            return (v as ScrollAdapter.PageView).holder!!.adapterPosition
        return -1
    }

    fun findLastPage(): Int {
        val hp0 = TreeMap<Int, View>()
        for (i in 0 until lm.childCount) {
            val v = lm.getChildAt(i)!!
            val hp = getViewPercent(v)
            if (hp > 0)
                hp0[v.top] = v
        }
        if (hp0.isEmpty())
            return -1
        val v = hp0.lastEntry().value as ScrollAdapter.PageView
        return v.holder!!.adapterPosition
    }

    override fun startManualScrolling(x: Int, y: Int, direction: ZLViewEnums.Direction) {
    }

    override fun scrollManuallyTo(x: Int, y: Int) {
    }

    override fun startAnimatedScrolling(pageIndex: ZLViewEnums.PageIndex, x: Int, y: Int, direction: ZLViewEnums.Direction, speed: Int) {
        startAnimatedScrolling(pageIndex, direction, speed)
    }

    override fun startAnimatedScrolling(pageIndex: ZLViewEnums.PageIndex, direction: ZLViewEnums.Direction, speed: Int) {
        var pos = findFirstPage()
        if (pos == -1)
            return
        when (pageIndex) {
            ZLViewEnums.PageIndex.next -> pos++
            ZLViewEnums.PageIndex.previous -> pos--
            else -> {}
        }
        if (pos < 0 || pos >= adapter.pages.size)
            return
        smoothScrollToPosition(pos)
    }

    override fun startAnimatedScrolling(x: Int, y: Int, speed: Int) {
    }

    override fun getScreenBrightness(): Int {
        return gesturesListener.brightness.getScreenBrightness()
    }

    override fun setScreenBrightness(percent: Int) {
        gesturesListener.brightness.setScreenBrightness(percent)
        postInvalidate()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
    }

    override fun draw(c: Canvas) {
        if (adapter.size.w != width || adapter.size.h != height) { // reset for textbook and reflow mode only
            adapter.reset()
        }
        super.draw(c)
        updatePosition()
        drawFooter(c)
        fb.invalidateFooter()
    }

    fun updatePosition() { // position can vary depend on which page drawn, restore it after every draw
        val first = findFirstPage()
        if (first == -1)
            return

        val c = adapter.pages[first]

        var pos: ZLTextPosition? = c.start
        if (pos == null)
            pos = c.end

        if (fb.pluginview != null && fb.pluginview!!.reflow) {
            if (c.start == null) {
                var p = c.end!!.paragraphIndex
                val i = c.end!!.elementIndex - 1
                if (i < 0)
                    p = p - 1
                fb.pluginview!!.current!!.pageNumber = p
            } else {
                fb.pluginview!!.current!!.pageNumber = c.start!!.paragraphIndex
            }
            fb.clearReflowPage() // reset reflow page, since we treat pageOffset differently for reflower/full page view
        } else {
            adapter.open(c)
            if (fb.scrollDelayed != null) {
                if (fb.pluginview != null) {
                    val info = fb.pluginview!!.getPageInfo(width, height, c)
                    for (p in adapter.pages) {
                        if (p.start != null && p.start!!.paragraphIndex == fb.scrollDelayed!!.paragraphIndex) {
                            var offset: Int
                            if (fb.scrollDelayed is FBReaderView.ZLTextIndexPosition) {
                                val s = fb.pluginview!!.select(fb.scrollDelayed!!, (fb.scrollDelayed as FBReaderView.ZLTextIndexPosition).end)
                                val page = fb.pluginview!!.selectPage(fb.scrollDelayed!!, null, info!!.w, info!!.h)
                                val bb = s!!.getBounds(page)
                                s.close()
                                val union = SelectionView.union(listOf(*bb!!.rr!!))
                                offset = union.top
                                // Центрирование позиции на экране
                                if (fb.scrollCentered) {
                                    offset = offset - getMainAreaHeight() / 2 + union.height() / 2
                                }
                            } else if (fb.scrollDelayed!!.elementIndex != 0) {
                                offset = (fb.scrollDelayed!!.elementIndex / info!!.ratio).toInt()
                                // Центрирование позиции на экране
                                if (fb.scrollCentered) {
                                    offset = offset - getMainAreaHeight() / 2
                                }
                            } else {
                                // elementIndex = 0 - просто центрируем страницу
                                offset = if (fb.scrollCentered) -getMainAreaHeight() / 2 else 0
                            }
                            scrollBy(0, offset)
                            adapter.oldTurn = pos
                            fb.scrollDelayed = null
                            fb.scrollCentered = false
                            break
                        }
                    }
                } else {
                    fb.gotoPosition(fb.scrollDelayed!!)
                    adapter.oldTurn = pos
                    fb.scrollDelayed = null
                    fb.scrollCentered = false
                }
            }
        }
        if (!pos!!.equals(adapter.oldTurn) && scrollState == RecyclerView.SCROLL_STATE_IDLE) {
            fb.onScrollingFinished(ZLViewEnums.PageIndex.current)
            adapter.oldTurn = pos
        }
    }

    fun drawFooter(c: Canvas) {
        if (fb.app.Model != null) {
            val footer = fb.app.BookTextView.footerArea
            if (footer == null)
                return
            val context = ZLAndroidPaintContext(
                fb.app.SystemInfo!!,
                c,
                ZLAndroidPaintContext.Geometry(
                    width,
                    height,
                    width,
                    footer.height,
                    0,
                    getMainAreaHeight()
                ),
                0
            )
            val voffset = height - footer.height
            c.save()
            c.translate(0f, voffset.toFloat())
            footer.paint(context)
            c.restore()
        }
    }

    fun getMainAreaHeight(): Int {
        val footer = fb.app.BookTextView.footerArea
        return if (footer != null) height - footer.height else height
    }

    fun onReflowerDone() {
        if (fb.search != null) {
            if (fb.searchPagePending != -1) {
                val p = fb.searchPagePending
                fb.post { searchPage(p) }
                fb.searchPagePending = -1
            }
        }
        if (fb.selection != null) {
            fb.post { updateOverlays() }
        }
        if (fb.scrollDelayed != null) {
            adapter.loadPages(fb.pluginview!!.reflower!!)
            for (i in adapter.pages.indices) {
                val c = adapter.pages[i]
                val pinfo = fb.pluginview!!.getPageInfo(width, height, c)
                if (c.start != null && c.start!!.paragraphIndex == fb.scrollDelayed!!.paragraphIndex) {
                    val info = Reflow.Info(fb.pluginview!!.reflower!!, c.start!!.elementIndex)
                    val ratio = info.bm.width() / width.toDouble()
                    val ss = ArrayList<Rect>(info.src.keys)
                    ss.sortWith(SelectionView.UL())
                    var offset: Int
                    if (fb.scrollDelayed is FBReaderView.ZLTextIndexPosition) {
                        val s = fb.pluginview!!.select(fb.scrollDelayed!!, (fb.scrollDelayed as FBReaderView.ZLTextIndexPosition).end)
                        val page = fb.pluginview!!.selectPage(fb.scrollDelayed!!, info, pinfo!!.w, pinfo!!.h)
                        val bb = s!!.getBounds(page)
                        s.close()
                        val union = SelectionView.union(listOf(*bb!!.rr!!))
                        offset = union.top
                    } else {
                        offset = (fb.scrollDelayed!!.elementIndex / pinfo!!.ratio * ratio).toInt()
                    }
                    for (s in ss) {
                        if (s.top > offset || s.bottom >= offset) {
                            scrollToPosition(i)
                            val screen = ((s.top - offset) / ratio).toInt()
                            val off = info.src[s]!!.top - screen
                            if (off > 0)
                                scrollBy(0, off)
                            fb.post { updateOverlays() }
                            adapter.oldTurn = ZLTextFixedPosition(c.start!!)
                            fb.scrollDelayed = null
                            return
                        }
                    }
                }
            }
        }
        lm.onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE)
    }

    fun overlayRemove(view: ScrollAdapter.PageView) {
        selectionRemove(view)
        linksRemove(view)
        searchRemove(view)
        ttsRemove(view)
    }

    fun overlaysClose() {
        for (h in adapter.holders)
            overlayRemove(h.page)
    }

    fun updateOverlays() {
        for (h in adapter.holders)
            overlayUpdate(h.page)
    }

    fun overlayUpdate(view: ScrollAdapter.PageView) {
        if (fb.selection != null)
            selectionUpdate(view)
        linksUpdate(view)
        bookmarksUpdate(view)
        if (fb.search != null)  // Call searchUpdate if search is active (not just if view.search exists)
            searchUpdate(view)
        if (view.tts != null)
            ttsUpdate(view)
    }

    fun linksClose() {
        for (h in adapter.holders)
            linksRemove(h.page)
    }

    fun linksRemove(view: ScrollAdapter.PageView) {
        if (view.links == null)
            return
        view.links!!.close()
        view.links = null
    }

    fun linksUpdate(view: ScrollAdapter.PageView) {
        val pos = view.holder!!.adapterPosition
        if (pos == -1) {
            linksRemove(view)
        } else {
            val c = adapter.pages[pos]

            val page: PluginView.Selection.Page?

            if (c.start == null || c.end == null)
                page = null
            else
                page = fb.pluginview!!.selectPage(c.start!!, view.info, view.width, view.height)

            if (page != null && (!fb.pluginview!!.reflow || view.info != null) && view.parent != null) { // cached views has no parrent
                if (view.links == null)
                    view.links = FBReaderView.LinksView(fb, fb.pluginview!!.getLinks(page), view.info)
                var x = view.left
                val y = view.top
                if (view.info != null)
                    x += view.info!!.margin.left
                view.links!!.update(x, y)
            } else {
                linksRemove(view)
            }
        }
    }

    fun bookmarksClose() {
        for (h in adapter.holders)
            bookmarksRemove(h.page)
    }

    fun bookmarksRemove(view: ScrollAdapter.PageView) {
        if (view.bookmarks == null)
            return
        view.bookmarks!!.close()
        view.bookmarks = null
    }

    fun bookmarksUpdate(view: ScrollAdapter.PageView) {
        val pos = view.holder!!.adapterPosition
        if (pos == -1) {
            bookmarksRemove(view)
        } else {
            val c = adapter.pages[pos]

            val page: PluginView.Selection.Page?

            if (c.start == null || c.end == null)
                page = null
            else
                page = fb.pluginview!!.selectPage(c.start!!, view.info, view.width, view.height)

            if (page != null && (!fb.pluginview!!.reflow || view.info != null) && view.parent != null) { // cached views has no parrent
                if (view.bookmarks == null)
                    view.bookmarks = FBReaderView.BookmarksView(fb, page, fb.book!!.info?.bookmarks, view.info)
                var x = view.left
                val y = view.top
                if (view.info != null)
                    x += view.info!!.margin.left
                view.bookmarks!!.update(x, y)
            } else {
                bookmarksRemove(view)
            }
        }
    }

    fun bookmarksUpdate() {
        for (h in adapter.holders) {
            bookmarksRemove(h.page)
            bookmarksUpdate(h.page)
        }
    }

    fun ttsClose() {
        for (h in adapter.holders)
            ttsRemove(h.page)
    }

    fun ttsRemove(view: ScrollAdapter.PageView) {
        if (view.tts == null)
            return
        view.tts!!.close()
        view.tts = null
    }

    fun ttsUpdate(view: ScrollAdapter.PageView) {
        val pos = view.holder!!.adapterPosition
        if (pos == -1) {
            ttsRemove(view)
        } else {
            val c = adapter.pages[pos]

            val page: PluginView.Selection.Page?

            if (c.start == null || c.end == null)
                page = null
            else
                page = fb.pluginview!!.selectPage(c.start!!, view.info, view.width, view.height)

            if (page != null && (!fb.pluginview!!.reflow || view.info != null) && view.parent != null) { // cached views has no parrent
                if (view.tts == null)
                    view.tts = FBReaderView.TTSView(fb, page, view.info)
                var x = view.left
                val y = view.top
                if (view.info != null)
                    x += view.info!!.margin.left
                view.tts!!.update(x, y)
            } else {
                ttsRemove(view)
            }
        }
    }

    fun ttsUpdate() {
        for (h in adapter.holders) {
            ttsRemove(h.page)
            ttsUpdate(h.page)
        }
    }

    fun searchPage(page: Int) {
        if (fb.pluginview!!.reflow) {
            if (fb.pluginview!!.reflower != null && fb.pluginview!!.reflower!!.page == page) {
                for (i in 0 until fb.pluginview!!.reflower!!.count()) {
                    val info = Reflow.Info(fb.pluginview!!.reflower!!, i)
                    val pos = ZLTextFixedPosition(page, i, 0)
                    val p = fb.pluginview!!.selectPage(pos, info, fb.pluginview!!.reflower!!.w, fb.pluginview!!.reflower!!.h)
                    val bb = fb.search!!.getBounds(p)
                    if (bb != null && bb.rr != null) {
                        bb.rr = fb.pluginview!!.boundsUpdate(bb.rr!!, info)
                        if (bb.highlight != null) {
                            val hh = HashSet(listOf(*fb.pluginview!!.boundsUpdate(bb.highlight!!, info)))
                            for (r in bb.rr!!) {
                                if (hh.contains(r)) {
                                    adapter.loadPages(fb.pluginview!!.reflower!!)
                                    val pp = adapter.findPos(pos)
                                    if (pp != -1) {
                                        smoothScrollToPosition(pp)
                                        searchClose() // remove all SearchView
                                        updateOverlays()
                                        return
                                    }
                                }
                            }
                        }
                    }
                }
                searchClose() // remove all SearchView
                updateOverlays()
                return // reflow missing for symbol (treated as image)
            }
            fb.searchPagePending = page
            fb.pluginview!!.gotoPosition(ZLTextFixedPosition(page, 0, 0))
            fb.resetNewPosition()
        } else {
            for (holder in adapter.holders) {
                val pos = holder.adapterPosition
                if (pos != -1) {
                    val c = adapter.pages[pos]
                    if (c.start != null && c.start!!.paragraphIndex == page) {
                        val p = fb.pluginview!!.selectPage(c.start!!, holder.page.info, holder.page.width, holder.page.height)
                        val bb = fb.search!!.getBounds(p)
                        if (bb != null && bb.rr != null) {
                            if (bb.highlight != null) {
                                val hh = HashSet(listOf(*bb.highlight!!))
                                for (r in bb.rr!!) {
                                    if (hh.contains(r)) {
                                        val h = getMainAreaHeight()
                                        val bottom = top + h
                                        var y = r.top + holder.page.top
                                        if (y > bottom) {
                                            val dy = y - bottom
                                            val pages = dy / height + 1
                                            smoothScrollBy(0, pages * h)
                                        } else {
                                            y = r.bottom + holder.page.top
                                            if (y > bottom) {
                                                val dy = y - bottom
                                                smoothScrollBy(0, dy)
                                            }
                                        }
                                        y = r.bottom + holder.page.top
                                        if (y < top) {
                                            val dy = y - top
                                            val pages = dy / height - 1
                                            smoothScrollBy(0, pages * h)
                                        } else {
                                            y = r.top + holder.page.top
                                            if (y < top) {
                                                val dy = y - top
                                                smoothScrollBy(0, dy)
                                            }
                                        }
                                        searchClose()
                                        updateOverlays()
                                        return
                                    }
                                }
                            }
                            return
                        }
                    }
                }
            }
            val pp = ZLTextFixedPosition(page, 0, 0)
            fb.gotoPluginPosition(pp)
            fb.resetNewPosition()
        }
    }

    fun searchClose() {
        for (h in adapter.holders) {
            searchRemove(h.page)
        }
    }

    fun searchRemove(view: ScrollAdapter.PageView) {
        if (view.search == null)
            return
        view.search!!.close()
        view.search = null
    }

    fun searchUpdate(view: ScrollAdapter.PageView) {
        val pos = view.holder!!.adapterPosition
        if (pos == -1) {
            searchRemove(view)
        } else {
            val c = adapter.pages[pos]

            val page: PluginView.Selection.Page?

            if (c.start == null || c.end == null) {
                page = null
            } else {
                page = fb.pluginview!!.selectPage(c.start!!, view.info, view.width, view.height)
            }

            if (page != null && (!fb.pluginview!!.reflow || view.info != null) && view.parent != null) { // cached views has no parrent
                if (view.search == null)
                    view.search = FBReaderView.SearchView(fb, fb.search!!.getBounds(page), view.info)
                var x = view.left
                val y = view.top
                if (view.info != null)
                    x += view.info!!.margin.left
                view.search!!.update(x, y)
            } else {
                searchRemove(view)
            }
        }
    }

    fun selectionClose() {
        for (h in adapter.holders)
            selectionRemove(h.page)
    }

    fun selectionRemove(view: ScrollAdapter.PageView) {
        if (view.selection != null) {
            fb.selection!!.remove(view.selection!!)
            view.selection = null
        }
    }

    fun selectionUpdate(view: ScrollAdapter.PageView) {
        val pos = view.holder!!.adapterPosition
        if (pos == -1) {
            selectionRemove(view)
        } else {
            val c = adapter.pages[pos]

            var selected = true
            val page: PluginView.Selection.Page?

            if (c.start == null || c.end == null) {
                selected = false
                page = null
            } else {
                page = fb.pluginview!!.selectPage(c.start!!, view.info, view.width, view.height)
            }

            if (selected)
                selected = fb.selection!!.selection.isSelected(page!!.page)

            val first: Rect?
            val last: Rect?

            if (fb.pluginview!!.reflow && selected && view.info != null) {
                val bounds = fb.selection!!.selection.getBoundsAll(page!!)
                val ii = ArrayList<Rect>()
                for (b in bounds!!) {
                    for (s in view.info!!.src.keys) {
                        val i = Rect(b)
                        if (i.intersect(s) && (i.height() * 100 / s.height() > SelectionView.ARTIFACT_PERCENTS || b.height() > 0 && i.height() * 100 / b.height() > SelectionView.ARTIFACT_PERCENTS))
                            ii.add(i)
                    }
                }
                ii.sortWith(SelectionView.LinesUL(ii))

                var a = false
                var f: Rect? = null
                var i = 0
                while (!a && i < ii.size) {
                    f = ii[i]
                    val rect = f!!
                    do {
                        a = fb.selection!!.selection.isValid(page!!, PluginView.Selection.Point(rect.left, rect.centerY()))
                    } while (!a && ++rect.left < rect.right)
                    i++
                }
                first = f

                var b = false
                var l: Rect? = null
                i = ii.size - 1
                while (!b && i >= 0) {
                    l = ii[i]
                    val rect = l!!
                    do {
                        b = fb.selection!!.selection.isValid(page!!, PluginView.Selection.Point(rect.right, rect.centerY()))
                    } while (!b && --rect.right > rect.left)
                    i--
                }
                last = l

                val r = fb.selection!!.selection.inBetween(page!!, PluginView.Selection.Point(f!!.left, f!!.centerY()), PluginView.Selection.Point(l!!.right, l!!.centerY()))

                selected = r != null && r
            } else {
                if (fb.pluginview!!.reflow)
                    selected = false
                first = null
                last = null
            }

            if (selected) {
                if (view.selection == null) {
                    val setter = object : PluginView.Selection.Setter {
                        override fun setStart(x: Int, y: Int) {
                            var pos = NO_POSITION
                            val v = findView(x.toFloat(), y.toFloat())
                            if (v != null) {
                                pos = v.holder!!.adapterPosition
                                if (pos != -1) {
                                    val cc = adapter.pages[pos]
                                    val xx = x - v.left
                                    val yy = y - v.top
                                    val page = fb.pluginview!!.selectPage(cc.start!!, v.info, v.width, v.height)
                                    val point = fb.pluginview!!.selectPoint(v.info, xx, yy)
                                    if (point != null)
                                        fb.selection!!.selection.setStart(page, point)
                                }
                            }
                            selectionUpdate(view)
                            if (pos != -1 && pos != view.holder!!.adapterPosition)
                                selectionUpdate(v!!)
                        }

                        override fun setEnd(x: Int, y: Int) {
                            var pos = NO_POSITION
                            val v = findView(x.toFloat(), y.toFloat())
                            if (v != null) {
                                pos = v.holder!!.adapterPosition
                                if (pos != -1) {
                                    val cc = adapter.pages[pos]
                                    val xx = x - v.left
                                    val yy = y - v.top
                                    val page = fb.pluginview!!.selectPage(cc.start!!, v.info, v.width, v.height)
                                    val point = fb.pluginview!!.selectPoint(v.info, xx, yy)
                                    if (point != null)
                                        fb.selection!!.selection.setEnd(page, point)
                                }
                            }
                            selectionUpdate(view)
                            if (pos != -1 && pos != view.holder!!.adapterPosition)
                                selectionUpdate(v!!)
                        }

                        override fun getBounds(): PluginView.Selection.Bounds? {
                            val bounds = fb.selection!!.selection.getBounds(page!!)
                            if (fb.pluginview!!.reflow) {
                                bounds!!.rr = fb.pluginview!!.boundsUpdate(bounds!!.rr!!, view.info!!)

                                val a = fb.selection!!.selection.isAbove(page!!, PluginView.Selection.Point(first!!.left, first!!.centerY()))
                                val b = fb.selection!!.selection.isBelow(page!!, PluginView.Selection.Point(last!!.right, last!!.centerY()))

                                bounds!!.start = a != null && !a
                                bounds!!.end = b != null && !b
                            }
                            return bounds
                        }
                    }
                    view.selection = SelectionView.PageView(context, fb.app.BookTextView as FBReaderView.CustomView, setter)
                    fb.selection!!.add(view.selection!!)
                }
                var x = view.left
                val y = view.top
                if (view.info != null)
                    x += view.info!!.margin.left
                fb.selection!!.update(view.selection!!, x, y)
            } else {
                selectionRemove(view)
            }
        }
    }

    inner class ScrollAdapter : RecyclerView.Adapter<ScrollAdapter.PageHolder>() {
        val lock = Any()
        @JvmField
        val pages = ArrayList<PageCursor>() // adapter items
        var thread: Thread? = null
        val size = Plugin.Box() // ScrollView size, after reset
        val invalidates = HashSet<PageHolder>() // pending invalidates
        @JvmField
        val holders = ArrayList<PageHolder>() // keep all active holders, including Recycler.mCachedViews
        var oldTurn: ZLTextPosition? = null // last page shown

        fun open(c: PageCursor) {
            if (c.start == null) {
                if (fb.pluginview != null) {
                    fb.pluginview!!.gotoPosition(c.end)
                    fb.pluginview!!.onScrollingFinished(ZLViewEnums.PageIndex.previous)
                    fb.pluginview!!.current!!.pageOffset = 0 // widget instanceof ScrollView
                    c.update(getCurrent())
                } else {
                    fb.app.BookTextView.gotoPosition(c.end)
                    fb.app.BookTextView.onScrollingFinished(ZLViewEnums.PageIndex.previous)
                    c.update(getCurrent())
                }
            } else {
                if (fb.pluginview != null)
                    fb.pluginview!!.gotoPosition(c.start)
                else {
                    val cc = getCurrent()
                    if (!cc.equals(c)) {
                        fb.app.BookTextView.gotoPosition(c.start!!, c.end)
                    }
                }
            }
        }

        fun findPage(c: PageCursor): Int {
            if (c.start != null && c.end != null) {
                for (i in pages.indices) {
                    val k = pages[i]
                    if (c.equals(k))
                        return i
                }
            } else if (c.start == null && c.end != null) {
                return findPage(c.end!!)
            } else if (c.start != null) {
                return findPage(c.start!!)
            }
            return -1
        }

        fun findPage(p: ZLTextPosition): Int {
            for (i in pages.indices) {
                val c = pages[i]
                if (c.start != null && c.end != null) {
                    if (c.start!!.compareTo(p) <= 0 && c.end!!.compareTo(p) > 0)
                        return i
                } else if (c.start == null && c.end != null) {
                    if (c.end!!.compareTo(p) > 0)
                        return i
                } else if (c.start != null) {
                    if (c.start!!.compareTo(p) <= 0)
                        return i
                }
            }
            return -1
        }

        fun findPos(p: ZLTextPosition): Int {
            for (i in pages.indices) {
                val c = pages[i]
                if (c.start != null && c.start!!.samePositionAs(p))
                    return i
            }
            return -1
        }

        fun loadPages(reflow: Reflow) {
            if (pages.size == 0) {
                val last = reflow.count() - 1
                for (i in 0..last) {
                    val pos = ZLTextFixedPosition(reflow.page, i, 0)
                    val end: ZLTextPosition?
                    if (i == last)
                        end = null
                    else
                        end = ZLTextFixedPosition(reflow.page, i + 1, 0)
                    pages.add(PageCursor(pos, end))
                    notifyItemInserted(i)
                }
            }
            val prev = ZLTextFixedPosition(reflow.page - 1, 0, 0)
            val start = ZLTextFixedPosition(reflow.page, 0, 0)
            val next = ZLTextFixedPosition(reflow.page + 1, 0, 0)
            var i = 0
            while (i < pages.size) {
                var c = pages[i]
                val startTest = c.start != null && c.start!!.samePositionAs(start)
                val prevTest = c.start != null && c.end != null && c.start!!.paragraphIndex == prev.paragraphIndex && i == (pages.size - 1)
                if (startTest || prevTest) { // update/add next reflow.count pages
                    val last = reflow.count() - 1
                    var k = 0
                    while (k <= last) {
                        if (i >= pages.size) {
                            c = PageCursor(null, null)
                            pages.add(c)
                            notifyItemInserted(i)
                        } else {
                            c = pages[i]
                        }
                        val pos = ZLTextFixedPosition(reflow.page, k, 0)
                        val pos2: ZLTextPosition?
                        if (k == last)
                            pos2 = null
                        else
                            pos2 = ZLTextFixedPosition(reflow.page, k + 1, 0)
                        c.update(PageCursor(pos, pos2))
                        k++
                        i++
                    }
                    return
                }
                if (c.start != null && c.start!!.samePositionAs(next)) { // update/add prev reflow.count pages
                    i--
                    val last = reflow.count() - 1
                    var k = last
                    while (k >= 0) {
                        if (i < 0) {
                            c = PageCursor(null, null)
                            pages.add(0, c)
                            notifyItemInserted(i)
                        } else {
                            c = pages[i]
                        }
                        val pos = ZLTextFixedPosition(reflow.page, k, 0)
                        val pos2: ZLTextPosition?
                        if (k == last)
                            pos2 = ZLTextFixedPosition(start)
                        else
                            pos2 = ZLTextFixedPosition(reflow.page, k + 1, 0)
                        c.update(PageCursor(pos, pos2))
                        k--
                        i--
                    }
                    return
                }
                i++
            }
            throw RuntimeException("unable to load reflower")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            return PageHolder(PageView(parent))
        }

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            holder.page.holder = holder
            holders.add(holder)
        }

        override fun onViewRecycled(holder: PageHolder) {
            super.onViewRecycled(holder)
            holder.page.recycle()
            holder.page.holder = null
            holders.remove(holder)
        }

        override fun getItemCount(): Int {
            return pages.size
        }

        fun reset() { // read current position
            size.w = width
            size.h = height
            if (fb.pluginview != null) {
                if (fb.pluginview!!.reflower != null)
                    fb.pluginview!!.reflower!!.reset()
            }
            recycledViewPool.clear()
            pages.clear()
            if (fb.app.Model != null) {
                fb.app.BookTextView.preparePage((fb.app.BookTextView as FBReaderView.CustomView).createContext(Canvas()), ZLViewEnums.PageIndex.current)
                val c = getCurrent()
                pages.add(c)
                oldTurn = c.start
            }
            postInvalidate()
            notifyDataSetChanged()
        }

        fun getCurrent(): PageCursor {
            if (fb.pluginview != null) {
                if (fb.pluginview!!.reflow) {
                    if (fb.pluginview!!.reflower != null) {
                        val s = ZLTextFixedPosition(fb.pluginview!!.reflower!!.page, fb.pluginview!!.reflower!!.index, 0)
                        val e: ZLTextFixedPosition?
                        val index = s.elementIndex + 1
                        if (fb.pluginview!!.reflower!!.count() == -1)
                            e = null
                        else if (index >= fb.pluginview!!.reflower!!.count()) // current points to next page +1
                            e = ZLTextFixedPosition(fb.pluginview!!.reflower!!.page + 1, 0, 0)
                        else
                            e = ZLTextFixedPosition(s.paragraphIndex, index, 0)
                        return PageCursor(s, e)
                    } else {
                        return PageCursor(ZLTextFixedPosition(fb.pluginview!!.current!!.pageNumber, 0, 0), null)
                    }
                } else {
                    return PageCursor(fb.pluginview!!.getPosition(), fb.pluginview!!.getNextPosition())
                }
            } else {
                return PageCursor(fb.app.BookTextView.startCursor, fb.app.BookTextView.endCursor)
            }
        }

        fun update() {
            if (fb.app.Model == null)
                return
            val c = getCurrent()
            var page = 0
            while (page < pages.size) {
                val p = pages[page]
                if (p.equals(c)) {
                    p.update(c)
                    break
                }
                page++
            }
            if (page == pages.size) { // not found == 0
                pages.add(c)
                notifyItemInserted(page)
            }
            if (fb.app.BookTextView.canScroll(ZLViewEnums.PageIndex.previous)) {
                if (page == 0) {
                    pages.add(page, PageCursor(null, c.start))
                    notifyItemInserted(page)
                    page++ // 'c' page moved to + 1
                }
            }
            if (fb.app.BookTextView.canScroll(ZLViewEnums.PageIndex.next)) {
                if (page == pages.size - 1) {
                    page++
                    pages.add(page, PageCursor(c.end, null))
                    notifyItemInserted(page)
                }
            }
        }

        fun processInvalidate() {
            for (h in invalidates) {
                h.page.recycle()
                h.page.invalidate()
            }
        }

        fun processClear() {
            invalidates.clear()
        }

        /**
         * @Воронин Отображает текст на странице
         */
        inner class PageView(parent: ViewGroup) : View(parent.context) {
            @JvmField
            var holder: PageHolder? = null
            var time: TimeAnimatorCompat? = null
            lateinit var progress: FrameLayout
            lateinit var progressBar: ProgressBar
            lateinit var progressText: TextView
            var bm: Bitmap? = null // cache bitmap
            var cache: PageCursor? = null // cache cursor

            @JvmField
            var text: ZLTextElementAreaVector? = null
            @JvmField
            var info: Reflow.Info? = null
            var selection: SelectionView.PageView? = null
            var links: FBReaderView.LinksView? = null
            var bookmarks: FBReaderView.BookmarksView? = null
            var tts: FBReaderView.TTSView? = null
            var search: FBReaderView.SearchView? = null

            init {
                progress = FrameLayout(context)

                progressBar = object : ProgressBar(context) {
                    val mHandler = Handler()

                    override fun draw(canvas: Canvas) {
                        super.draw(canvas)
                        onAttachedToWindow() // startAnimation
                    }

                    override fun getVisibility(): Int {
                        return View.VISIBLE
                    }

                    override fun getWindowVisibility(): Int {
                        return View.VISIBLE
                    }

                    override fun scheduleDrawable(@NonNull who: Drawable, @NonNull what: Runnable, atTime: Long) {
                        if (time != null)
                            mHandler.postAtTime(what, atTime)
                        else
                            onDetachedFromWindow() // stopAnimation
                    }
                }
                progressBar.isIndeterminate = true
                progress.addView(progressBar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

                progressText = TextView(context)
                progressText.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                progress.addView(progressText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
            }

            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val w = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
                var h = getMainAreaHeight()
                if (fb.pluginview != null) {
                    if (!fb.pluginview!!.reflow) {
                        val c = current()
                        h = Math.ceil(fb.pluginview!!.getPageHeight(w, c!!)).toInt()
                    }
                }
                setMeasuredDimension(w, h)
            }

            fun current(): PageCursor? {
                val page = holder!!.adapterPosition
                if (page == -1)
                    return null
                return pages[page]
            }

            override fun onDraw(draw: Canvas) {
                val c = current()
                if (c == null) {
                    invalidate()
                    return
                }
                if (isCached(c)) {
                    drawCache(draw)
                    return
                }
                if (fb.pluginview != null) {
                    if (fb.pluginview!!.reflow) {
                        var page: Int
                        var index: Int
                        if (c.start == null) {
                            var p = c.end!!.paragraphIndex
                            var i = c.end!!.elementIndex
                            i = i - 1
                            if (i < 0)
                                p = p - 1
                            else
                                c.start = ZLTextFixedPosition(p, i, 0)
                            page = p
                            index = i
                        } else {
                            page = c.start!!.paragraphIndex
                            index = c.start!!.elementIndex
                        }
                        synchronized(lock) {
                            val w = width
                            val h = height
                            if (thread == null) {
                                if (fb.pluginview!!.reflower != null) {
                                    if (fb.pluginview!!.reflower!!.page != page || fb.pluginview!!.reflower!!.count() == -1 || fb.pluginview!!.reflower!!.w != w || fb.pluginview!!.reflower!!.h != h) {
                                        fb.pluginview!!.reflower!!.close()
                                        fb.pluginview!!.reflower = null
                                    }
                                }
                            }
                            if (fb.pluginview!!.reflower == null) {
                                if (thread == null) {
                                    thread = object : Thread("reflow load thread") {
                                        override fun run() {
                                            var i = index
                                            val pluginview = fb.pluginview // closeBook
                                            val reflower = Reflow(context, w, h, page, fb.app.BookTextView as FBReaderView.CustomView, fb.book!!.info!!)
                                            val bm = pluginview!!.render(reflower.w, reflower.h, page)
                                            reflower.load(bm!!)
                                            if (reflower.count() > 0)
                                                bm.recycle()
                                            if (i < 0) {
                                                i = reflower.emptyCount() + i
                                                c.start = ZLTextFixedPosition(page, i, 0)
                                            }
                                            reflower.index = i
                                            synchronized(lock) {
                                                pluginview.reflower = reflower
                                                thread = null
                                            }
                                        }
                                    }
                                    thread!!.priority = Thread.MIN_PRIORITY
                                    thread!!.start()
                                }
                            }
                            if (thread != null) {
                                if (time == null) {
                                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                                    if (pm.isPowerSaveMode) {
                                        fb.postDelayed({ invalidate() }, 1000)
                                    } else {
                                        time = TimeAnimatorCompat()
                                        time!!.start()
                                        time!!.setTimeListener(object : TimeAnimatorCompat.TimeListener {
                                            override fun onTimeUpdate(animation: TimeAnimatorCompat, totalTime: Long, deltaTime: Long) {
                                                invalidate()
                                            }
                                        })
                                    }
                                }
                                drawProgress(draw, page, index)
                                return
                            }
                            if (time != null) {
                                time!!.cancel()
                                time = null
                            }
                            val canvas = getCanvas(c)
                            fb.pluginview!!.current!!.pageNumber = page
                            fb.pluginview!!.reflower!!.index = c.start!!.elementIndex
                            if (fb.pluginview!!.reflower!!.count() > 0) {
                                val bm = fb.pluginview!!.reflower!!.render(c.start!!.elementIndex)
                                val src = Rect(0, 0, bm.width, bm.height)
                                val dst = Rect(fb.app.BookTextView.getLeftMargin(), 0, fb.app.BookTextView.getLeftMargin() + fb.pluginview!!.reflower!!.rw, fb.pluginview!!.reflower!!.h)
                                canvas.drawColor(Color.WHITE) // cache color always white
                                canvas.drawBitmap(bm, src, dst, null) // cache paint always clean
                                info = Reflow.Info(fb.pluginview!!.reflower!!, c.start!!.elementIndex)
                            } else { // empty source page?
                                fb.pluginview!!.drawWallpaper(canvas)
                                fb.pluginview!!.drawPage(canvas, w, h, fb.pluginview!!.reflower!!.bm!!)
                            }
                            update()
                            drawCache(draw)
                            onReflowerDone()
                        }
                        return
                    }
                    open(c)
                    fb.pluginview!!.drawOnCanvas(context, draw, width, height, ZLViewEnums.PageIndex.current, fb.app.BookTextView as FBReaderView.CustomView, fb.book!!.info!!)
                    update()
                } else {
                    open(c)
                    val context = ZLAndroidPaintContext(
                        fb.app.SystemInfo!!,
                        draw,
                        ZLAndroidPaintContext.Geometry(
                            width,
                            height,
                            width,
                            height,
                            0,
                            0
                        ),
                        verticalScrollbarWidth
                    )
                    fb.app.BookTextView.paint(context, ZLViewEnums.PageIndex.current)
                    text = fb.app.BookTextView.myCurrentPage.TextElementMap
                    fb.app.BookTextView.myCurrentPage.TextElementMap = ZLTextElementAreaVector()
                    update()
                }
            }

            fun drawProgress(canvas: Canvas, page: Int, index: Int) {
                canvas.drawColor(Color.GRAY)
                canvas.save()
                canvas.translate((width / 2 - progressBar.measuredWidth / 2).toFloat(), (height / 2 - progressBar.measuredHeight / 2).toFloat())

                val t = "${page + 1}.${if (index == -1) "*" else index}"
                progressText.text = t

                val dp60 = ThemeUtils.dp2px(context, 60f)
                progress.measure(View.MeasureSpec.makeMeasureSpec(dp60, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp60, View.MeasureSpec.EXACTLY))
                progress.layout(0, 0, dp60, dp60)
                progress.draw(canvas)

                canvas.restore()
            }

            fun recycle() {
                if (bm != null) {
                    bm!!.recycle()
                    bm = null
                }
                info = null
                text = null
                if (links != null) {
                    links!!.close()
                    links = null
                }
                if (bookmarks != null) {
                    bookmarks!!.close()
                    bookmarks = null
                }
                if (search != null) {
                    search!!.close()
                    search = null
                }
                selection = null
                if (time != null) {
                    time!!.cancel()
                    time = null
                }
            }

            fun isCached(c: PageCursor): Boolean {
                if (cache == null || cache != c) // should be same 'cache' memory ref
                    return false
                return bm != null
            }

            fun drawCache(draw: Canvas) {
                val src = Rect(0, 0, bm!!.width, bm!!.height)
                val dst = Rect(0, 0, width, height)
                draw.drawBitmap(bm!!, src, dst, fb.pluginview!!.paint)
            }

            fun getCanvas(c: PageCursor): Canvas {
                if (bm != null)
                    recycle()
                bm = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                cache = c
                return Canvas(bm!!)
            }
        }

        inner class PageHolder(@JvmField val page: PageView) : RecyclerView.ViewHolder(page)

        inner class PageCursor {
            @JvmField
            var start: ZLTextPosition? = null
            @JvmField
            var end: ZLTextPosition? = null

            constructor(s: ZLTextPosition?, e: ZLTextPosition?) {
                if (s != null)
                    start = ZLTextFixedPosition(s)
                if (e != null)
                    end = ZLTextFixedPosition(e)
            }

            fun equals(p1: ZLTextPosition, p2: ZLTextPosition): Boolean {
                return p1.charIndex == p2.charIndex && p1.elementIndex == p2.elementIndex && p1.paragraphIndex == p2.paragraphIndex
            }

            override fun equals(other: Any?): Boolean {
                val p = other as PageCursor
                if (start != null && p.start != null) {
                    if (equals(start!!, p.start!!))
                        return true
                }
                if (end != null && p.end != null) {
                    return equals(end!!, p.end!!)
                }
                return false
            }

            fun update(c: PageCursor) {
                if (c.start != null)
                    start = c.start
                if (c.end != null)
                    end = c.end
            }

            override fun toString(): String {
                var str = ""
                val format = "[%d,%d,%d]"
                if (start == null)
                    str += "- "
                else
                    str += String.format(Locale.ROOT, format, start!!.paragraphIndex, start!!.elementIndex, start!!.charIndex)
                if (end == null)
                    str += " -"
                else {
                    if (start != null)
                        str += " - "
                    str += String.format(Locale.ROOT, format, end!!.paragraphIndex, end!!.elementIndex, end!!.charIndex)
                }
                return str
            }
        }
    }

    inner class Gestures : GestureDetector.OnGestureListener, ZoomGestureHandler.ZoomListener {
        @JvmField
        var e: MotionEvent? = null
        var x: Int = 0
        var y: Int = 0
        var v: ScrollAdapter.PageView? = null
        var c: ScrollAdapter.PageCursor? = null
        lateinit var gestures: GestureDetectorCompat
        lateinit var brightness: FBReaderView.BrightnessGesture
        lateinit var zoomHandler: ZoomGestureHandler

        init {
            gestures = GestureDetectorCompat(fb.context, this)
            brightness = FBReaderView.BrightnessGesture(fb)
            zoomHandler = ZoomGestureHandler(fb.context, this)
        }

        override fun onZoomChange(scale: Float, pivotX: Float, pivotY: Float) {
            if (fb.listener != null) {
                fb.listener!!.onZoomChange(scale, pivotX, pivotY)
            }
            // Apply zoom to FBReaderView
            fb.scaleX = scale
            fb.scaleY = scale
            fb.pivotX = pivotX
            fb.pivotY = pivotY
        }

        override fun onZoomEnd() {
            if (fb.listener != null) {
                fb.listener!!.onZoomEnd()
            }
            // Reset zoom on FBReaderView
            fb.scaleX = 1.0f
            fb.scaleY = 1.0f
            fb.pivotX = 0f
            fb.pivotY = 0f
        }

        override fun getPageContentWidth(): Int? {
            // Get page content width for fit-width zoom calculation
            if (fb.pluginview != null && fb.pluginview!!.current != null) {
                // Use current.w - the rendered page width on screen (in pixels)
                // This accounts for actual display size including margins
                return fb.pluginview!!.current!!.w
            }
            // Default: use widget width
            return width
        }

        override fun getScreenWidth(): Int {
            return width
        }

        override fun getScreenHeight(): Int {
            return height
        }

        override fun onPanChange(offsetX: Float, offsetY: Float) {
            // Apply translation offset for pan when zoomed
            fb.translationX = offsetX
            fb.translationY = offsetY
        }

        fun open(e: MotionEvent): Boolean {
            if (!openCursor(e))
                return false
            return openText(e)
        }

        fun openCursor(e: MotionEvent): Boolean {
            this.e = e
            v = findView(e)
            if (v == null)
                return false
            // Adapt coordinates for zoom if needed
            val zoomAdapter = fb.getZoomTouchAdapter()
            x = zoomAdapter.adaptX(e.x, v!!)
            y = zoomAdapter.adaptY(e.y, v!!)
            val pos = v!!.holder!!.adapterPosition
            if (pos == -1)
                return false
            c = adapter.pages[pos]
            return true
        }

        fun openText(e: MotionEvent): Boolean {
            if (v!!.text == null)
                return false
            if (!fb.app.BookTextView.startCursor.samePositionAs(c!!.start!!))
                fb.app.BookTextView.gotoPosition(c!!.start)
            fb.app.BookTextView.myCurrentPage.TextElementMap = v!!.text!!
            return true
        }

        fun closeText() {
            fb.app.BookTextView.myCurrentPage.TextElementMap = ZLTextElementAreaVector()
        }

        override fun onDown(e: MotionEvent): Boolean {
            if (fb.app.BookTextView.mySelection.isEmpty())
                return false
            if (!open(e))
                return false
            fb.app.BookTextView.onFingerPress(x, y)
            v!!.invalidate()
            closeText()
            return true
        }

        override fun onShowPress(e: MotionEvent) {
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (!open(e)) { // pluginview or reflow
                (fb.app.BookTextView as FBReaderView.CustomView).onFingerSingleTapLastResort(e)
                return true
            }
            fb.app.BookTextView.onFingerSingleTap(x, y)
            v!!.invalidate()
            adapter.invalidates.add(v!!.holder!!)
            closeText()
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (fb.app.BookTextView.mySelection.isEmpty())
                return false
            if (!open(e!!))
                return false
            fb.app.BookTextView.onFingerMove(x, y)
            v!!.invalidate()
            adapter.invalidates.add(v!!.holder!!)
            closeText()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (!openCursor(e))
                return
            if (fb.pluginview != null) {
                val s = fb.pluginview!!.select(c!!.start!!, v!!.info, v!!.width, v!!.height, x, y)
                if (s != null) {
                    fb.tts?.selectionOpen(s)
                        ?: fb.selectionOpen(s)
                    return
                }
                fb.tts?.selectionClose()
                    ?: fb.selectionClose()
            }
            if (!openText(e))
                return
            if (fb.tts != null) {
                fb.tts!!.selectionOpen(c!!, x, y)
            } else {
                // onFingerReleaseAfterLongPress будет вызван при ACTION_UP через onReleaseCheck
                fb.app.BookTextView.onFingerLongPress(x, y)
            }
            v!!.invalidate()
            adapter.invalidates.add(v!!.holder!!)
            closeText()
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            return false
        }

        fun onReleaseCheck(e: MotionEvent): Boolean {
            if (fb.app.BookTextView.mySelection.isEmpty())
                return false
            if (e.action == MotionEvent.ACTION_UP) {
                if (!open(e))
                    return false
                fb.app.BookTextView.onFingerRelease(x, y)
                v!!.invalidate()
                closeText()
                return true
            }
            return false
        }

        fun onCancelCheck(e: MotionEvent): Boolean {
            if (fb.app.BookTextView.mySelection.isEmpty())
                return false
            if (e.action == MotionEvent.ACTION_CANCEL) {
                fb.app.BookTextView.onFingerEventCancelled()
                v!!.invalidate()
                return true
            }
            return false
        }

        fun onFilter(e: MotionEvent): Boolean {
            return !fb.app.BookTextView.mySelection.isEmpty()
        }

        fun onTouchEvent(e: MotionEvent): Boolean {
            // Process zoom gestures first, but don't intercept (returns false)
            // Only enable zoom for PDF/DJVU without reflow
            if (fb.pluginview != null && !fb.pluginview!!.reflow) {
                zoomHandler.onTouchEvent(e)
            }
            onReleaseCheck(e)
            onCancelCheck(e)
            if (brightness.onTouchEvent(e))
                return true
            if (gestures.onTouchEvent(e))
                return true
            return onFilter(e)
        }
    }
}
