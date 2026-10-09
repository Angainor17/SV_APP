package com.github.axet.bookreader.widgets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.os.PowerManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.github.axet.androidlibrary.net.HttpClient
import com.github.axet.androidlibrary.preferences.AboutPreferenceCompat
import com.github.axet.androidlibrary.widgets.ThemeUtils
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.Plugin
import com.github.axet.bookreader.app.PluginPage
import com.github.axet.bookreader.app.PluginView
import com.github.axet.bookreader.app.ReaderPreferences
import com.github.axet.bookreader.app.Reflow
import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.services.ImagesProvider
import com.google.android.material.snackbar.Snackbar
import org.geometerplus.android.fbreader.dict.DictionaryUtil
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow
import org.geometerplus.android.util.UIMessageUtil
import org.geometerplus.android.util.UIUtil
import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.fbreader.bookmodel.FBHyperlinkType
import org.geometerplus.fbreader.bookmodel.TOCTree
import org.geometerplus.fbreader.fbreader.ActionCode
import org.geometerplus.fbreader.fbreader.FBAction
import org.geometerplus.fbreader.fbreader.FBView
import org.geometerplus.fbreader.fbreader.options.ColorProfile
import org.geometerplus.fbreader.fbreader.options.FooterOptions
import org.geometerplus.fbreader.fbreader.options.ImageOptions
import org.geometerplus.fbreader.fbreader.options.MiscOptions
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions
import org.geometerplus.fbreader.formats.FormatPlugin
import org.geometerplus.fbreader.util.AutoTextSnippet
import org.geometerplus.fbreader.util.TextSnippet
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.options.StringPair
import org.geometerplus.zlibrary.core.options.ZLOption
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.view.ZLPaintContext
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.core.view.ZLViewWidget
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator
import org.geometerplus.zlibrary.text.model.ZLTextModel
import org.geometerplus.zlibrary.text.model.ZLTextParagraph
import org.geometerplus.zlibrary.text.view.ZLTextControlElement
import org.geometerplus.zlibrary.text.view.ZLTextElement
import org.geometerplus.zlibrary.text.view.ZLTextElementArea
import org.geometerplus.zlibrary.text.view.ZLTextElementAreaVector
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextHighlighting
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink
import org.geometerplus.zlibrary.text.view.ZLTextHyperlinkRegionSoul
import org.geometerplus.zlibrary.text.view.ZLTextImageElement
import org.geometerplus.zlibrary.text.view.ZLTextImageRegionSoul
import org.geometerplus.zlibrary.text.view.ZLTextParagraphCursor
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import org.geometerplus.zlibrary.text.view.ZLTextRegion
import org.geometerplus.zlibrary.text.view.ZLTextView
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor
import org.geometerplus.zlibrary.text.view.ZLTextWordRegionSoul
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidPaintContext
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget
import com.github.axet.bookreader.domain.cleanBookmarkText
import su.sv.managers.OnBookPagerManager
import timber.log.Timber
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import java.util.TreeMap

open class FBReaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {

    lateinit var app: FBReaderApp
    lateinit var config: ConfigShadow
    @JvmField
    var widget: ZLViewWidget? = null
    var battery: Int = 0
    var book: Storage.FBook? = null
    var pluginview: PluginView? = null
    var listener: Listener? = null
    var tts: TTSPopup? = null
    var title: String? = null
    var w: Window? = null
    var footer: FBFooterView? = null
    var selection: SelectionView? = null
    var scrollDelayed: ZLTextPosition? = null
    var scrollCentered: Boolean = false
    @JvmField
    var drawer: DrawerLayout? = null
    var search: PluginView.Search? = null
    var searchPagePending: Int = 0
    private var isFullscreenMode = false
    private var searchCurrentIndex = 0
    private var searchTotalCount = 0

    init {
        create()
    }

    companion object {
        @JvmField
        val ACTION_MENU: String = FBReaderView::class.java.canonicalName + ".ACTION_MENU"

        const val PAGE_OVERLAP_PERCENTS = 5
        const val PAGE_PAPER_COLOR = 0x80ffffff

        @JvmStatic
        fun showControls(p: ViewGroup, areas: View) {
            p.removeCallbacks(areas.tag as? Runnable)
            p.addView(areas)
            val hide = Runnable { hideControls(p, areas) }
            areas.tag = hide
            p.postDelayed(hide, 3000)
        }

        @JvmStatic
        fun hideControls(p: ViewGroup, areas: View) {
            p.removeCallbacks(areas.tag as? Runnable)
            areas.tag = null
            val v = ValueAnimator.ofFloat(1f, 0f)
            v.addUpdateListener { animation -> ViewCompat.setAlpha(areas, animation.animatedValue as Float) }
            v.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    p.removeView(areas)
                }
            })
            v.duration = 500
            v.start()
        }

        @JvmStatic
        fun findUnion(areas: List<ZLTextElementArea>, bm: Storage.Bookmark): Rect? {
            var union: Rect? = null
            for (a in areas) {
                if (bm.start!!.compareTo(a) <= 0 && bm.end!!.compareTo(a) >= 0) {
                    if (union == null)
                        union = Rect(a.XStart, a.YStart, a.XEnd, a.YEnd)
                    else
                        union.union(a.XStart, a.YStart, a.XEnd, a.YEnd)
                }
            }
            return union
        }
    }

    fun create() {
        config = ConfigShadow()
        app = FBReaderApp(context)

        app.setWindow(FBApplicationWindow())
        app.initWindow()

        configInit()

        app.BookTextView = CustomView(app)
        app.setView(app.BookTextView)

        footer = FBFooterView(context, this)

        setWidget(Widgets.PAGING)
    }

    fun configColorProfile() {
        config.setValue(app.ViewOptions.ColorProfileName, ColorProfile.DAY)
        val p = ColorProfile.get(ColorProfile.DAY)
        config.setValue(p.BackgroundOption, 0xF5E5CC)
        config.setValue(p.WallpaperOption, "")
    }

    fun configWidget(shared: SharedPreferences) {
        val mode = shared.getString(ReaderPreferences.PREFERENCE_VIEW_MODE, "")
        setWidget(if (mode == Widgets.CONTINUOUS.toString()) Widgets.CONTINUOUS else Widgets.PAGING)
    }

    open fun configInit() {
        val shared = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
        configColorProfile()

        val d = shared.getInt(ReaderPreferences.PREFERENCE_FONTSIZE_FBREADER, app.ViewOptions.textStyleCollection.baseStyle.FontSizeOption.getValue())
        config.setValue(app.ViewOptions.textStyleCollection.baseStyle.FontSizeOption, d)

        val f = shared.getString(ReaderPreferences.PREFERENCE_FONTFAMILY_FBREADER, app.ViewOptions.textStyleCollection.baseStyle.FontFamilyOption.getValue())!!
        config.setValue(app.ViewOptions.textStyleCollection.baseStyle.FontFamilyOption, f)

        val ignoreCSSFonts = shared.getBoolean(ReaderPreferences.PREFERENCE_IGNORE_EMBEDDED_FONTS, false)
        config.setValue(app.ViewOptions.textStyleCollection.baseStyle.UseCSSFontFamilyOption, !ignoreCSSFonts)

        config.setValue(app.MiscOptions.AllowScreenBrightnessAdjustment, false)
        config.setValue(app.ViewOptions.ScrollbarType, 0) // FBView.SCROLLBAR_SHOW_AS_FOOTER
        config.setValue(app.ViewOptions.footerOptions.ShowProgress, FooterOptions.ProgressDisplayType.asPages)

        config.setValue(app.ImageOptions.TapAction, ImageOptions.TapActionEnum.openImageView)
        config.setValue(fitToScreenOption(), imageFitting("covers"))

        config.setValue(app.MiscOptions.WordTappingAction, MiscOptions.WordTappingActionEnum.startSelecting)

        // Two column view setting
        val twoColumnValue = shared.getString(ReaderPreferences.PREFERENCE_TWO_COLUMN_VIEW, "auto")
        val twoColumnEnabled: Boolean
        if (twoColumnValue == "true") {
            twoColumnEnabled = true
        } else if (twoColumnValue == "false") {
            twoColumnEnabled = false
        } else {
            // auto - use default FBReader logic (based on screen size)
            twoColumnEnabled = app.ViewOptions.TwoColumnView.getValue()
        }
        config.setValue(app.ViewOptions.TwoColumnView, twoColumnEnabled)
    }

    fun setWidget(w: Widgets) {
        when (w) {
            Widgets.CONTINUOUS -> setWidget(ScrollWidget(this))
            Widgets.PAGING -> setWidget(PagerWidget(this))
        }
    }

    fun getWidgetType(): Widgets {
        if (widget is ScrollWidget) {
            return Widgets.CONTINUOUS
        } else if (widget is PagerWidget) {
            return Widgets.PAGING
        }
        return Widgets.PAGING
    }

    fun setWidget(v: ZLViewWidget) {
        if (selection != null) {
            Timber.tag("voronin").d("FBReaderView setWidget: closing selection before mode switch")
            selectionClose()
        }
        overlaysClose()
        var pos: ZLTextPosition? = null
        if (widget != null) {
            pos = position
            Timber.tag("voronin").d("FBReaderView setWidget: resetting zoom before mode switch")
            scaleX = 1.0f
            scaleY = 1.0f
            translationX = 0f
            translationY = 0f
            removeView(widget as View)
        }
        widget = v
        var lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        lp.addRule(RelativeLayout.ABOVE, footer!!.id)
        addView(v as View, 0, lp)
        if (pos != null)
            gotoPosition(pos)
        if (footer != null)
            removeView(footer)
        lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        addView(footer, lp)
    }

    fun loadBook(fbook: Storage.FBook) {
        try {
            book = fbook
            if (book!!.info == null)
                book!!.info = Storage.RecentInfo()
            val plugin = Storage.getPlugin(app.SystemInfo as Storage.Info, fbook)
            if (plugin is Plugin) {
                pluginview = plugin.create(fbook)
                val model = BookModel.createModel(fbook.book!!, plugin)
                app.BookTextView.setModel(model.textModel)
                app.Model = model
                if (book!!.info!!.position != null)
                    gotoPluginPosition(book!!.info!!.position)
            } else {
                val model = BookModel.createModel(fbook.book!!, plugin)
                ZLTextHyphenator.Instance().load(fbook.book!!.getLanguage())
                app.BookTextView.setModel(model.textModel)
                app.Model = model
                if (book!!.info!!.position != null)
                    app.BookTextView.gotoPosition(book!!.info!!.position!!)
                if (book!!.info!!.scale != null)
                    config.setValue(fitToScreenOption(), book!!.info!!.scale!!)
                if (book!!.info!!.fontsize != null)
                    config.setValue(app.ViewOptions.textStyleCollection.baseStyle.FontSizeOption, book!!.info!!.fontsize!!)
                bookmarksUpdate()
            }
            widget!!.repaint()
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun closeBook() {
        if (pluginview != null) {
            pluginview!!.close()
            pluginview = null
        }
        app.BookTextView.setModel(null)
        app.Model = null
        book = null
        if (tts != null) {
            tts!!.close()
            tts = null
        }
    }

    val position: ZLTextPosition
        get() {
        if (pluginview != null) {
            if (widget is ScrollWidget) {
                val first = (widget as ScrollWidget).findFirstPage()
                if (first != -1) {
                    val h = (widget as ScrollWidget).findViewHolderForAdapterPosition(first)
                    val p = h!!.itemView as ScrollWidget.ScrollAdapter.PageView
                    val c = (widget as ScrollWidget).adapter.pages[first]
                    val info = pluginview!!.getPageInfo(p.width, p.height, c)
                    if (p.info != null) { // reflow can be true but reflower == null
                        val rr = ArrayList(p.info!!.dst.keys)
                        Collections.sort(rr, SelectionView.UL())
                        val top = -p.top
                        for (r in rr) {
                            if (r.top > top || (r.top < top && r.bottom > top)) {
                                val screen = r.top - top // offset from top screen to top element
                                val ratio = p.info!!.bm.width() / p.width.toFloat()
                                var offset = (p.info!!.dst[r]!!.top / ratio - screen).toInt() // recommended page offset
                                offset = (offset * info!!.ratio).toInt()
                                return ZLTextFixedPosition(pluginview!!.current!!.pageNumber, offset, 0)
                            }
                        }
                    } else {
                        var top = -p.top
                        if (top < 0)
                            top = 0
                        val offset = (top * info!!.ratio).toInt()
                        return ZLTextFixedPosition(pluginview!!.current!!.pageNumber, offset, 0)
                    }
                }
            }
            return pluginview!!.getPosition()
        } else {
            if (widget is ScrollWidget) {
                val first = (widget as ScrollWidget).findFirstPage()
                if (first != -1) {
                    val c = (widget as ScrollWidget).adapter.pages[first]
                    val h = (widget as ScrollWidget).findViewHolderForAdapterPosition(first)
                    val p = h!!.itemView as ScrollWidget.ScrollAdapter.PageView
                    if (p.text != null) { // happens when view invalidate / recycled before calling getPosition()
                        val top = -p.top
                        for (a in p.text!!.areas()) {
                            if (a.YStart > top || (a.YStart < top && a.YEnd > top)) {
                                val paragraphCursor = ZLTextParagraphCursor(app.Model!!.textModel!!, a.paragraphIndex)
                                val wordCursor = ZLTextWordCursor(paragraphCursor)
                                wordCursor.moveTo(a)
                                var last: ZLTextFixedPosition
                                var e: ZLTextElement
                                do {
                                    last = ZLTextFixedPosition(wordCursor)
                                    wordCursor.previousWord()
                                    e = wordCursor.element
                                } while (e is ZLTextControlElement && wordCursor.compareTo(c.start!!) >= 0)
                                return last
                            }
                        }
                    }
                }
            }
            return ZLTextFixedPosition(app.BookTextView.startCursor)
        }
    }

    fun setWindow(w: Window) {
        this.w = w
    }

    fun setActivity(a: Activity, onBookPagerManager: OnBookPagerManager) {
        app.addAction(ActionCode.DISPLAY_BOOK_POPUP, object : FBAction(app) {
            override fun run(vararg params: Any?) {
            }
        })
        app.addAction(ActionCode.PROCESS_HYPERLINK, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                if (pluginview != null) {
                    val l = params[0] as BookModel.Label
                    showHyperlink(l)
                    return
                }
                val region = app.BookTextView.outlinedRegion
                if (region == null) {
                    return
                }
                val soul = region.soul
                if (soul is ZLTextHyperlinkRegionSoul) {
                    app.BookTextView.hideOutline()
                    val hyperlink = soul.Hyperlink
                    when (hyperlink.Type) {
                        FBHyperlinkType.EXTERNAL ->
                            AboutPreferenceCompat.openUrlDialog(context, hyperlink.Id)
                        FBHyperlinkType.INTERNAL, FBHyperlinkType.FOOTNOTE -> {
                            val snippet = app.getFootnoteData(hyperlink.Id)
                            if (snippet != null) {
                                app.Collection.markHyperlinkAsVisited(app.currentBook, hyperlink.Id)
                                val showToast = when (app.MiscOptions.ShowFootnoteToast.getValue()) {
                                    MiscOptions.FootnoteToastEnum.never -> false
                                    MiscOptions.FootnoteToastEnum.footnotesOnly -> hyperlink.Type == FBHyperlinkType.FOOTNOTE
                                    MiscOptions.FootnoteToastEnum.footnotesAndSuperscripts ->
                                        hyperlink.Type == FBHyperlinkType.FOOTNOTE || region.isVerticallyAligned()
                                    else -> true
                                }
                                if (showToast) {
                                    app.BookTextView.outlineRegion(region)
                                    val rootView = findViewById<View>(android.R.id.content)
                                    if (rootView != null) {
                                        if (snippet.IsEndOfText) {
                                            Snackbar.make(rootView, snippet.getText(), Snackbar.LENGTH_SHORT).show()
                                        } else {
                                            Snackbar.make(rootView, snippet.getText(), Snackbar.LENGTH_LONG)
                                                .setAction(ZLResource.resource("toast").getResource("more").getValue()) {
                                                    app.BookTextView.hideOutline()
                                                    showHyperlink(hyperlink)
                                                }
                                                .addCallback(object : Snackbar.Callback() {
                                                    override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                                                        app.BookTextView.hideOutline()
                                                    }
                                                })
                                                .show()
                                        }
                                    }
                                } else {
                                    book!!.info!!.position = position
                                    showHyperlink(hyperlink)
                                }
                            }
                        }
                        else -> {}
                    }
                } else if (soul is ZLTextImageRegionSoul) {
                    val image = soul
                    val anchor = View(context)
                    val lp = RelativeLayout.LayoutParams(region.right - region.left, region.bottom - region.top)
                    lp.leftMargin = region.left
                    lp.topMargin = region.top
                    if (widget is ScrollWidget) {
                        val p = (widget as ScrollWidget).findRegionView(soul)
                        lp.leftMargin += p!!.left
                        lp.topMargin += p.top
                    }
                    this@FBReaderView.addView(anchor, lp)
                    val menu = PopupMenu(context, anchor, Gravity.BOTTOM)
                    menu.inflate(R.menu.image_menu)
                    menu.setOnMenuItemClickListener { item ->
                        val id = item.itemId
                        when (id) {
                            R.id.action_open -> {
                                val name = image.ImageElement.Id
                                val uri = Uri.parse(image.ImageElement.URL)
                                val intent = ImagesProvider.getProvider().openIntent(uri, name)
                                context.startActivity(intent)
                            }
                            R.id.action_share -> {
                                val name = image.ImageElement.Id
                                val type = Storage.getTypeByExt(ImagesProvider.EXT)
                                val uri = Uri.parse(image.ImageElement.URL)
                                val intent = ImagesProvider.getProvider().shareIntent(uri, name, type, Storage.getTitle(book!!.info!!) + " (" + name + ")")
                                context.startActivity(intent)
                            }
                            R.id.action_original -> {
                                (app.BookTextView as CustomView).setScalingType(image.ImageElement, ZLPaintContext.ScalingType.OriginalSize)
                                resetCaches()
                            }
                            R.id.action_zoom -> {
                                (app.BookTextView as CustomView).setScalingType(image.ImageElement, ZLPaintContext.ScalingType.FitMaximum)
                                resetCaches()
                            }
                            R.id.action_original_all -> {
                                book!!.info!!.scales.clear()
                                book!!.info!!.scale = imageFitting("covers")
                                config.setValue(fitToScreenOption(), imageFitting("covers"))
                                resetCaches()
                            }
                            R.id.action_zoom_all -> {
                                book!!.info!!.scales.clear()
                                book!!.info!!.scale = imageFitting("all")
                                config.setValue(fitToScreenOption(), imageFitting("all"))
                                resetCaches()
                            }
                        }
                        true
                    }
                    menu.setOnDismissListener {
                        app.BookTextView.hideOutline()
                        widget!!.repaint()
                        this@FBReaderView.removeView(anchor)
                    }
                    this@FBReaderView.post { menu.show() }
                } else if (soul is ZLTextWordRegionSoul) {
                    DictionaryUtil.openTextInDictionary(
                        a,
                        soul.Word.getString(),
                        true,
                        region.top,
                        region.bottom
                    ) {
                    }
                }
            }
        })
        app.addAction(ActionCode.SHOW_MENU, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                // Toggle fullscreen mode - listener notification is inside toggleFullscreen()
                toggleFullscreen()
            }

            private fun toggleFullscreen(): Boolean {
                if (w != null) {
                    val controller = WindowInsetsControllerCompat(w!!, w!!.decorView)

                    val bgColor = app.BookTextView.backgroundColor.intValue()
                    val bgColorWithAlpha = if ((bgColor and -0x1000000) == 0)
                        (0xFF shl 24) or bgColor
                    else
                        bgColor
                    w!!.decorView.setBackgroundColor(bgColorWithAlpha)

                    Timber.tag("voronin").d("=== Fullscreen Toggle ===")
                    Timber.tag("voronin").d("Current mode: isFullscreenMode=%s", isFullscreenMode)
                    Timber.tag("voronin").d("Widget type: %s", widget!!.javaClass.simpleName)
                    Timber.tag("voronin").d("Book bgColor: 0x%08X", bgColor)
                    Timber.tag("voronin").d("Window bgColor set: 0x%08X", bgColorWithAlpha)
                    Timber.tag("voronin").d("DecorView size: %dx%d", w!!.decorView.width, w!!.decorView.height)
                    Timber.tag("voronin").d("FBReaderView size: %dx%d", width, height)
                    if (widget is ScrollWidget) {
                        val sw = widget as ScrollWidget
                        Timber.tag("voronin").d("ScrollWidget mainAreaHeight: %d", sw.getMainAreaHeight())
                        Timber.tag("voronin").d("ScrollWidget height: %d", sw.height)
                    }

                    if (isFullscreenMode) {
                        Timber.tag("voronin").d("Action: EXIT fullscreen")
                        isFullscreenMode = false
                        listener?.onFullscreenToggle(false)
                        post {
                            controller.show(WindowInsetsCompat.Type.systemBars())
                            updateSelectionAfterFullscreenChange()
                        }
                        return false
                    } else {
                        Timber.tag("voronin").d("Action: ENTER fullscreen")
                        isFullscreenMode = true
                        listener?.onFullscreenToggle(true)
                        post {
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                            controller.systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            updateSelectionAfterFullscreenChange()
                        }
                        return true
                    }
                }
                return false
            }
        })
        app.addAction(ActionCode.SHOW_NAVIGATION, object : FBAction(app) {
            override fun isVisible(): Boolean {
                if (pluginview != null)
                    return true
                val textModel = app.BookTextView.model
                return textModel != null && textModel.getParagraphsNumber() != 0
            }

            override fun run(vararg params: Any?) {
                listener?.onNavigationRequest()
            }
        })
        app.addAction(ActionCode.SELECTION_SHOW_PANEL, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                val view = app.textView
                listener?.onSelectionShow(view.selectionStartY, view.selectionEndY)
            }
        })
        app.addAction(ActionCode.SELECTION_HIDE_PANEL, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                listener?.onSelectionHide()
            }
        })
        app.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                val text: String
                if (selection != null) {
                    text = selection!!.selection.getText()!!
                } else {
                    val snippet = app.BookTextView.selectedSnippet
                    if (snippet == null)
                        return
                    text = snippet.getText()
                }

                app.BookTextView.clearSelection()
                selectionClose()

                val clipboard = context.getSystemService(Application.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.text = text
                UIMessageUtil.showMessageText(a, clipboard.text.toString())

                if (widget is ScrollWidget) {
                    (widget as ScrollWidget).adapter.processInvalidate()
                    (widget as ScrollWidget).adapter.processClear()
                }
            }
        })
        app.addAction(ActionCode.SELECTION_SHARE, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                val text: String
                if (selection != null) {
                    text = selection!!.selection.getText()!!
                } else {
                    val snippet = app.BookTextView.selectedSnippet
                    if (snippet == null)
                        return
                    text = snippet.getText()
                }

                app.BookTextView.clearSelection()
                selectionClose()

                val intent = Intent(Intent.ACTION_SEND)
                intent.type = HttpClient.CONTENTTYPE_TEXT
                intent.putExtra(Intent.EXTRA_SUBJECT, Storage.getTitle(book!!.info!!))
                intent.putExtra(Intent.EXTRA_TEXT, text)
                a.startActivity(Intent.createChooser(intent, null))

                if (widget is ScrollWidget) {
                    this@FBReaderView.post {
                        (widget as ScrollWidget).adapter.processInvalidate()
                        (widget as ScrollWidget).adapter.processClear()
                    }
                }
            }
        })
        app.addAction(ActionCode.SELECTION_BOOKMARK, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                if (params.isNotEmpty()) {
                    val bm = params[0] as Storage.Bookmark
                    listener?.onEditBookmark(bm)
                } else {
                    if (book!!.info!!.bookmarks == null)
                        book!!.info!!.bookmarks = Storage.Bookmarks()
                    val bm: Storage.Bookmark
                    if (selection != null) {
                        bm = Storage.Bookmark(selection!!.selection.getText()!!, selection!!.selection.getStart()!!, selection!!.selection.getEnd()!!)
                    } else {
                        val snippet = app.BookTextView.selectedSnippet!!
                        bm = Storage.Bookmark(snippet.getText(), snippet.getStart(), snippet.getEnd())
                    }

                    app.BookTextView.clearSelection()
                    selectionClose()

                    bm.coverUrl = book!!.info!!.coverUrl
                    bm.bookFileUri = book!!.info!!.bookFileUri

                    val context = extractSentenceContext(bm)

                    if (context != null) {
                        bm.sentenceBefore = context.first
                        bm.sentenceAfter = context.second
                    }

                    book!!.info!!.bookmarks!!.add(bm)
                    bookmarksUpdate()
                    listener?.onBookmarksUpdate()
                }
            }
        })
        app.addAction(ActionCode.SELECTION_CLEAR, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                app.BookTextView.clearSelection()
                selectionClose()
                if (widget is ScrollWidget) {
                    (widget as ScrollWidget).adapter.processInvalidate()
                    (widget as ScrollWidget).adapter.processClear()
                }
            }
        })

        app.addAction(ActionCode.FIND_PREVIOUS, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                if (search != null) {
                    val run = Runnable {
                        val page = search!!.prev()
                        if (page == -1)
                            return@Runnable
                        this@FBReaderView.post {
                            if (widget is ScrollWidget) {
                                (widget as ScrollWidget).searchPage(page)
                                return@post
                            }
                            if (widget is PagerWidget) {
                                (widget as PagerWidget).searchPage(page)
                            }
                        }
                    }
                    UIUtil.wait("search", run, context)
                    return
                } else {
                    app.BookTextView.findPrevious()
                }
                resetNewPosition()
            }
        })
        app.addAction(ActionCode.FIND_NEXT, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                if (search != null) {
                    val run = Runnable {
                        val page = search!!.next()
                        if (page == -1)
                            return@Runnable
                        this@FBReaderView.post {
                            if (widget is ScrollWidget) {
                                (widget as ScrollWidget).searchPage(page)
                                return@post
                            }
                            if (widget is PagerWidget) {
                                (widget as PagerWidget).searchPage(page)
                            }
                        }
                    }
                    UIUtil.wait("search", run, context)
                    return
                } else {
                    app.BookTextView.findNext()
                }
                resetNewPosition()
            }
        })
        app.addAction(ActionCode.CLEAR_FIND_RESULTS, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                listener?.onSearchClose()
                searchClose()
                app.BookTextView.clearFindResults()
                resetNewPosition()
            }
        })

        app.addAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                scrollNextPage()
            }
        })
        app.addAction(ActionCode.VOLUME_KEY_SCROLL_BACK, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                scrollPrevPage()
            }
        })
        app.addAction(ActionCode.ASK_QUESTION, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                selectionSVAction(onBookPagerManager::askQuestion)
            }
        })
        app.addAction(ActionCode.TEL_ABOUT_MISSPELL, object : FBAction(app) {
            override fun run(vararg params: Any?) {
                selectionSVAction(onBookPagerManager::tellAboutMisspell)
            }
        })
    }

    private fun selectionSVAction(action: CustomAction) {
        val snippet = app.BookTextView.selectedSnippet

        val text: String
        val pageIndex: Int
        if (selection != null) {
            text = selection!!.selection.getText()!!
            pageIndex = selection!!.selection.getStart()!!.paragraphIndex
        } else {
            if (snippet == null)
                return
            text = snippet.getText()
            pageIndex = snippet.getStart().paragraphIndex
        }

        app.BookTextView.clearSelection()
        selectionClose()

        val title = book?.info?.title ?: ""
        val authors = book?.info?.authors ?: ""

        action.action(
            context,
            text,
            title,
            authors,
            pageIndex
        )
    }

    fun scrollNextPage() {
        val preferences = app.PageTurningOptions
        widget!!.startAnimatedScrolling(
            ZLViewEnums.PageIndex.next,
            if (preferences.horizontal.getValue()) ZLViewEnums.Direction.rightToLeft else ZLViewEnums.Direction.up,
            preferences.animationSpeed.getValue()
        )
    }

    fun scrollPrevPage() {
        val preferences = app.PageTurningOptions
        widget!!.startAnimatedScrolling(
            ZLViewEnums.PageIndex.previous,
            if (preferences.horizontal.getValue()) ZLViewEnums.Direction.rightToLeft else ZLViewEnums.Direction.up,
            preferences.animationSpeed.getValue()
        )
    }

    fun setDrawer(drawer: DrawerLayout) {
        this.drawer = drawer
    }

    fun showHyperlink(hyperlink: ZLTextHyperlink) {
        val label = app.Model!!.getLabel(hyperlink.Id!!)
        showHyperlink(label!!)
    }

    fun showHyperlink(label: BookModel.Label) {
        val context = context

        val ll = LinearLayout(context)
        ll.orientation = LinearLayout.VERTICAL

        val f = WallpaperLayout(context)
        val c = ImageButton(context)
        c.setImageResource(com.github.axet.androidlibrary.R.drawable.ic_close_black_24dp)
        c.setColorFilter(ThemeUtils.getThemeColor(context, com.github.axet.androidlibrary.R.attr.colorAccent))
        f.addView(c, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT or Gravity.TOP))

        val r = object : FBReaderView(context) {
            override fun configInit() {
                super.configInit()
                config.setValue(app.ViewOptions.ScrollbarType, 0)
                config.setValue(app.MiscOptions.WordTappingAction, MiscOptions.WordTappingActionEnum.doNothing)
                config.setValue(app.ImageOptions.TapAction, ImageOptions.TapActionEnum.doNothing)
            }
        }

        r.app.addAction(ActionCode.PROCESS_HYPERLINK, object : FBAction(r.app) {
            override fun run(vararg params: Any?) {
                if (r.pluginview != null) {
                    val l = params[0] as BookModel.Label
                    r.app.BookTextView.gotoPosition(l.ParagraphIndex, 0, 0)
                    r.resetNewPosition()
                    return
                }
                val region = r.app.BookTextView.outlinedRegion
                if (region == null) {
                    return
                }
                val soul = region.soul
                if (soul is ZLTextHyperlinkRegionSoul) {
                    r.app.BookTextView.hideOutline()
                    r.widget!!.repaint()
                    val hyperlink = soul.Hyperlink
                    when (hyperlink.Type) {
                        FBHyperlinkType.EXTERNAL ->
                            AboutPreferenceCompat.openUrlDialog(context, hyperlink.Id)
                        FBHyperlinkType.INTERNAL, FBHyperlinkType.FOOTNOTE -> {
                            val label = r.app.Model!!.getLabel(hyperlink.Id!!)
                            r.app.BookTextView.gotoPosition(label!!.ParagraphIndex, 0, 0)
                            r.resetNewPosition()
                        }
                        else -> {}
                    }
                }
            }
        })

        val shared = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
        r.configWidget(shared)

        val rlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        ll.addView(f, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        ll.addView(r, rlp)

        val builder = AlertDialog.Builder(context)
        builder.setView(ll)
        builder.setOnDismissListener { listener?.onDismissDialog() }
        if (label.ModelId == null) {
            builder.setNeutralButton(R.string.sv_keep_reading_position) { _, _ -> gotoPosition(r.position) }
        }
        builder.setPositiveButton(com.github.axet.androidlibrary.R.string.close) { _, _ ->
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            val w = dialog.window
            w!!.setLayout(width, height) // fixed size after creation
            r.loadBook(book!!)
            if (label.ModelId == null) {
                r.app.BookTextView.gotoPosition(label.ParagraphIndex, 0, 0)
                r.app.setView(r.app.BookTextView)
            } else {
                val model = r.app.Model!!.getFootnoteModel(label.ModelId!!)
                r.app.BookTextView.setModel(model)
                r.app.setView(r.app.BookTextView)
                r.app.BookTextView.gotoPosition(label.ParagraphIndex, 0, 0)
            }
        }
        c.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    fun gotoPluginPosition(p: ZLTextPosition?) {
        if (p == null)
            return
        var pos = p
        if (widget is ScrollWidget) {
            if (pos.elementIndex != 0 || scrollCentered) {
                scrollDelayed = pos
                pos = ZLTextFixedPosition(pos.paragraphIndex, 0, 0)
            }
        }
        pluginview!!.gotoPosition(pos)
    }

    fun gotoPosition(p: TOCTree.Reference) {
        if (p.Model != null)
            app.BookTextView.setModel(p.Model)
        gotoPosition(ZLTextFixedPosition(p.ParagraphIndex, 0, 0))
    }

    fun gotoPosition(p: ZLTextPosition) {
        scrollCentered = false
        if (pluginview != null)
            gotoPluginPosition(p)
        else
            app.BookTextView.gotoPosition(p)
        resetNewPosition()
    }

    fun gotoPositionCentered(p: ZLTextPosition) {
        scrollCentered = true
        if (pluginview != null)
            gotoPluginPosition(p)
        else
            app.BookTextView.gotoPosition(p)
        resetNewPosition()
    }

    fun resetNewPosition() {
        if (widget is ScrollWidget) {
            (widget as ScrollWidget).adapter.reset()
        } else {
            widget!!.reset()
            widget!!.repaint()
        }
    }

    fun reset() {
        if (widget is ScrollWidget) {
            (widget as ScrollWidget).updatePosition()
            (widget as ScrollWidget).adapter.reset()
            if (pluginview != null)
                (widget as ScrollWidget).updateOverlays()
        } else {
            widget!!.reset()
            widget!!.repaint()
        }
    }

    fun updateTheme() {
        if (pluginview != null)
            pluginview!!.updateTheme()
        if (widget is ScrollWidget) {
            (widget as ScrollWidget).requestLayout() // repaint views
            widget!!.reset()
        } else {
            widget!!.reset()
            widget!!.repaint()
        }
    }

    fun resetCaches() {
        app.clearTextCaches()
        reset()
    }

    fun invalidateFooter() {
        if (footer == null) {
            if (widget is ScrollWidget)
                (widget as ScrollWidget).invalidate()
            else
                widget!!.repaint()
        } else {
            footer!!.invalidate()
        }
    }

    fun clearReflowPage() {
        pluginview!!.current!!.pageOffset = 0
        if (pluginview!!.reflower != null)
            pluginview!!.reflower!!.index = 0
    }

    fun selectionOpen(s: PluginView.Selection) {
        if (selection != null) {
            selectionCloseInternal()
        }

        val callbacks = object : SelectionCallbacks {
            override fun onDragStart(handle: HandleType) {
                if (drawer != null)
                    drawer!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                app.runAction(ActionCode.SELECTION_HIDE_PANEL)
            }

            override fun onDragEnd(handle: HandleType) {
                if (drawer != null)
                    drawer!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                app.runAction(ActionCode.SELECTION_SHOW_PANEL)
            }

            override fun onBoundsChanged(startBounds: Rect, endBounds: Rect) {
            }
        }

        val sv = SelectionView(context, app.BookTextView as CustomView, s, callbacks)
        selection = sv
        addView(sv)
        if (widget is ScrollWidget) {
            (widget as ScrollWidget).updateOverlays()
            sv.setClipHeight((widget as ScrollWidget).getMainAreaHeight())
        } else {
            sv.setClipHeight((widget as ZLAndroidWidget).getMainAreaHeight())
        }
        app.runAction(ActionCode.SELECTION_SHOW_PANEL)
    }

    private fun selectionCloseInternal() {
        if (widget is ScrollWidget)
            (widget as ScrollWidget).selectionClose()
        if (selection != null) {
            selection!!.close()
            removeView(selection)
            selection = null
        }
    }

    fun selectionClose() {
        selectionCloseInternal()
        app.runAction(ActionCode.SELECTION_HIDE_PANEL)
    }

    private fun updateSelectionAfterFullscreenChange() {
        if (selection == null || widget == null) {
            Timber.tag("voronin").d("updateSelectionAfterFullscreenChange: no selection or widget, skipping")
            return
        }

        Timber.tag("voronin").d("updateSelectionAfterFullscreenChange: updating selection coordinates for fullscreen change")

        if (widget is ScrollWidget) {
            selection!!.setClipHeight((widget as ScrollWidget).getMainAreaHeight())
        } else if (widget is PagerWidget) {
            selection!!.setClipHeight((widget as PagerWidget).getMainAreaHeight())
        }

        if (widget is PagerWidget) {
            (widget as PagerWidget).updateOverlays()
        }

        if (selection!!.childCount > 0) {
            try {
                selection!!.update()
                Timber.tag("voronin").d("updateSelectionAfterFullscreenChange: selection updated successfully")
            } catch (e: Exception) {
                Timber.tag("voronin").e(e, "updateSelectionAfterFullscreenChange: error updating selection")
            }
        }
    }

    fun linksClose() {
        if (widget is ScrollWidget)
            (widget as ScrollWidget).linksClose()
        if (widget is PagerWidget)
            (widget as PagerWidget).linksClose()
    }

    fun bookmarksClose() {
        if (widget is ScrollWidget)
            (widget as ScrollWidget).bookmarksClose()
        if (widget is PagerWidget)
            (widget as PagerWidget).bookmarksClose()
    }

    fun bookmarksUpdate() {
        if (pluginview == null) {
            app.BookTextView.removeHighlightings(ZLBookmark::class.java)
            val hi = ArrayList<ZLTextHighlighting>()
            if (book!!.info!!.bookmarks != null) {
                for (b in book!!.info!!.bookmarks!!) {
                    val h = ZLBookmark(app.BookTextView, b)
                    hi.add(h)
                }
            }
            app.BookTextView.addHighlightings(hi)
        }
        if (widget is ScrollWidget) {
            if (pluginview == null) {
                for (h in (widget as ScrollWidget).adapter.holders) {
                    h.page.recycle()
                    h.page.invalidate()
                }
            } else {
                (widget as ScrollWidget).bookmarksUpdate()
            }
        }
        if (widget is PagerWidget)
            (widget as PagerWidget).updateOverlaysReset()
    }

    fun ttsClose() {
        if (widget is ScrollWidget)
            (widget as ScrollWidget).ttsClose()
        if (widget is PagerWidget)
            (widget as PagerWidget).ttsClose()
        if (pluginview == null) {
            app.BookTextView.removeHighlightings(ZLTTSMark::class.java)
            if (widget is ScrollWidget) {
                for (h in (widget as ScrollWidget).adapter.holders) {
                    h.page.recycle()
                    h.page.invalidate()
                }
            }
        }
    }

    fun ttsUpdate() {
        if (tts == null)
            return // already closed, run from handler
        if (pluginview == null) {
            app.BookTextView.removeHighlightings(ZLTTSMark::class.java)
            if (tts != null) {
                val hi = ArrayList<ZLTextHighlighting>()
                for (m in tts!!.marks) {
                    val h = ZLTTSMark(app.BookTextView, m)
                    hi.add(h)
                }
                app.BookTextView.addHighlightings(hi)
            }
        }
        if (widget is ScrollWidget) {
            if (pluginview == null) {
                for (h in (widget as ScrollWidget).adapter.holders) {
                    h.page.recycle()
                    h.page.invalidate()
                }
            } else {
                (widget as ScrollWidget).ttsUpdate()
            }
        }
        if (widget is PagerWidget)
            (widget as PagerWidget).updateOverlaysReset()
        tts!!.view.bringToFront()
    }

    fun ttsOpen() {
        tts = TTSPopup(this)
        tts!!.show()
        ttsUpdate()
    }

    fun searchClose() {
        app.hideActivePopup()
        if (widget is ScrollWidget)
            (widget as ScrollWidget).searchClose()
        if (widget is PagerWidget)
            (widget as PagerWidget).searchClose()
        if (search != null) {
            search!!.close()
            search = null
        }
        searchPagePending = -1
        searchCurrentIndex = 0
        searchTotalCount = 0
    }

    fun performSearch(pattern: String, callback: SearchCallback) {
        if (pattern.isEmpty()) {
            callback.onResult(0, 0)
            return
        }

        app.hideActivePopup()
        config.setValue(app.MiscOptions.TextSearchPattern, pattern)

        if (pluginview != null) {
            searchClose()
            search = pluginview!!.search(pattern)
            search!!.setPage(position.paragraphIndex)
            val count = search!!.getCount()
            searchCurrentIndex = if (count > 0) Math.max(search!!.getIndex(), 0) else 0
            Timber.tag("voronin").d("performSearch: pattern=%s, count=%d, currentPage=%d, currentIndex=%d", pattern, count, position.paragraphIndex, searchCurrentIndex)
            if (count > 0) {
                if (widget is ScrollWidget)
                    (widget as ScrollWidget).updateOverlays()
                if (widget is PagerWidget)
                    (widget as PagerWidget).updateOverlaysReset()
            }
            callback.onResult(count, searchCurrentIndex)
        } else {
            val count = app.BookTextView.search(pattern, true, false, false, false)
            searchTotalCount = count
            searchCurrentIndex = if (count > 0) Math.max(app.BookTextView.searchMarkIndex, 0) else 0
            if (count > 0 && widget is ScrollWidget) {
                post { reset() }
            }
            callback.onResult(count, searchCurrentIndex)
        }
    }

    fun performSearchNext(callback: SearchCallback) {
        hideKeyboard()
        if (search != null) {
            val count = search!!.getCount()
            val page = search!!.next()
            if (page != -1 && searchCurrentIndex < count - 1) {
                searchCurrentIndex++
                post {
                    if (widget is ScrollWidget) {
                        (widget as ScrollWidget).searchPage(page)
                    } else if (widget is PagerWidget) {
                        (widget as PagerWidget).searchPage(page)
                        (widget as PagerWidget).updateOverlaysReset()
                    }
                }
            }
            callback.onResult(count, searchCurrentIndex)
        } else {
            app.BookTextView.findNext()
            searchCurrentIndex = Math.max(app.BookTextView.searchMarkIndex, 0)
            callback.onResult(searchTotalCount, searchCurrentIndex)
        }
    }

    fun performSearchPrevious(callback: SearchCallback) {
        hideKeyboard()
        if (search != null) {
            val count = search!!.getCount()
            val page = search!!.prev()
            if (page != -1 && searchCurrentIndex > 0) {
                searchCurrentIndex--
                post {
                    if (widget is ScrollWidget) {
                        (widget as ScrollWidget).searchPage(page)
                    } else if (widget is PagerWidget) {
                        (widget as PagerWidget).searchPage(page)
                        (widget as PagerWidget).updateOverlaysReset()
                    }
                }
            }
            callback.onResult(count, searchCurrentIndex)
        } else {
            app.BookTextView.findPrevious()
            searchCurrentIndex = Math.max(app.BookTextView.searchMarkIndex, 0)
            callback.onResult(searchTotalCount, searchCurrentIndex)
        }
    }

    private fun hideKeyboard() {
        if (context is Activity) {
            val activity = context as Activity
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    fun overlaysClose() {
        selectionClose()
        linksClose()
        bookmarksClose()
        searchClose()
    }

    fun isZoom(): Boolean {
        if (widget is ScrollWidget)
            return (widget as ScrollWidget).gesturesListener.zoomHandler.isInZoom
        if (widget is PagerWidget)
            return (widget as PagerWidget).getZoomHandler().isInZoom
        return false
    }

    fun resetZoom() {
        if (widget is ScrollWidget)
            (widget as ScrollWidget).gesturesListener.zoomHandler.resetZoom()
        if (widget is PagerWidget)
            (widget as PagerWidget).getZoomHandler().resetZoom()
    }

    fun getZoomScale(): Float {
        if (widget is ScrollWidget)
            return (widget as ScrollWidget).gesturesListener.zoomHandler.currentZoom
        if (widget is PagerWidget)
            return (widget as PagerWidget).getZoomHandler().currentZoom
        return 1.0f
    }

    fun getZoomTouchAdapter(): ZoomTouchAdapter {
        return ZoomTouchAdapter(this)
    }

    fun showControls() {
        val areas = ActiveAreasView(context)
        val w = width
        if (w == 0)
            return // activity closed
        areas.create(app, w)
        showControls(this, areas)
    }

    fun exitFullscreen() {
        if (w != null && isFullscreenMode) {
            val controller = WindowInsetsControllerCompat(w!!, w!!.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
            isFullscreenMode = false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val scrollDelayed = position
        reset()
        gotoPosition(scrollDelayed)
    }

    val isReflow: Boolean
        get() = pluginview!!.reflow

    fun setReflow(b: Boolean) {
        pluginview!!.reflow = b
        val scrollDelayed = position
        reset()
        gotoPosition(scrollDelayed)
    }

    fun canChangeFont(): Boolean {
        if (pluginview == null) {
            return true
        }
        return pluginview!!.reflow
    }

    fun getFontsizeFB(): Int {
        val dpiValue: Int
        if (book!!.info!!.fontsize != null)
            dpiValue = book!!.info!!.fontsize!!
        else
            dpiValue = app.ViewOptions.textStyleCollection.baseStyle.FontSizeOption.getValue()
        return dpiValue * 160 / ZLibrary.Instance().getDisplayDPI()
    }

    fun setFontsizeFB(p: Int) {
        val dpiScaled = p * ZLibrary.Instance().getDisplayDPI() / 160
        book!!.info!!.fontsize = dpiScaled
        config.setValue(app.ViewOptions.textStyleCollection.baseStyle.FontSizeOption, dpiScaled)
        val scrollDelayed = position
        resetCaches()
        gotoPosition(scrollDelayed)
    }

    fun getIgnoreCssFonts(): Boolean {
        return !app.ViewOptions.textStyleCollection.baseStyle.UseCSSFontFamilyOption.getValue()
    }

    fun setIgnoreCssFonts(b: Boolean) {
        config.setValue(app.ViewOptions.textStyleCollection.baseStyle.UseCSSFontFamilyOption, !b)
        val scrollDelayed = position
        resetCaches()
        gotoPosition(scrollDelayed)
    }

    fun setFontFB(f: String) {
        config.setValue(app.ViewOptions.textStyleCollection.baseStyle.FontFamilyOption, f)
        val scrollDelayed = position
        resetCaches()
        gotoPosition(scrollDelayed)
    }

    fun getFontsizeReflow(): Float? {
        if (book!!.info!!.fontsize != null)
            return book!!.info!!.fontsize!! / 100f
        return null
    }

    fun setFontsizeReflow(p: Float) {
        book!!.info!!.fontsize = (p * 100f).toInt()
        if (pluginview!!.reflower != null && pluginview!!.reflower!!.k2 != null)
            pluginview!!.reflower!!.k2!!.setFontSize(p)
        val scrollDelayed = position
        reset()
        gotoPosition(scrollDelayed)
    }

    fun onScrollingFinished(pageIndex: ZLViewEnums.PageIndex) {
        listener?.onScrollingFinished(pageIndex)
        tts?.onScrollingFinished(pageIndex)
    }

    fun extractSentenceContext(bookmark: Storage.Bookmark): Pair<String?, String?>? {
        if (app.BookTextView == null) {
            android.util.Log.w("voronin2", "extractSentenceContext: app or BookTextView is null")
            return null
        }

        if (pluginview != null) {
            return extractSentenceContextPDF(bookmark)
        }

        try {
            val view = app.BookTextView
            val model = view.model
            if (model == null) {
                android.util.Log.w("voronin2", "extractSentenceContext: model is null")
                return null
            }

            val start: ZLTextPosition = bookmark.start!!
            val end: ZLTextPosition = bookmark.end!!

            val contextParagraphs = 3
            val startParagraph = Math.max(0, start.paragraphIndex - contextParagraphs)
            val endParagraph = Math.min(model.getParagraphsNumber() - 1, end.paragraphIndex + contextParagraphs)

            val fullText = StringBuilder()
            for (i in startParagraph..endParagraph) {
                try {
                    val paragraph = model.getParagraph(i)
                    if (paragraph == null) {
                        android.util.Log.d("voronin", "Paragraph $i is null")
                        continue
                    }

                    val iterator = paragraph.iterator()
                    if (iterator == null) {
                        android.util.Log.d("voronin", "Paragraph $i iterator is null (PDF page?)")
                        continue
                    }

                    while (iterator.next()) {
                        if (iterator.getType() == ZLTextParagraph.Entry.TEXT) {
                            val data = iterator.getTextData()
                            val offset = iterator.getTextOffset()
                            val length = iterator.getTextLength()
                            fullText.append(data, offset, length)
                        }
                    }
                    fullText.append(" ")
                } catch (e: Exception) {
                    android.util.Log.e("voronin", "Error extracting paragraph $i", e)
                }
            }

            val text = normalizeExtractedText(fullText.toString().trim())
            if (text.isEmpty()) {
                android.util.Log.w("voronin", "extractSentenceContext: text is empty")
                return null
            }

            var noteText = normalizeExtractedText(bookmark.text!!)
            var noteIndex = text.indexOf(noteText)

            if (noteIndex == -1) {
                noteIndex = text.lowercase().indexOf(noteText.lowercase())
                if (noteIndex == -1) {
                    android.util.Log.w("voronin", "extractSentenceContext: note text not found in extracted text")
                    return null
                }
                noteText = text.substring(noteIndex, Math.min(noteIndex + noteText.length, text.length))
            }

            bookmark.text = noteText

            val sentenceStart = findSentenceStart(text, noteIndex)
            val sentenceEnd = findSentenceEnd(text, noteIndex + noteText.length)

            val before = text.substring(sentenceStart, noteIndex).trim()
            val after = text.substring(noteIndex + noteText.length, sentenceEnd).trim()

            return Pair(
                if (before.isEmpty()) null else before,
                if (after.isEmpty()) null else after
            )
        } catch (e: Exception) {
            android.util.Log.e("FBReaderView", "Error extracting sentence context", e)
            return null
        }
    }

    private fun extractSentenceContextPDF(bookmark: Storage.Bookmark): Pair<String?, String?>? {
        try {
            val pageNum = bookmark.start!!.paragraphIndex

            if (pluginview == null) {
                android.util.Log.w("voronin2", "pluginview is null")
                return null
            }

            var pageText = pluginview!!.getPageText(pageNum)

            if (pageText == null || pageText.isEmpty()) {
                android.util.Log.w("voronin2", "Failed to get page text or page is empty")
                return null
            }

            pageText = normalizeExtractedText(pageText)

            var noteText = bookmark.text
            if (noteText == null || noteText.isEmpty()) {
                android.util.Log.w("voronin2", "Bookmark text is null or empty")
                return null
            }

            noteText = normalizeExtractedText(noteText)

            var noteIndex = pageText.indexOf(noteText)

            if (noteIndex == -1) {
                noteIndex = pageText.lowercase().indexOf(noteText.lowercase())
                if (noteIndex == -1) {
                    android.util.Log.w("voronin2", "Note text not found in page text")
                    return null
                }
                noteText = pageText.substring(noteIndex, Math.min(noteIndex + noteText.length, pageText.length))
            }

            bookmark.text = noteText

            val sentenceStart = findSentenceStart(pageText, noteIndex)
            val sentenceEnd = findSentenceEnd(pageText, noteIndex + noteText.length)

            val before = pageText.substring(sentenceStart, noteIndex).trim()
            val after = pageText.substring(noteIndex + noteText.length, sentenceEnd).trim()

            val beforeClean = if (before.isEmpty()) null else cleanBookmarkText(before)
            val afterClean = if (after.isEmpty()) null else cleanBookmarkText(after)

            return Pair(beforeClean, afterClean)
        } catch (e: Exception) {
            android.util.Log.e("voronin2", "Error extracting PDF sentence context", e)
            return null
        }
    }

    private fun findSentenceStart(text: String, fromIndex: Int): Int {
        var index = fromIndex - 1
        while (index >= 0) {
            val c = text[index]
            if (c == '.' || c == '!' || c == '?') {
                return index + 1
            }
            index--
        }
        return 0
    }

    private fun findSentenceEnd(text: String, fromIndex: Int): Int {
        var index = fromIndex
        while (index < text.length) {
            val c = text[index]
            if (c == '.' || c == '!' || c == '?') {
                return index + 1
            }
            index++
        }
        return text.length
    }

    private fun normalizeExtractedText(text: String): String {
        val dehyphenated = text.replace(Regex("(\\p{L})-\\s+(\\p{Ll})"), "\$1\$2")
        val flattened = dehyphenated.replace(Regex("\\s*\\n\\s*"), " ")
        return flattened.replace(Regex(" {2,}"), " ")
    }

    enum class Widgets { PAGING, CONTINUOUS }

    private fun interface CustomAction {
        fun action(
            context: Context,
            selectionText: String,
            title: String?,
            author: String?,
            page: Int
        )
    }

    fun interface SearchCallback {
        fun onResult(count: Int, currentIndex: Int)
    }

    interface Listener {
        fun onScrollingFinished(index: ZLViewEnums.PageIndex?)

        fun onSearchClose()

        fun onBookmarksUpdate()

        fun onDismissDialog()

        fun ttsStatus(speaking: Boolean)

        fun onEditBookmark(bookmark: Storage.Bookmark)

        fun onFullscreenToggle(isFullscreen: Boolean)

        fun onNavigationRequest()

        fun onSelectionShow(startY: Int, endY: Int)

        fun onSelectionHide()

        fun onZoomChange(scale: Float, pivotX: Float, pivotY: Float)

        fun onZoomEnd()
    }

    class ZLTextIndexPosition : com.github.axet.bookreader.widgets.ZLTextIndexPosition {
        constructor(p: ZLTextPosition?, e: ZLTextPosition?) : super(p!!, e!!)
        constructor(inParcel: Parcel) : super(inParcel)

        companion object CREATOR : Parcelable.Creator<ZLTextIndexPosition> {
            override fun createFromParcel(source: Parcel): ZLTextIndexPosition {
                return ZLTextIndexPosition(source)
            }

            override fun newArray(size: Int): Array<ZLTextIndexPosition?> {
                return arrayOfNulls(size)
            }
        }
    }

    class ZLBookmark(view: FBView, b: Storage.Bookmark) : com.github.axet.bookreader.widgets.ZLBookmark(view, b)

    class ZLTTSMark(view: FBView, m: Storage.Bookmark) : com.github.axet.bookreader.widgets.ZLTTSMark(view, m)

    class ConfigShadow : Config() {
        val map: MutableMap<String, String> = TreeMap()

        fun setValue(opt: ZLOption, i: Int) {
            apply(opt)
            setValue(opt.myId, i.toString())
        }

        fun setValue(opt: ZLOption, b: Boolean) {
            apply(opt)
            setValue(opt.myId, b.toString())
        }

        fun setValue(opt: ZLOption, v: Enum<*>) {
            apply(opt)
            setValue(opt.myId, v.toString())
        }

        fun setValue(opt: ZLOption, v: String) {
            apply(opt)
            setValue(opt.myId, v)
        }

        fun apply(opt: ZLOption) {
            opt.Config = object : ZLOption.ConfigInstance() {
                override fun Instance(): Config = this@ConfigShadow
            }
        }

        override fun getValue(id: StringPair, defaultValue: String): String {
            val v = map[id.Group + ":" + id.Name]
            if (v != null)
                return v
            return super.getValue(id, defaultValue)
        }

        override fun setValue(id: StringPair, value: String) {
            map[id.Group + ":" + id.Name] = value
        }

        override fun unsetValue(id: StringPair) {
            map.remove(id.Group + ":" + id.Name)
        }

        override fun setValueInternal(group: String, name: String, value: String) {
        }

        override fun unsetValueInternal(group: String, name: String) {
        }

        override fun requestAllValuesForGroupInternal(group: String): Map<String, String>? = null

        override fun isInitialized(): Boolean = true

        override fun runOnConnect(runnable: Runnable) {
        }

        override fun listGroups(): List<String>? = null

        override fun listNames(group: String): List<String>? = null

        override fun removeGroup(name: String) {
        }

        override fun getSpecialBooleanValue(name: String, defaultValue: Boolean): Boolean = false

        override fun setSpecialBooleanValue(name: String, value: Boolean) {
        }

        override fun getSpecialStringValue(name: String, defaultValue: String): String? = null

        override fun setSpecialStringValue(name: String, value: String) {
        }

        override fun getValueInternal(group: String, name: String): String {
            throw Config.NotAvailableException("default")
        }
    }

    class BrightnessGesture(view: FBReaderView) : com.github.axet.bookreader.widgets.BrightnessGesture(view)

    class LinksView(view: FBReaderView, ll: Array<PluginView.Link>?, info: Reflow.Info?) {
        val links: ArrayList<View> = ArrayList()
        var fb: FBReaderView = view

        init {
            run build@{
                if (ll == null)
                    return@build
                for (l in ll) {
                    val rr: Array<Rect>
                    if (fb.pluginview!!.reflow) {
                        rr = fb.pluginview!!.boundsUpdate(arrayOf(l.rect!!), info!!)
                        if (rr.isEmpty())
                            continue
                    } else {
                        rr = arrayOf(l.rect!!)
                    }
                    for (r in rr) {
                        val lp = ViewGroup.MarginLayoutParams(r.width(), r.height())
                        val v = View(fb.context)
                        v.layoutParams = lp
                        v.tag = r
                        v.setOnClickListener {
                            if (l.index != -1)
                                fb.app.runAction(ActionCode.PROCESS_HYPERLINK, BookModel.Label(null, l.index))
                            else
                                AboutPreferenceCompat.openUrlDialog(fb.context, l.url)
                        }
                        links.add(v)
                        fb.addView(v)
                    }
                }
            }
        }

        fun update(x: Int, y: Int) {
            for (v in links) {
                val l = v.tag as Rect
                val lp = v.layoutParams as ViewGroup.MarginLayoutParams
                lp.leftMargin = x + l.left
                lp.topMargin = y + l.top
                lp.width = l.width()
                lp.height = l.height()
                v.requestLayout()
            }
        }

        fun hide() {
            for (v in links)
                v.visibility = View.GONE
        }

        fun show() {
            for (v in links)
                v.visibility = View.VISIBLE
        }

        fun close() {
            val old = ArrayList(links)
            // can be called during RelativeLayout onLayout
            fb.post {
                for (v in old)
                    fb.removeView(v)
            }
            links.clear()
        }
    }

    open class BookmarksView(view: FBReaderView, page: PluginView.Selection.Page, bms: Storage.Bookmarks?, info: Reflow.Info?) {
        val bookmarks: ArrayList<View> = ArrayList()
        var fb: FBReaderView = view
        var clip: Int = 0

        init {
            clip = if (fb.widget is ScrollWidget)
                (fb.widget as ScrollWidget).getMainAreaHeight()
            else
                (fb.widget as ZLAndroidWidget).getMainAreaHeight()
            run build@{
                if (bms == null)
                    return@build
                val ll = bms.getBookmarks(page)
                if (ll == null)
                    return@build
                for (l in ll) {
                    val s = fb.pluginview!!.select(l.start!!, l.end!!)
                    if (s == null)
                        return@build
                    val bb = s.getBounds(page)
                    s.close()
                    val rr: Array<Rect>
                    if (fb.pluginview!!.reflow)
                        rr = fb.pluginview!!.boundsUpdate(bb!!.rr!!, info!!)
                    else
                        rr = bb!!.rr!!
                    val kk = SelectionView.lines(rr)
                    for (r in kk) {
                        val lp = ViewGroup.MarginLayoutParams(r.width(), r.height())
                        val v = WordView(fb.context)
                        v.layoutParams = lp
                        v.tag = r
                        v.setOnClickListener {
                            fb.listener?.onEditBookmark(l)
                        }
                        val color = if (l.color == 0) fb.app.BookTextView.highlightingBackgroundColor.intValue() else l.color
                        v.setBackgroundColor(SelectionView.SELECTION_ALPHA shl 24 or (color and 0xffffff))
                        addView(v)
                    }
                }
            }
        }

        open fun addView(v: View) {
            bookmarks.add(v)
            fb.addView(v)
        }

        fun update(x: Int, y: Int) {
            for (v in bookmarks) {
                val l = v.tag as Rect
                val lp = v.layoutParams as ViewGroup.MarginLayoutParams
                lp.leftMargin = x + l.left
                lp.topMargin = y + l.top
                lp.width = l.width()
                lp.height = l.height()
                v.requestLayout()
            }
        }

        fun hide() {
            for (v in bookmarks)
                v.visibility = View.GONE
        }

        fun show() {
            for (v in bookmarks)
                v.visibility = View.VISIBLE
        }

        fun close() {
            val old = ArrayList(bookmarks) // can be called during RelativeLayout onLayout
            fb.post {
                for (v in old)
                    fb.removeView(v)
            }
            bookmarks.clear()
        }

        inner class WordView(context: Context) : View(context) {
            override fun draw(canvas: Canvas) {
                val c = canvas.clipBounds
                c.bottom = clip - top
                canvas.clipRect(c)
                super.draw(canvas)
            }
        }
    }

    class TTSView(view: FBReaderView, page: PluginView.Selection.Page, info: Reflow.Info?) : BookmarksView(view, page, view.tts!!.marks, info) {
        override fun addView(v: View) {
            super.addView(v)
            v.setOnClickListener(null)
        }
    }

    class SearchView(view: FBReaderView, bb: PluginView.Search.Bounds?, info: Reflow.Info?) {
        val words: ArrayList<View> = ArrayList()
        var fb: FBReaderView = view
        var padding: Int = 0
        var clip: Int = 0

        init {
            clip = if (fb.widget is ScrollWidget)
                (fb.widget as ScrollWidget).getMainAreaHeight()
            else
                (fb.widget as ZLAndroidWidget).getMainAreaHeight()
            padding = ThemeUtils.dp2px(fb.context, SelectionView.SELECTION_PADDING.toFloat())
            run build@{
                if (bb == null || bb.rr == null)
                    return@build
                if (fb.pluginview!!.reflow)
                    bb.rr = fb.pluginview!!.boundsUpdate(bb.rr!!, info!!)
                val hh: HashSet<Rect>? = if (bb.highlight != null) {
                    if (fb.pluginview!!.reflow)
                        HashSet(fb.pluginview!!.boundsUpdate(bb.highlight!!, info!!).toList())
                    else
                        HashSet(bb.highlight!!.toList())
                } else null
                for (l in bb.rr!!) {
                    val lp = ViewGroup.MarginLayoutParams(l.width(), l.height())
                    val v = WordView(fb.context)
                    v.layoutParams = lp
                    v.tag = l
                    if (hh != null && hh.contains(l))
                        v.setBackgroundColor(SelectionView.SELECTION_ALPHA shl 24 or 0x00AA00)
                    else
                        v.setBackgroundColor(SelectionView.SELECTION_ALPHA shl 24 or fb.app.BookTextView.highlightingBackgroundColor.intValue())
                    words.add(v)
                    fb.addView(v)
                }
            }
        }

        fun update(x: Int, y: Int) {
            for (v in words) {
                val l = v.tag as Rect
                val lp = v.layoutParams as ViewGroup.MarginLayoutParams
                lp.leftMargin = x + l.left - padding
                lp.topMargin = y + l.top - padding
                lp.width = l.width() + 2 * padding
                lp.height = l.height() + 2 * padding
                v.requestLayout()
            }
        }

        fun hide() {
            for (v in words)
                v.visibility = View.GONE
        }

        fun show() {
            for (v in words)
                v.visibility = View.VISIBLE
        }

        fun close() {
            val old = ArrayList(words)
            // can be called during RelativeLayout onLayout
            fb.post {
                for (v in old)
                    fb.removeView(v)
            }
            words.clear()
        }

        inner class WordView(context: Context) : View(context) {
            override fun draw(canvas: Canvas) {
                val c = canvas.clipBounds
                c.bottom = clip - top
                canvas.clipRect(c)
                super.draw(canvas)
            }
        }
    }

    inner class CustomView(reader: FBReaderApp) : FBView(reader) {
        fun createContext(c: Canvas): ZLAndroidPaintContext {
            return ZLAndroidPaintContext(
                app.SystemInfo,
                c,
                ZLAndroidPaintContext.Geometry(
                    this@FBReaderView.width,
                    this@FBReaderView.height,
                    this@FBReaderView.width,
                    this@FBReaderView.height,
                    0,
                    0
                ),
                this@FBReaderView.verticalScrollbarWidth
            )
        }

        val footer: FBView.Footer?
            get() {
                val type = FBView.SCROLLBAR_SHOW_AS_FOOTER
                if (type == FBView.SCROLLBAR_SHOW_AS_FOOTER)
                    return FooterNew()
                if (type == FBView.SCROLLBAR_SHOW_AS_FOOTER_OLD_STYLE)
                    return FooterOld()
                return null
            }

        fun setContext(): ZLAndroidPaintContext {
            val context = createContext(Canvas())
            setContext(context)
            return context
        }

        override fun getAnimationType(): ZLViewEnums.Animation {
            val pm = this@FBReaderView.context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isPowerSaveMode)
                return ZLViewEnums.Animation.none
            else
                return super.getAnimationType()
        }

        fun setScalingType(imageElement: ZLTextImageElement, s: ZLPaintContext.ScalingType) {
            book!!.info!!.scales[imageElement.Id] = s
        }

        override fun getScalingType(imageElement: ZLTextImageElement): ZLPaintContext.ScalingType {
            val s = book!!.info!!.scales[imageElement.Id]
            if (s != null)
                return s
            return super.getScalingType(imageElement)
        }

        override fun hideOutline() {
            super.hideOutline()
            if (widget is ScrollWidget) {
                (widget as ScrollWidget).adapter.processInvalidate()
                (widget as ScrollWidget).adapter.processClear()
            }
        }

        override fun findRegion(x: Int, y: Int, maxDistance: Int, filter: ZLTextRegion.Filter): ZLTextRegion {
            return super.findRegion(x, y, maxDistance, filter)
        }

        override fun onFingerSingleTap(x: Int, y: Int) {
            val highlighting = findHighlighting(x, y, maxSelectionDistance())
            if (highlighting is ZLBookmark) {
                app.runAction(ActionCode.SELECTION_BOOKMARK, highlighting.b)
                return
            }
            super.onFingerSingleTap(x, y)
        }

        override fun onFingerSingleTapLastResort(x: Int, y: Int) {
            if (widget is ScrollWidget)
                onFingerSingleTapLastResort((widget as ScrollWidget).gesturesListener.e!!)
            else
                super.onFingerSingleTapLastResort(x, y)
        }

        fun onFingerSingleTapLastResort(e: MotionEvent) {
            setContext()
            super.onFingerSingleTapLastResort(e.x.toInt(), e.y.toInt())
        }

        override fun twoColumnView(): Boolean {
            if (widget is ScrollWidget)
                return false
            return super.twoColumnView()
        }

        override fun canScroll(index: ZLViewEnums.PageIndex): Boolean {
            if (pluginview != null)
                return pluginview!!.canScroll(index)
            else
                return super.canScroll(index)
        }

        @Synchronized
        override fun onScrollingFinished(pageIndex: ZLViewEnums.PageIndex) {
            if (pluginview != null) {
                if (pluginview!!.onScrollingFinished(pageIndex))
                    widget!!.reset()
            } else {
                super.onScrollingFinished(pageIndex)
            }
            this@FBReaderView.onScrollingFinished(pageIndex)
            if (widget is ZLAndroidWidget)
                (widget as PagerWidget).updateOverlays()
        }

        @Synchronized
        override fun pagePosition(): ZLTextView.PagePosition {
            if (pluginview != null)
                return pluginview!!.pagePosition()
            else
                return super.pagePosition()
        }

        override fun gotoHome() {
            if (pluginview != null)
                pluginview!!.gotoPosition(ZLTextFixedPosition(0, 0, 0))
            else
                super.gotoHome()
            resetNewPosition()
        }

        @Synchronized
        override fun gotoPage(page: Int) {
            if (pluginview != null)
                pluginview!!.gotoPosition(ZLTextFixedPosition(page - 1, 0, 0))
            else
                super.gotoPage(page)
            resetNewPosition()
        }

        @Synchronized
        override fun paint(context: ZLPaintContext, pageIndex: ZLViewEnums.PageIndex) {
            super.paint(context, pageIndex)
        }

        override fun getSelectionStartY(): Int {
            if (selection != null)
                return selection!!.getSelectionStartY()
            return super.getSelectionStartY()
        }

        override fun getSelectionEndY(): Int {
            if (selection != null)
                return selection!!.getSelectionEndY()
            return super.getSelectionEndY()
        }

        inner class FooterNew : FBView.FooterNewStyle() {
            override fun buildInfoString(pagePosition: ZLTextView.PagePosition, separator: String): String = ""
        }

        inner class FooterOld : FBView.FooterOldStyle() {
            override fun buildInfoString(pagePosition: ZLTextView.PagePosition, separator: String): String = ""
        }
    }

    inner class FBApplicationWindow : ZLApplicationWindow {
        override fun setWindowTitle(title: String) {
            this@FBReaderView.title = title
        }

        override fun showErrorMessage(resourceKey: String) {
        }

        override fun showErrorMessage(resourceKey: String, parameter: String) {
        }

        override fun createExecutor(key: String): ZLApplication.SynchronousExecutor? = null

        override fun processException(e: Exception) {
        }

        override fun refresh() {
            if (widget is PagerWidget)
                (widget as PagerWidget).updateOverlays()
        }

        override fun getViewWidget(): ZLViewWidget? = widget

        override fun close() {
        }

        override fun getBatteryLevel(): Int = battery
    }

    inner class FBReaderApp(context: Context) : org.geometerplus.fbreader.fbreader.FBReaderApp(Storage.Info(context), BookCollectionShadow()) {
        override fun getCurrentTOCElement(): TOCTree? {
            if (Model == null)
                return null
            if (pluginview != null)
                return pluginview!!.getCurrentTOCElement(Model!!.TOCTree)
            else
                return super.getCurrentTOCElement()
        }
    }

    private fun imageFitting(name: String): Enum<*> {
        val clazz = Class.forName("org.geometerplus.zlibrary.text.view.ZLTextViewBase\$ImageFitting")
        @Suppress("UNCHECKED_CAST")
        return (clazz as Class<out Enum<*>>).getEnumConstants()!!.first { it.name == name }
    }

    private fun fitToScreenOption(): ZLOption {
        val field = app.ImageOptions.javaClass.getField("FitToScreen")
        return field.get(app.ImageOptions) as ZLOption
    }
}
