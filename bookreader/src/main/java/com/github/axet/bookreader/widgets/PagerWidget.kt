package com.github.axet.bookreader.widgets

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.github.axet.bookreader.app.Plugin
import com.github.axet.bookreader.app.Reflow
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget

/**
 * Виджет для постраничного отображения книги.
 */
class PagerWidget(private val fb: FBReaderView) : ZLAndroidWidget(fb.context) {

    @JvmField val pinch: FBReaderView.PinchGesture?
    @JvmField val brightness: FBReaderView.BrightnessGesture

    private var x: Int = 0
    private var y: Int = 0

    private var selectionPage: ZLTextPosition? = null

    private val infos = ReflowMap<Reflow.Info>()
    private val links = ReflowMap<FBReaderView.LinksView>()
    private val bookmarks = ReflowMap<FBReaderView.BookmarksView>()
    private val tts = ReflowMap<FBReaderView.TTSView>()
    private val searchs = ReflowMap<FBReaderView.SearchView>()

    init {
        ZLApplication = object : ZLAndroidWidget.ZLApplicationInstance() {
            override fun Instance(): ZLApplication = fb.app
        }
        isFocusable = true

        fb.config.setValue(fb.app.pageTurningOptions.fingerScrolling, org.geometerplus.fbreader.fbreader.options.PageTurningOptions.FingerScrollingType.byTapAndFlick)

        pinch = if (Looper.myLooper() != null) { // render view only
            object : FBReaderView.PinchGesture(fb) {
                override fun onScaleBegin(x: Float, y: Float) {
                    pinchOpen(fb.pluginview!!.current!!.pageNumber, getPageRect())
                }
            }
        } else null

        brightness = FBReaderView.BrightnessGesture(fb)
    }

    /**
     * Возвращает прямоугольник текущей страницы.
     */
    fun getPageRect(): Rect {
        val dst: Rect
        if (fb.pluginview!!.reflow) {
            dst = Rect(0, 0, width, height)
        } else {
            val p = fb.pluginview!!.current!! // не использует current.renderRect() - показывает частичную страницу
            if (p.pageOffset < 0) { // показываем пустое место в начале
                val t = (-p.pageOffset / p.ratio).toInt()
                dst = Rect(0, t, p.w, t + (p.pageBox!!.h / p.ratio).toInt())
            } else if (p.pageOffset == 0 && p.hh > p.pageBox!!.h) {  // показываем по центру по вертикали
                val t = ((p.hh - p.pageBox!!.h) / p.ratio / 2).toInt()
                dst = Rect(0, t, p.w, p.h - t)
            } else {
                val t = (-p.pageOffset / p.ratio).toInt()
                dst = Rect(0, t, p.w, t + (p.pageBox!!.h / p.ratio).toInt())
            }
        }
        return dst
    }

    override fun getScreenBrightness(): Int = brightness.getScreenBrightness()

    override fun setScreenBrightness(percent: Int) {
        myColorLevel = brightness.setScreenBrightness(percent)
        postInvalidate()
        updateColorLevel()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = false

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean = false

    override fun drawOnBitmap(bitmap: Bitmap, index: ZLViewEnums.PageIndex) {
        if (fb.pluginview != null) {
            fb.pluginview!!.drawOnBitmap(context, bitmap, width, mainAreaHeight, index, fb.app.bookTextView as FBReaderView.CustomView, fb.book.info)
            var info: Reflow.Info? = null
            val position: ZLTextPosition
            if (fb.pluginview!!.reflow) {
                position = ZLTextFixedPosition(fb.pluginview!!.reflower!!.page, fb.pluginview!!.reflower!!.index + fb.pluginview!!.reflower!!.pending, 0)
                info = Reflow.Info(fb.pluginview!!.reflower!!, position.elementIndex)
                infos.put(position, info)
            } else {
                val old = object : Plugin.Page(fb.pluginview!!.current!!) {
                    override fun load() {}
                    override fun getPagesCount(): Int = fb.pluginview!!.current!!.getPagesCount()
                }
                old.load(index)
                position = ZLTextFixedPosition(old.pageNumber, 0, 0)
            }
            val dst = getPageRect()
            val page = fb.pluginview!!.selectPage(position, info, dst.width(), dst.height())
            val l = FBReaderView.LinksView(fb, fb.pluginview!!.getLinks(page), info)
            val lold = links.put(position, l)
            lold?.close()
            val b = FBReaderView.BookmarksView(fb, page, fb.book.info.bookmarks, info)
            val bold = bookmarks.put(position, b)
            bold?.close()
            if (fb.tts != null) {
                val t = FBReaderView.TTSView(fb, page, info)
                val told = tts.put(position, t)
                told?.close()
            }
            if (fb.search != null) {
                val s = FBReaderView.SearchView(fb, fb.search!!.getBounds(page), info)
                val sold = searchs.put(position, s)
                sold?.close()
            }
        } else {
            super.drawOnBitmap(bitmap, index)
        }
    }

    fun updateOverlaysReset() {
        updateOverlays()
        resetCache() // нужно для drawonbitmap
    }

    private fun getPosition(): ZLTextFixedPosition {
        return if (fb.pluginview!!.reflow)
            ZLTextFixedPosition(fb.pluginview!!.reflower!!.page, fb.pluginview!!.reflower!!.index, 0)
        else
            ZLTextFixedPosition(fb.pluginview!!.current!!.pageNumber, 0, 0)
    }

    fun updateOverlays() {
        fb.invalidateFooter()
        if (fb.pluginview != null) {
            val dst = getPageRect()
            var x = dst.left
            var y = dst.top
            if (fb.pluginview!!.reflow) {
                val info = getInfo()
                if (info != null) // в onDrawInScrolling onScrollingFinished вызывается перед onDrawStatic, вызывая info == null
                    x += info.margin.left
            }

            val position = getPosition()

            for (l in links.values) {
                l?.hide()
            }
            links[position]?.let {
                it.show()
                it.update(x, y)
            }

            for (b in bookmarks.values) {
                b?.hide()
            }
            bookmarks[position]?.let {
                it.show()
                it.update(x, y)
            }

            for (t in tts.values) {
                t?.hide()
            }
            tts[position]?.let {
                it.show()
                it.update(x, y)
            }

            for (s in searchs.values) {
                s?.hide()
            }
            searchs[position]?.let {
                it.show()
                it.update(x, y)
            }

            if (selectionPage != null && !selectionPage!!.samePositionAs(position)) {
                fb.post { fb.selectionClose() }
                selectionPage = null
            }
        }
    }

    fun linksClose() {
        for (l in links.values) {
            l?.close()
        }
        links.clear()
    }

    fun bookmarksClose() {
        for (l in bookmarks.values) {
            l?.close()
        }
        bookmarks.clear()
    }

    fun ttsClose() {
        for (l in tts.values) {
            l?.close()
        }
        tts.clear()
    }

    fun searchClose() {
        for (l in searchs.values) {
            l?.close()
        }
        searchs.clear()
    }

    @Suppress("UNCHECKED_CAST")
    fun searchPage(page: Int) {
        val w = width
        val h = mainAreaHeight

        fb.pluginview!!.current!!.w = w
        fb.pluginview!!.current!!.h = h
        fb.pluginview!!.current!!.load(page, 0)
        fb.pluginview!!.current!!.renderPage()

        val dst = getPageRect()

        if (fb.pluginview!!.reflow) {
            if (fb.pluginview!!.reflower != null && (fb.pluginview!!.reflower!!.page != page || fb.pluginview!!.reflower!!.w != w || fb.pluginview!!.reflower!!.h != h)) {
                fb.pluginview!!.reflower!!.close()
                fb.pluginview!!.reflower = null
            }
            if (fb.pluginview!!.reflower == null) {
                fb.pluginview!!.reflower = Reflow(context, w, h, page, fb.app.bookTextView as FBReaderView.CustomView, fb.book.info)
                val bm = fb.pluginview!!.render(fb.pluginview!!.reflower!!.rw, fb.pluginview!!.reflower!!.h, page)!!
                fb.pluginview!!.reflower!!.load(bm, page, 0)
            }
            for (i in 0 until fb.pluginview!!.reflower!!.count()) {
                val info = Reflow.Info(fb.pluginview!!.reflower!!, i)
                val pos = ZLTextFixedPosition(page, i, 0)
                val p = fb.pluginview!!.selectPage(pos, info, fb.pluginview!!.reflower!!.w, fb.pluginview!!.reflower!!.h)
                val bb = fb.search!!.getBounds(p)!!
                if (bb.rr != null) {
                    bb.rr = fb.pluginview!!.boundsUpdate(bb.rr!!, info)
                    if (bb.highlight != null) {
                        val hh = HashSet(bb.highlight!!.toList())
                        for (r in bb.rr!!) {
                            if (hh.contains(r))
                                fb.pluginview!!.gotoPosition(ZLTextFixedPosition(page, i, 0))
                        }
                    }
                }
            }
            resetCache()
        } else {
            val pos = ZLTextFixedPosition(page, 0, 0)
            val p = fb.pluginview!!.selectPage(pos, getInfo(), dst.width(), dst.height())
            val bb = fb.search!!.getBounds(p)!!
            if (bb.rr != null) {
                if (bb.highlight != null) {
                    val hh = HashSet(bb.highlight!!.toList())
                    for (r in bb.rr!!) {
                        if (hh.contains(r)) {
                            var offset = 0
                            val t = r.top + dst.top
                            val b = r.bottom + dst.top
                            val curr = fb.pluginview!!.current!!
                            while (t - offset / curr.ratio > bottom || b - offset / curr.ratio > bottom && r.height() < mainAreaHeight)
                                offset += curr.pageStep
                            fb.pluginview!!.gotoPosition(ZLTextFixedPosition(page, offset, 0))
                            resetCache()
                            return
                        }
                    }
                }
            }
        }
    }

    fun resetCache() { // не сбрасывает reflower
        super.reset()
        repaint()
    }

    override fun reset() {
        super.reset()
        if (fb.pluginview != null) {
            fb.pluginview!!.reflower?.reset()
        }
        infos.clear()
        linksClose()
        bookmarksClose()
        searchClose()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        x = event.x.toInt()
        y = event.y.toInt()
        if (fb.pluginview != null && !fb.pluginview!!.reflow) {
            if (pinch?.onTouchEvent(event) == true)
                return true
        }
        return super.onTouchEvent(event)
    }

    fun getInfo(): Reflow.Info? {
        if (fb.pluginview!!.reflower == null)
            return null
        return infos[ZLTextFixedPosition(fb.pluginview!!.reflower!!.page, fb.pluginview!!.reflower!!.index, 0)]
    }

    override fun onLongClick(v: View): Boolean {
        if (fb.pluginview != null) {
            val dst = getPageRect()
            val pos = getPosition()
            val s = fb.pluginview!!.select(pos, getInfo(), dst.width(), dst.height(), x - dst.left, y - dst.top)
            if (s != null) {
                if (fb.tts != null) {
                    fb.tts!!.selectionOpen(s)
                } else {
                    selectionPage = pos
                    fb.selectionOpen(s)
                    val page = fb.pluginview!!.selectPage(pos, getInfo(), dst.width(), dst.height())
                    val run = {
                        var x = dst.left
                        var y = dst.top
                        if (fb.pluginview!!.reflow)
                            x += getInfo()!!.margin.left
                        fb.selection.update(fb.selection.getChildAt(0) as SelectionView.PageView, x, y)
                    }
                    val setter = object : Plugin.View.Selection.Setter {
                        override fun setStart(x: Int, y: Int) {
                            val point = fb.pluginview!!.selectPoint(getInfo(), x - dst.left, y - dst.top)
                            if (point != null)
                                s.setStart(page, point)
                            run()
                        }

                        override fun setEnd(x: Int, y: Int) {
                            val point = fb.pluginview!!.selectPoint(getInfo(), x - dst.left, y - dst.top)
                            if (point != null)
                                s.setEnd(page, point)
                            run()
                        }

                        override fun getBounds(): Plugin.View.Selection.Bounds {
                            val bounds = s.getBounds(page)!!
                            if (fb.pluginview!!.reflow) {
                                bounds.rr = fb.pluginview!!.boundsUpdate(bounds.rr!!, getInfo()!!)
                                bounds.start = true
                                bounds.end = true
                            }
                            return bounds
                        }
                    }
                    val view = SelectionView.PageView(context, fb.app.bookTextView as FBReaderView.CustomView, setter)
                    fb.selection.add(view)
                    run()
                }
                return true
            }
            if (fb.tts != null)
                fb.tts!!.selectionClose()
            else
                fb.selectionClose()
        }
        if (fb.tts != null) {
            fb.tts!!.selectionOpen(x, y)
            return true
        }
        return super.onLongClick(v)
    }

    /**
     * HashMap для хранения reflow информации с ограничением размера.
     */
    inner class ReflowMap<V> : HashMap<ZLTextPosition, V>() {
        private val last = ArrayList<ZLTextPosition>()

        override fun put(key: ZLTextPosition, value: V): V? {
            val v = super.put(key, value)
            if (fb.pluginview!!.reflower != null) {
                val l = fb.pluginview!!.reflower!!.emptyCount() - 1
                if (key.elementIndex == l) {
                    val n = ZLTextFixedPosition(key.paragraphIndex + 1, -1, 0)
                    super.put(n, value) // игнорируем результат, дублируем ключ для того же значения
                    last.add(n) // (3,-1,0) == (2,2,0) когда (2,1,0) последний

                    val k = ZLTextFixedPosition(key.paragraphIndex, l + 1, 0)
                    val kv = get(ZLTextFixedPosition(key.paragraphIndex + 1, 0, 0))
                    if (kv != null) {
                        super.put(k, kv) // игнорируем результат, дублируем ключ для того же значения
                        last.add(k) // (2,2,0) == (3,0,0) когда (2,1,0) последний
                    }
                }
                if (key.elementIndex == 0) {
                    val p = key.paragraphIndex - 1
                    for (k in keys) {
                        if (k.paragraphIndex == p && get(k) == null)
                            super.put(k, value) // обновляем (2,3,0) == (3,0,0)
                    }
                }
            }
            if (v != null)
                return v
            last.add(key)
            if (last.size > 9) { // количество возможных старых значений + дубликаты
                val k = last.removeAt(0)
                return remove(k)
            }
            return null
        }

        override fun clear() {
            super.clear()
            last.clear()
        }
    }
}
