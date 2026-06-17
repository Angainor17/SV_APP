package com.github.axet.bookreader.widgets

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.github.axet.androidlibrary.sound.TTS
import com.github.axet.androidlibrary.widgets.ThemeUtils
import com.github.axet.androidlibrary.widgets.Toast
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.BookApplication
import com.github.axet.bookreader.app.Plugin
import com.github.axet.bookreader.app.Reflow
import com.github.axet.bookreader.app.Storage
import org.geometerplus.fbreader.fbreader.TextBuildTraverser
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.text.view.ZLTextElement
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextParagraphCursor
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import org.geometerplus.zlibrary.text.view.ZLTextWord
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor
import java.util.Arrays
import java.util.Collections
import java.util.Locale

class TTSPopup(val fb: FBReaderView) {
    companion object {
        val TAG: String = TTSPopup::class.java.simpleName

        @JvmField
        val EOL = arrayOf("\n", "\r")
        @JvmField
        val STOPS = arrayOf(".", ";") // ",", "\"", "'", "!", "?", """, ":", "(", ")"
        @JvmField
        val MAX_COUNT = getMaxSpeechInputLength(200)
        @JvmField
        val TTS_BG_COLOR = 0xaaaaaa00.toInt()
        @JvmField
        val TTS_BG_ERROR_COLOR = 0xaaff0000.toInt()
        @JvmField
        val TTS_WORD_COLOR = 0x33333333

        @JvmStatic
        fun getMaxSpeechInputLength(max: Int): Int {
            return if (max > TextToSpeech.getMaxSpeechInputLength()) TextToSpeech.getMaxSpeechInputLength() else max
        }

        @JvmStatic
        fun getRect(
            pluginview: Plugin.View,
            v: ScrollWidget.ScrollAdapter.PageView,
            bm: Storage.Bookmark
        ): Rect? {
            val page = pluginview.selectPage(bm.start, v.info, v.width, v.height)
            val s = pluginview.select(bm.start, bm.end)
            return if (s != null) {
                val bb = s.getBounds(page)
                s.close()
                SelectionView.union(bb!!.rr!!.toList())
            } else {
                null
            }
        }

        @JvmStatic
        fun isStopSymbol(e: ZLTextElement?): Boolean {
            if (e is ZLTextWord) {
                val str = e.getString()
                return isStopSymbol(str)
            }
            return false
        }

        @JvmStatic
        fun isStopSymbol(str: String?): Boolean {
            if (str.isNullOrEmpty()) return true
            for (s in STOPS) {
                if (str.contains(s)) return true
            }
            return false
        }

        @JvmStatic
        fun isEOL(s: Plugin.View.Selection): Boolean {
            val str = s.getText() ?: return false
            for (e in EOL) {
                if (str == e) return true
            }
            return false
        }

        @JvmStatic
        fun stopOnLeft(e: ZLTextElement): Boolean {
            if (e is ZLTextWord) {
                val str = e.getString()
                return stopOnLeft(str)
            }
            return false
        }

        @JvmStatic
        fun stopOnLeft(str: String?): Boolean {
            if (str == null || str.length <= 1) return false
            for (s in STOPS) {
                if (str.startsWith(s)) return true
            }
            return false
        }

        @JvmStatic
        fun stopOnRight(e: ZLTextElement): Boolean {
            if (e is ZLTextWord) {
                val str = e.getString()
                return stopOnRight(str)
            }
            return false
        }

        @JvmStatic
        fun stopOnRight(str: String?): Boolean {
            if (str.isNullOrEmpty()) return false
            for (s in STOPS) {
                if (str.endsWith(s)) return true
            }
            return false
        }

        @JvmStatic
        fun isEmpty(bm: Storage.Bookmark?): Boolean {
            return bm == null || bm.start == null || bm.end == null
        }
    }

    val context: Context = fb.context
    val tts: TTS = object : TTS(context) {
        override fun getUserLocale(): Locale {
            val shared: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val lang = shared.getString(BookApplication.PREFERENCE_LANGUAGE, "") ?: ""
            return if (lang.isEmpty()) Locale.getDefault() else Locale(lang)
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            if (fb.tts == null) return
            marks.clear()
            marks.add(fragment!!.fragment)
            val bm = fragment!!.findWord(start, end)
            if (bm != null) {
                marks.add(bm)
                fragment!!.word = bm
            }
            if (fb.widget is ScrollWidget && (fb.widget as ScrollWidget).scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                val page = if (isEmpty(fragment!!.word)) fragment!!.fragment else fragment!!.word!!
                val pos = (fb.widget as ScrollWidget).adapter.findPage(page.start)
                if (pos != -1) {
                    val c = (fb.widget as ScrollWidget).adapter.pages[pos]
                    val first = (fb.widget as ScrollWidget).findFirstPage()
                    if (first != -1) {
                        val cur = (fb.widget as ScrollWidget).adapter.pages[first]
                        if (c != cur) {
                            val gravity = Runnable { updateGravity() }
                            if (c.end != null && cur.start != null && c.end!!.compareTo(cur.start!!) <= 0) {
                                onScrollFinished.add(gravity)
                                fb.scrollPrevPage()
                            }
                            if (c.start != null && cur.end != null && c.start!!.compareTo(cur.end!!) >= 0) {
                                onScrollFinished.add(gravity)
                                fb.scrollNextPage()
                            }
                        } else {
                            ensureVisible(page)
                        }
                    }
                }
            }
            fb.ttsUpdate()
        }

        override fun onError(utteranceId: String?, done: Runnable) {
            Log.d(TAG, "onError")
            if (fragment!!.isEmpty() && fragment!!.retry < 2) {
                dones.remove(delayed)
                handler.removeCallbacks(delayed)
                delayed = null
                dones.remove(speakNext)
                fragment!!.retry++
                ttsShowError("TTS Unknown error", 2000) { speakNext() }
            } else {
                done.run()
            }
        }

        override fun onDone(utteranceId: String?, done: Runnable) {
            Log.d(TAG, "onDone")
            super.onDone(utteranceId, done)
        }
    }

    @JvmField
    val marks: Storage.Bookmarks = Storage.Bookmarks()
    @JvmField
    var panel: View
    @JvmField
    var view: View
    var fragment: Fragment? = null
    var play: ImageView
    var onScrollFinished: ArrayList<Runnable> = ArrayList()
    val handler: Handler = Handler(Looper.getMainLooper())
    var gravity: Int = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM

    private val updateGravity = Runnable {
        val lp = panel.layoutParams as FrameLayout.LayoutParams
        lp.gravity = gravity
        panel.layoutParams = lp
    }

    var speakRetry: Runnable? = null

    private val speakNext = Runnable {
        Log.d(TAG, "speakNext")
        selectNext()
        speakNext()
    }

    init {
        tts.ttsCreate()
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.tts_popup, null)
        view.findViewById<View>(R.id.tts_left).setOnClickListener {
            stop()
            selectPrev()
        }
        view.findViewById<View>(R.id.tts_right).setOnClickListener {
            stop()
            selectNext()
        }
        play = view.findViewById(R.id.tts_play)
        play.setOnClickListener {
            if (tts.dones.contains(speakNext) || speakRetry != null) {
                stop()
            } else {
                resetColor()
                speakNext()
            }
        }
        view.findViewById<View>(R.id.tts_close).setOnClickListener { dismiss() }
        val dp20 = ThemeUtils.dp2px(context, 20f).toInt()
        val f = FrameLayout(context)
        val round = FrameLayout(context)
        round.setBackgroundResource(org.geometerplus.R.drawable.panel)
        round.addView(view)
        gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        f.addView(
            round,
            FrameLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                gravity
            )
        )
        f.setPadding(dp20, dp20, dp20, dp20)
        this.view = f
        this.panel = round
    }

    fun speakNext() {
        if (fragment == null) selectNext()
        val r = Runnable {
            fragment!!.last = System.currentTimeMillis()
            tts.playSpeech(TTS.Speak(tts.ttsLocale, fragment!!.fragmentText), speakNext)
            updatePlay()
        }
        if (fb.widget is ScrollWidget) {
            if ((fb.widget as ScrollWidget).scrollState == RecyclerView.SCROLL_STATE_IDLE)
                onScrollingFinished(ZLViewEnums.PageIndex.current)
        }
        if (onScrollFinished.isEmpty()) r.run() else onScrollFinished.add(r)
    }

    fun stop() {
        tts.close()
        tts.dones.remove(speakNext)
        speakRetry?.let { handler.removeCallbacks(it) }
        speakRetry = null
        updatePlay()
    }

    fun selectPrev() {
        marks.clear()
        if (fragment == null) {
            if (fb.widget is ScrollWidget) {
                val first = (fb.widget as ScrollWidget).findFirstPage()
                val c = (fb.widget as ScrollWidget).adapter.pages[first]
                val bm = expandWord(Storage.Bookmark("", c.start, c.start))
                fragment = Fragment(bm)
            }
            if (fb.widget is PagerWidget) {
                val position = fb.getPosition()
                val bm = expandWord(Storage.Bookmark("", position, position))
                fragment = Fragment(bm)
            }
        } else {
            val bm = selectPrev(fragment!!.fragment)
            fragment = Fragment(bm)
        }
        marks.add(fragment!!.fragment)
        if (fb.widget is ScrollWidget) {
            val pos = (fb.widget as ScrollWidget).adapter.findPage(fragment!!.fragment.start)
            if (pos == -1) return
            val nc = (fb.widget as ScrollWidget).adapter.pages[pos]
            val first = (fb.widget as ScrollWidget).findFirstPage()
            val cur = (fb.widget as ScrollWidget).adapter.pages[first]
            if (nc != cur) {
                onScrollFinished.add { }
                fb.scrollPrevPage()
            } else {
                ensureVisible(fragment!!.fragment)
            }
            updateGravity()
        }
        if (fb.widget is PagerWidget) {
            if (fb.pluginview == null) {
                val start = fb.app.bookTextView.getStartCursor()
                if (start != null && start.compareTo(fragment!!.fragment.end!!) >= 0) {
                    onScrollFinished.add { updateGravity() }
                    fb.scrollPrevPage()
                } else {
                    updateGravity()
                }
            } else {
                val s = fb.pluginview.select(fragment!!.fragment.start!!, fragment!!.fragment.end!!)
                val dst = (fb.widget as PagerWidget).getPageRect()
                val px = fb.pluginview.getPosition()
                if (px.paragraphIndex > fragment!!.fragment.start!!.paragraphIndex) {
                    onScrollFinished.add { updateGravity() }
                    fb.scrollPrevPage()
                } else {
                    val page = fb.pluginview.selectPage(
                        px,
                        (fb.widget as PagerWidget).getInfo(),
                        dst.width(),
                        dst.height()
                    )
                    val bounds = s!!.getBounds(page)
                    if (fb.pluginview.reflow) {
                        bounds!!.rr = fb.pluginview.boundsUpdate(
                            bounds.rr!!,
                            (fb.widget as PagerWidget).getInfo()!!
                        )
                        bounds.start = true
                        bounds.end = true
                    }
                    val ii = ArrayList(Arrays.asList(*bounds!!.rr!!))
                    Collections.sort(ii, SelectionView.LinesUL(ii))
                    s.close()
                    if (ii[ii.size - 1].bottom < (fb.widget as PagerWidget).top + fb.pluginview.current!!.pageOffset / fb.pluginview.current!!.ratio) {
                        onScrollFinished.add { updateGravity() }
                        fb.scrollPrevPage()
                    } else {
                        val r = SelectionView.union(Arrays.asList(*bounds.rr!!))
                        if ((fb.widget as PagerWidget).height / 2 < r.centerY())
                            updateGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP)
                        else
                            updateGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
                    }
                }
            }
        }
        fb.ttsUpdate()
    }

    fun updatePlay() {
        val p = tts.dones.contains(speakNext) || speakRetry != null
        play.setImageResource(if (p) R.drawable.ic_outline_pause_24 else R.drawable.ic_outline_play_arrow_24)
        fb.listener.ttsStatus(p)
    }

    fun selectNext() {
        marks.clear()
        if (fragment == null) {
            if (fb.widget is ScrollWidget) {
                val first = (fb.widget as ScrollWidget).findFirstPage()
                val c = (fb.widget as ScrollWidget).adapter.pages[first]
                val bm = expandWord(Storage.Bookmark("", c.start, c.start))
                fragment = Fragment(bm)
            }
            if (fb.widget is PagerWidget) {
                val position = fb.getPosition()
                val bm = expandWord(Storage.Bookmark("", position, position))
                fragment = Fragment(bm)
            }
        } else {
            val bm = selectNext(fragment!!.fragment)
            fragment = Fragment(bm)
        }
        marks.add(fragment!!.fragment)
        if (fb.widget is ScrollWidget) {
            val pos = (fb.widget as ScrollWidget).adapter.findPage(fragment!!.fragment.start)
            if (pos == -1) return
            val nc = (fb.widget as ScrollWidget).adapter.pages[pos]
            val first = (fb.widget as ScrollWidget).findFirstPage()
            val cur = (fb.widget as ScrollWidget).adapter.pages[first]
            if (nc != cur) {
                val page = (fb.widget as ScrollWidget).adapter.findPage(nc)
                onScrollFinished.add { updateGravity() }
                (fb.widget as ScrollWidget).smoothScrollToPosition(page)
            } else {
                ensureVisible(fragment!!.fragment)
                updateGravity()
            }
        }
        if (fb.widget is PagerWidget) {
            if (fb.pluginview == null) {
                val end = fb.app.bookTextView.getEndCursor()
                if (end != null && end.compareTo(fragment!!.fragment.start!!) <= 0) {
                    onScrollFinished.add { updateGravity() }
                    fb.scrollNextPage()
                } else {
                    updateGravity()
                }
            } else {
                val s = fb.pluginview.select(fragment!!.fragment.start!!, fragment!!.fragment.end!!)
                val dst = (fb.widget as PagerWidget).getPageRect()
                val px = fb.pluginview.getPosition()
                if (px.paragraphIndex < fragment!!.fragment.start!!.paragraphIndex) {
                    fb.scrollNextPage()
                } else {
                    val page = fb.pluginview.selectPage(
                        px,
                        (fb.widget as PagerWidget).getInfo(),
                        dst.width(),
                        dst.height()
                    )
                    val bounds = s!!.getBounds(page)
                    if (fb.pluginview.reflow) {
                        bounds!!.rr = fb.pluginview.boundsUpdate(
                            bounds.rr!!,
                            (fb.widget as PagerWidget).getInfo()!!
                        )
                        bounds.start = true
                        bounds.end = true
                    }
                    val ii = ArrayList(Arrays.asList(*bounds!!.rr!!))
                    Collections.sort(ii, SelectionView.LinesUL(ii))
                    s.close()
                    if (ii[0].bottom > (fb.widget as PagerWidget).bottom + fb.pluginview.current!!.pageOffset / fb.pluginview.current!!.ratio) {
                        onScrollFinished.add { updateGravity() }
                        fb.scrollNextPage()
                    } else {
                        val r = SelectionView.union(Arrays.asList(*bounds.rr!!))
                        if ((fb.widget as PagerWidget).height / 2 < r.centerY())
                            updateGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP)
                        else
                            updateGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
                    }
                }
            }
        }
        fb.ttsUpdate()
    }

    fun ttsShowError(text: String, delay: Int, done: Runnable) {
        for (m in marks) m.color = TTS_BG_ERROR_COLOR
        fb.ttsUpdate()
        speakRetry?.let { handler.removeCallbacks(it) }
        speakRetry = Runnable {
            resetColor()
            done.run()
            speakRetry = null
        }
        handler.postDelayed(speakRetry!!, delay.toLong())
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    fun selectNext(bm: Storage.Bookmark): Storage.Bookmark {
        if (fb.pluginview != null) {
            val start = bm.end!!
            val k = PluginWordCursor(start)
            if (k.nextWord()) {
                val end = expandRight(k)
                return Storage.Bookmark(k.text, start, end)
            }
            k.close()
            return bm
        } else {
            var start = bm.end!!
            val paragraphCursor =
                ZLTextParagraphCursor(fb.app.model!!.getTextModel()!!, start.paragraphIndex)
            val wordCursor = ZLTextWordCursor(paragraphCursor)
            wordCursor.moveTo(start)
            if (wordCursor.isEndOfParagraph) wordCursor.nextParagraph() else wordCursor.nextWord()
            start = wordCursor
            val end = expandRight(start)
            return Storage.Bookmark(bm.text!!, start, end)
        }
    }

    fun selectPrev(bm: Storage.Bookmark): Storage.Bookmark {
        if (fb.pluginview != null) {
            val end = bm.start!!
            val k = PluginWordCursor(end)
            if (k.prevWord()) {
                val start = expandLeft(k)
                return Storage.Bookmark(k.text, start, end)
            }
            k.close()
            return bm
        } else {
            var end = bm.start!!
            val paragraphCursor =
                ZLTextParagraphCursor(fb.app.model!!.getTextModel()!!, end.paragraphIndex)
            val wordCursor = ZLTextWordCursor(paragraphCursor)
            wordCursor.moveTo(end)
            wordCursor.previousWord()
            if (wordCursor.elementIndex < 0) {
                if (!wordCursor.previousParagraph()) {
                    wordCursor.moveTo(0, 0)
                } else {
                    wordCursor.moveToParagraphEnd()
                    wordCursor.previousWord()
                }
            }
            end = wordCursor
            val e = wordCursor.getElement()
            if (e is ZLTextWord) wordCursor.setCharIndex(e.length - 1)
            val start = expandLeft(end)
            return Storage.Bookmark(bm.text!!, start, end)
        }
    }

    fun show() {
        fb.addView(
            view,
            RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        view.visibility = View.VISIBLE
    }

    fun close() {
        fb.listener?.ttsStatus(false)
        view.visibility = View.GONE
        fb.removeView(view)
        fb.ttsClose()
        tts.close()
    }

    fun dismiss() {
        close()
        fb.tts = null
    }

    fun ensureVisible(bm: Storage.Bookmark) {
        val pos = (fb.widget as ScrollWidget).adapter.findPage(bm.start)
        val c = (fb.widget as ScrollWidget).adapter.pages[pos]
        val v = (fb.widget as ScrollWidget).findViewPage(c)
        val bottom = fb.top + (fb.widget as ScrollWidget).getMainAreaHeight()
        val rect: Rect? = if (fb.pluginview != null) {
            getRect(fb.pluginview, v, bm) ?: return
        } else {
            if (v.text == null) return
            FBReaderView.findUnion(v.text.areas(), bm) ?: return
        }
        rect!!.top += v.top
        rect.bottom += v.top
        rect.left += v.left
        rect.right += v.left
        var dy = 0
        if (rect.bottom > bottom) dy = rect.bottom - bottom
        if (rect.top < fb.top) dy = rect.top - fb.top
        (fb.widget as ScrollWidget).smoothScrollBy(0, dy)
    }

    fun scrollVerticallyBy(dy: Int) {
        gravity =
            if (dy > 0) Gravity.CENTER_HORIZONTAL or Gravity.TOP else Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        updateGravity()
    }

    fun updateGravity(g: Int) {
        gravity = g
        handler.removeCallbacks(updateGravity)
        handler.postDelayed(updateGravity, 200)
    }

    fun updateGravity() {
        if (fragment == null || marks.isEmpty()) {
            updateGravity(gravity)
            return
        }
        if (fb.pluginview == null) {
            var view: View? = null
            var text: org.geometerplus.zlibrary.text.view.ZLTextElementAreaVector? = null
            if (fb.widget is ScrollWidget) {
                val pos = (fb.widget as ScrollWidget).adapter.findPage(fragment!!.fragment.start)
                if (pos == -1) return
                val c = (fb.widget as ScrollWidget).adapter.pages[pos]
                val v = (fb.widget as ScrollWidget).findViewPage(c)
                view = v
                if (v != null) text = v.text
            }
            if (fb.widget is PagerWidget) {
                view = fb.widget as View
                text = fb.app.bookTextView.currentPage.textElementMap
            }
            if (view == null || text == null) {
                updateGravity(gravity)
            } else {
                val rr = ArrayList<Rect>()
                for (a in text.areas()) {
                    if (a.compareTo(fragment!!.fragment.start!!) >= 0 && a.compareTo(fragment!!.fragment.end!!) <= 0)
                        rr.add(Rect(a.XStart, a.YStart, a.XEnd, a.YEnd))
                }
                if (rr.isEmpty()) {
                    updateGravity(gravity)
                } else {
                    val r = SelectionView.union(rr)
                    if ((fb.widget as View).height / 2 < view.top + r.centerY())
                        updateGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP)
                    else
                        updateGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
                }
            }
        } else {
            var view: View? = null
            var info: Reflow.Info? = null
            if (fb.widget is ScrollWidget) {
                val pos = (fb.widget as ScrollWidget).adapter.findPage(fragment!!.fragment.start)
                if (pos == -1) return
                val c = (fb.widget as ScrollWidget).adapter.pages[pos]
                val v = (fb.widget as ScrollWidget).findViewPage(c)
                view = v
                info = v.info
            }
            if (fb.widget is PagerWidget) {
                view = fb.widget as View
                info = (fb.widget as PagerWidget).getInfo()
            }
            val s = fb.pluginview.select(fragment!!.fragment.start!!, fragment!!.fragment.end!!)
            if (s != null) {
                val page = fb.pluginview.selectPage(
                    fragment!!.fragment.start!!,
                    info,
                    view!!.width,
                    view.height
                )
                val bounds = s.getBounds(page)
                val r = SelectionView.union(Arrays.asList(*bounds!!.rr!!))
                s.close()
                if ((fb.widget as View).height / 2 < view.top + r.centerY())
                    updateGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP)
                else
                    updateGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
            }
        }
    }

    fun getText(start: ZLTextPosition, end: ZLTextPosition): String? {
        if (fb.pluginview != null) {
            val s = fb.pluginview.select(start, end)
            return if (s != null) {
                val str = s.getText()
                s.close()
                str
            } else null
        } else {
            val tt = TextBuildTraverser(fb.app.bookTextView)
            tt.traverse(start, end)
            return tt.getText()
        }
    }

    fun selectionOpen(s: Plugin.View.Selection) {
        marks.clear()
        var bm = Storage.Bookmark(s.getText()!!, s.getStart()!!, s.getEnd()!!)
        bm = expandWord(bm)
        fragment = Fragment(bm)
        marks.add(fragment!!.fragment)
        updateGravity()
        fb.ttsUpdate()
    }

    fun selectionOpen(x: Int, y: Int) {
        var bm = selectWord(fb.app.bookTextView.currentPage.textElementMap, x, y)
        bm = expandWord(bm)
        marks.clear()
        if (!isEmpty(bm)) {
            fragment = Fragment(bm)
            marks.add(fragment!!.fragment)
        }
        updateGravity()
        fb.ttsUpdate()
    }

    fun selectionOpen(c: ScrollWidget.ScrollAdapter.PageCursor, x: Int, y: Int) {
        val v = (fb.widget as ScrollWidget).findViewPage(c)
        var bm = selectWord(v.text, x, y)
        bm = expandWord(bm)
        marks.clear()
        if (!isEmpty(bm)) {
            fragment = Fragment(bm)
            marks.add(fragment!!.fragment)
        }
        updateGravity()
        fb.ttsUpdate()
    }

    fun selectionClose() {
        marks.clear()
        fb.ttsUpdate()
    }

    fun onScrollingFinished(pageIndex: ZLViewEnums.PageIndex) {
        for (r in onScrollFinished) r.run()
        onScrollFinished.clear()
        updateGravity()
    }

    fun selectWord(
        text: org.geometerplus.zlibrary.text.view.ZLTextElementAreaVector?,
        x: Int,
        y: Int
    ): Storage.Bookmark {
        var start: ZLTextPosition? = null
        var end: ZLTextPosition? = null
        for (a in text!!.areas()) {
            if (a.XStart < x && a.XEnd > x && a.YStart < y && a.YEnd > y) {
                if (start == null) start = a
                if (end == null) end = a
                if (start!!.compareTo(a) > 0) start = a
                if (end!!.compareTo(a) < 0) end = a
            }
        }
        return if (start == null || end == null) Storage.Bookmark() else Storage.Bookmark(
            getText(
                start,
                end
            )!!, start, end
        )
    }

    fun expandLeft(start: ZLTextPosition): ZLTextPosition {
        if (fb.pluginview != null) {
            var last: ZLTextPosition
            val k = PluginWordCursor(start)
            var count = 0
            do {
                last = ZLTextFixedPosition(k)
                k.prevWord()
                count++
            } while (!isStopSymbol(k.text) && count < MAX_COUNT)
            if (stopOnLeft(k.text)) last = ZLTextFixedPosition(k)
            k.close()
            return last
        } else {
            val paragraphCursor =
                ZLTextParagraphCursor(fb.app.model!!.getTextModel()!!, start.paragraphIndex)
            val wordCursor = ZLTextWordCursor(paragraphCursor)
            wordCursor.moveTo(start)
            wordCursor.setCharIndex(0)
            var last: ZLTextPosition
            var e: ZLTextElement? = null
            var count = 0
            do {
                last = ZLTextFixedPosition(wordCursor)
                wordCursor.previousWord()
                if (wordCursor.elementIndex < 0) {
                    if (!wordCursor.previousParagraph()) wordCursor.moveTo(0, 0)
                    break
                }
                e = wordCursor.getElement()
                count++
            } while (!isStopSymbol(e) && count < MAX_COUNT)
            if (stopOnLeft(e!!)) last = wordCursor
            return last
        }
    }

    fun expandRight(end: ZLTextPosition): ZLTextPosition {
        if (fb.pluginview != null) {
            val k = PluginWordCursor(end)
            var count = 0
            do {
                k.nextWord()
                count++
            } while (!(isStopSymbol(k.text) && stopOnRight(k.text)) && count < MAX_COUNT)
            val result = ZLTextFixedPosition(k)
            k.close()
            return result
        } else {
            val paragraphCursor =
                ZLTextParagraphCursor(fb.app.model!!.getTextModel()!!, end.paragraphIndex)
            val wordCursor = ZLTextWordCursor(paragraphCursor)
            wordCursor.moveTo(end)
            var count = 0
            var e: ZLTextElement = wordCursor.getElement()!!
            while (!(isStopSymbol(e) && stopOnRight(e)) && count < MAX_COUNT) {
                wordCursor.nextWord()
                count++
                e = wordCursor.getElement()!!
            }
            e = wordCursor.getElement()!!
            if (e is ZLTextWord) wordCursor.setCharIndex(e.length - 1)
            return wordCursor
        }
    }

    fun expandWord(bm: Storage.Bookmark): Storage.Bookmark {
        if (isEmpty(bm)) return bm
        val start = expandLeft(bm.start!!)
        val end = expandRight(bm.end!!)
        return Storage.Bookmark(getText(start, end)!!, start, end)
    }

    fun resetColor() {
        for (m in marks) m.color = TTS_BG_COLOR
        fb.ttsUpdate()
    }

    inner class Fragment(bm: Storage.Bookmark) {
        var fragment: Storage.Bookmark
        var fragmentText: String
        var fragmentWords: ArrayList<Bookmark> = ArrayList()
        var word: Storage.Bookmark? = null
        var retry: Int = 0
        var last: Long = 0

        init {
            var str = ""
            val list = ArrayList<Bookmark>()
            if (fb.pluginview != null) {
                var start = bm.start!!
                val end = bm.end!!
                val k = PluginWordCursor(start)
                if (k.nextWord()) {
                    while (k.compareTo(end) <= 0) {
                        val b =
                            Bookmark(k.text!!, ZLTextFixedPosition(start), ZLTextFixedPosition(k))
                        b.strStart = str.length
                        str += k.text!!
                        b.strEnd = str.length
                        str += " "
                        list.add(b)
                        start = ZLTextFixedPosition(k)
                        k.nextWord()
                    }
                }
                k.close()
            } else {
                val paragraphCursor = ZLTextParagraphCursor(
                    model = fb.app.model!!.getTextModel()!!,
                    index = bm.start!!.paragraphIndex
                )
                val wordCursor = ZLTextWordCursor(paragraphCursor)
                wordCursor.moveTo(bm.start!!)
                var e: ZLTextElement = wordCursor.getElement()!!
                while (wordCursor.compareTo(bm.end!!) < 0) {
                    if (e is ZLTextWord) {
                        val z = e.getString()
                        val b = Bookmark(
                            z,
                            ZLTextFixedPosition(wordCursor),
                            ZLTextFixedPosition(
                                wordCursor.paragraphIndex,
                                wordCursor.elementIndex,
                                wordCursor.charIndex + e.length
                            )
                        )
                        b.strStart = str.length
                        str += z
                        b.strEnd = str.length
                        str += " "
                        list.add(b)
                    }
                    wordCursor.nextWord()
                    e = wordCursor.getElement()!!
                }
            }
            fragmentText = str
            fragmentWords = list
            fragment = Storage.Bookmark(bm)
            fragment.color = TTS_BG_COLOR
            word = null
        }

        fun findWord(start: Int, end: Int): Storage.Bookmark? {
            for (bm in fragmentWords) {
                if (bm.strStart == start) return bm
            }
            return null
        }

        fun isEmpty(): Boolean {
            return fragmentText.trim().isEmpty()
        }

        inner class Bookmark(z: String, s: ZLTextPosition, e: ZLTextPosition) :
            Storage.Bookmark(z, s, e) {
            var strStart: Int = 0
            var strEnd: Int = 0

            init {
                color = TTS_WORD_COLOR
            }
        }
    }

    inner class PluginWordCursor(k: ZLTextPosition) : ZLTextPosition() {
        var p: Int = k.paragraphIndex
        var e: Int = k.elementIndex
        var c: Int = k.charIndex
        var all: Plugin.View.Selection? = null
        var allText: String? = null
        var text: String? = null

        fun getCurrent(): ZLTextPosition = ZLTextFixedPosition(p, e, c)

        fun left(): Boolean {
            if (all == null) return false
            e--
            if (e < 0) {
                e = 0
                p--
                if (p < 0) {
                    p = 0
                    return false
                } else {
                    all()
                    e = all!!.getEnd()!!.elementIndex - 1
                }
            }
            return true
        }

        fun right(): Boolean {
            if (all == null) return false
            e++
            if (e >= all!!.getEnd()!!.elementIndex) {
                e = 0
                p++
                val last = fb.pluginview.pagePosition().Total - 1
                if (p > last) {
                    p = last
                    return false
                } else {
                    all()
                }
            }
            return true
        }

        fun all() {
            close()
            all = fb.pluginview.select(p)
            if (all != null) allText = all!!.getText()
        }

        fun close() {
            all?.close()
            all = null
        }

        fun select(): String? =
            allText?.substring(getCurrent().elementIndex, getCurrent().elementIndex + 1)

        fun prevWord(): Boolean {
            all()
            if (all == null) return false
            val sp = p
            val se = e
            var s: String?
            do {
                if (!left()) break
                s = select()
            } while (!isWord(s))
            var k = e
            if (sp != p) k = se
            var last: Int
            do {
                last = e
                if (!left()) break
                s = select()
            } while (isWord(s) && !stopOnLeft(s))
            e = last
            val m =
                fb.pluginview.select(ZLTextFixedPosition(p, e, 0), ZLTextFixedPosition(sp, k, 0))
            if (m != null) {
                text = m.getText()
                m.close()
            }
            return true
        }

        fun isWord(str: String?): Boolean {
            if (str == null || str.length != 1) return false
            for (z in STOPS) {
                if (str.contains(z)) return true
            }
            return all!!.isWord(str[0])
        }

        fun nextWord(): Boolean {
            all()
            if (all == null) return false
            val sp = p
            val se = e
            var s: String?
            do {
                if (!right()) break
                s = select()
            } while (!isWord(s))
            var k = e
            if (sp != p) k = se
            var last: Int
            do {
                last = e
                if (!right()) break
                s = select()
            } while (isWord(s) && !stopOnRight(s))
            e = last
            val m =
                fb.pluginview.select(ZLTextFixedPosition(sp, k, 0), ZLTextFixedPosition(p, e, 0))
            if (m != null) {
                text = m.getText()
                m.close()
            }
            return true
        }

        override val paragraphIndex: Int get() = p
        override val elementIndex: Int get() = e
        override val charIndex: Int get() = c
    }
}
