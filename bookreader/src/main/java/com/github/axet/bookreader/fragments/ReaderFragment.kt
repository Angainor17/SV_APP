package com.github.axet.bookreader.fragments

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.view.MenuItemCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.github.axet.androidlibrary.BuildConfig
import com.github.axet.androidlibrary.preferences.ScreenlockPreference
import com.github.axet.androidlibrary.widgets.ErrorDialog
import com.github.axet.androidlibrary.widgets.InvalidateOptionsMenuCompat
import com.github.axet.androidlibrary.widgets.PopupWindowCompat
import com.github.axet.androidlibrary.widgets.ThemeUtils
import com.github.axet.androidlibrary.widgets.TreeListView
import com.github.axet.androidlibrary.widgets.TreeRecyclerView
import com.github.axet.bookreader.R
import com.github.axet.bookreader.activities.FullscreenActivity.FullscreenListener
import com.github.axet.bookreader.activities.MainActivity
import com.github.axet.bookreader.activities.MainActivity.OnBackPressed
import com.github.axet.bookreader.activities.MainActivity.SearchListener
import com.github.axet.bookreader.app.BookApplication
import com.github.axet.bookreader.app.ComicsPlugin.ComicsView
import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.app.Storage.FBook
import com.github.axet.bookreader.app.Storage.RecentInfo
import com.github.axet.bookreader.widgets.BookmarksDialog
import com.github.axet.bookreader.widgets.FBReaderView
import com.github.axet.bookreader.widgets.FBReaderView.Widgets
import com.github.axet.bookreader.widgets.FBReaderView.ZLTextIndexPosition
import com.github.axet.bookreader.widgets.FontsPopup
import com.github.axet.bookreader.widgets.ScrollWidget
import com.github.axet.bookreader.widgets.ToolbarButtonView
import org.geometerplus.fbreader.bookmodel.TOCTree
import org.geometerplus.fbreader.fbreader.ActionCode
import org.geometerplus.zlibrary.core.view.ZLViewEnums.PageIndex

class ReaderFragment : Fragment(), SearchListener, OnSharedPreferenceChangeListener,
    FullscreenListener, OnBackPressed {

    private val handler: Handler = Handler()
    private val storage: Storage by lazy { Storage(context) }
    private var book: Storage.Book? = null
    private var fbook: FBook? = null
    private var fb: FBReaderView? = null
    private var tocdialog: AlertDialog? = null
    private var showRTL: Boolean = false
    private var fontsPopup: FontsPopup? = null
    private var searchMenu: MenuItem? = null
    private var battery: BroadcastReceiver? = null
    private var invalidateOptionsMenu: Runnable? = null
    private val time: Runnable = object : Runnable {
        override fun run() {
            val s60 = (60 * 1000).toLong()
            val secs = System.currentTimeMillis() % s60
            handler.removeCallbacks(this)
            var d = s60 - secs
            if (d < 1000) d = s60 + d
            handler.postDelayed(this, d)
            fb!!.invalidateFooter()
            savePosition()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        val shared = PreferenceManager.getDefaultSharedPreferences(context)
        shared.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_reader, container, false)

        val main = activity as MainActivity?
        fb = v.findViewById<View?>(R.id.main_view) as FBReaderView?

        fb!!.listener = object : FBReaderView.Listener {
            override fun onScrollingFinished(index: PageIndex?) {
                if (fontsPopup != null) {
                    fontsPopup!!.dismiss()
                    fontsPopup = null
                }
                updateToolbar()
            }

            override fun onSearchClose() {
                MenuItemCompat.collapseActionView(searchMenu)
            }

            override fun onBookmarksUpdate() {
                updateToolbar()
            }

            override fun onDismissDialog() {
                if (main!!.fullscreen) main.hideSystemUI()
            }

            override fun ttsStatus(speaking: Boolean) {
                val main = activity as MainActivity?
                main!!.volumeEnabled = !speaking
            }
        }

        val shared = PreferenceManager.getDefaultSharedPreferences(context)
        val mode: String = shared.getString(BookApplication.PREFERENCE_VIEW_MODE, "")!!
        fb!!.setWidget(if (mode == Widgets.CONTINUOUS.toString()) Widgets.CONTINUOUS else Widgets.PAGING)

        fb!!.setWindow(requireActivity().window)
        fb!!.setActivity(activity)

        val uri = requireArguments().getParcelable<Uri?>("uri")
        val pos = requireArguments().getParcelable<ZLTextIndexPosition?>("pos")

        try {
            book = storage.load(uri)
            fbook = storage.read(book)
            fb!!.loadBook(fbook)
            if (pos != null) fb!!.gotoPosition(pos)
        } catch (e: RuntimeException) {
            ErrorDialog.Error(main, e)
            // or openLibrary crash with java.lang.IllegalStateException on FragmentActivity.onResume
            handler.post(Runnable {
                if (!main!!.isFinishing) main.openLibrary()
            })
            return v // ignore post called
        }

        handler.post(
            Runnable {
                if (requireActivity().isFinishing) return@Runnable
                updateToolbar() // update toolbar after page been drawn to detect RTL
                fb?.showControls() //  update toolbar after page been drawn, getWidth() == 0
            }
        )

        return v
    }

    fun updateToolbar() {
        if (invalidateOptionsMenu != null) invalidateOptionsMenu!!.run()
    }

    override fun onResume() {
        super.onResume()
        ScreenlockPreference.onResume(activity, BookApplication.PREFERENCE_SCREENLOCK)

        battery = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                fb!!.battery = level * 100 / scale
                fb!!.invalidateFooter()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            battery!!.onReceive(
                requireContext(),
                requireContext().registerReceiver(
                    battery,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                    Context.RECEIVER_NOT_EXPORTED
                )
            )
        } else {
            battery!!.onReceive(
                requireContext(),
                requireContext().registerReceiver(
                    battery,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
            )
        }

        time.run()

        updateTheme() // MainActivity.restartActivity() not called when double change while ReaderFragment active
    }

    override fun onPause() {
        super.onPause()
        savePosition()
        ScreenlockPreference.onPause(activity, BookApplication.PREFERENCE_SCREENLOCK)

        if (battery != null) {
            requireContext().unregisterReceiver(battery)
            battery = null
        }

        handler.removeCallbacks(time)
    }

    override fun onDetach() {
        super.onDetach()
        savePosition()
    }

    val fontsizeReflow: Float
        get() {
            val fontsize = fb!!.getFontsizeReflow()
            if (fontsize != null) return fontsize
            val shared =
                PreferenceManager.getDefaultSharedPreferences(context)
            return shared.getFloat(
                BookApplication.PREFERENCE_FONTSIZE_REFLOW,
                BookApplication.PREFERENCE_FONTSIZE_REFLOW_DEFAULT
            )
        }

    fun savePosition() {
        if (book == null) return
        if (fb!!.book == null)  // when book isn't loaded and view closed
            return
        val save = RecentInfo(fb!!.book.info)
        save.position = fb!!.position
        val u = storage!!.recentUri(book)
        if (Storage.exists(
                context,
                u
            )
        ) { // file can be changed during sync, check for conflicts
            try {
                val info = RecentInfo(context, u)
                if (info.position != null && save.position.samePositionAs(info.position)) {
                    if (save.fontsize == null || info.fontsize != null && save.fontsize == info.fontsize) {
                        if (save.equals(info.fontsizes)) if (save.bookmarks == null || info.bookmarks != null && save.bookmarks.equals(
                                info.bookmarks
                            )
                        ) return  // nothing to save
                    }
                }
                if (book!!.info.last != info.last)  // file changed between saves?
                    storage!!.move(u, storage!!.getStoragePath()) // yes. create conflict (1)

                save.merge(info.fontsizes, info.last)
            } catch (e: RuntimeException) {
                Log.d(TAG, "Unable to load JSON", e)
            }
        }
        book!!.info = save
        storage!!.save(book)
        Log.d(TAG, "savePosition " + save.position)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        val shared = PreferenceManager.getDefaultSharedPreferences(context)
        shared.unregisterOnSharedPreferenceChangeListener(this)
        handler.removeCallbacks(time)
        ScreenlockPreference.onUserInteractionRemove()
        if (fb != null)  // onDestory without onCreate
            fb!!.closeBook()
        if (fontsPopup != null) {
            fontsPopup!!.dismiss()
            fontsPopup = null
        }
        if (fbook != null) {
            fbook!!.close()
            fbook = null
        }
        book = null
    }

    override fun onUserInteraction() {
        ScreenlockPreference.onUserInteraction(activity, BookApplication.PREFERENCE_SCREENLOCK)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (fontsPopup != null) {
            fontsPopup!!.dismiss()
            fontsPopup = null
        }
        val id = item.itemId
        if (id == R.id.action_toc) {
            showTOC()
            return true
        }
        if (id == R.id.action_bm) {
            val dialog: BookmarksDialog = object : BookmarksDialog(context) {
                override fun onSelected(b: Storage.Bookmark) {
                    fb!!.gotoPosition(ZLTextIndexPosition(b.start, b.end))
                }

                override fun onSave(bm: Storage.Bookmark?) {
                    fb!!.bookmarksUpdate()
                    savePosition()
                }

                override fun onDelete(bm: Storage.Bookmark?) {
                    var i = book!!.info.bookmarks.indexOf(bm)
                    book!!.info.bookmarks.removeAt(i)
                    i = fb!!.book.info.bookmarks.indexOf(bm)
                    fb!!.book.info.bookmarks.removeAt(i)
                    fb!!.bookmarksUpdate()
                    savePosition()
                }
            }
            dialog.load(fb!!.book.info.bookmarks)
            dialog.show()
            return true
        }
        if (id == R.id.action_reflow) {
            fb!!.setReflow(!fb!!.isReflow)
            updateToolbar()
        }
        if (id == R.id.action_debug) {
            fb!!.pluginview.reflowDebug = !fb!!.pluginview.reflowDebug
            if (fb!!.pluginview.reflowDebug) {
                fb!!.pluginview.reflow = true
                fb!!.setWidget(Widgets.PAGING)
            }
            fb!!.reset()
            updateToolbar()
        }
        if (id == R.id.action_fontsize) {
            if (fb!!.pluginview == null) {
                fontsPopup =
                    object : FontsPopup(context, BookApplication.from(requireContext())?.ttf) {
                        override fun setFont(f: String?) {
                            val shared = PreferenceManager.getDefaultSharedPreferences(context)
                            shared.edit {
                                putString(BookApplication.PREFERENCE_FONTFAMILY_FBREADER, f)
                            }
                            fb?.setFontFB(f)
                            updateToolbar()
                        }

                        override fun setFontsize(f: Int) {
                            val shared = PreferenceManager.getDefaultSharedPreferences(context)
                            shared.edit {
                                putInt(BookApplication.PREFERENCE_FONTSIZE_FBREADER, f)
                            }
                            fb?.setFontsizeFB(f)
                            updateToolbar()
                        }

                        override fun setIgnoreEmbeddedFonts(f: Boolean) {
                            val shared = PreferenceManager.getDefaultSharedPreferences(context)
                            shared.edit {
                                putBoolean(BookApplication.PREFERENCE_IGNORE_EMBEDDED_FONTS, f)
                            }
                            fb?.setIgnoreCssFonts(f)
                            updateToolbar()
                        }

                        override fun updateFontsize(f: Int) {
                            fontsizepopup_text.text = f.toString()
                        }
                    }
                fontsPopup!!.fragment = this
                fontsPopup!!.code = RESULT_FONTS
                fontsPopup!!.loadFonts()
                fontsPopup!!.fonts.select(
                    fb!!.app.ViewOptions.getTextStyleCollection()
                        .baseStyle.FontFamilyOption.value
                )
                fontsPopup!!.ignore_embedded_fonts.setChecked(fb!!.ignoreCssFonts)
                fontsPopup!!.fontsList.scrollToPosition(fontsPopup!!.fonts.selected)
                fontsPopup!!.updateFontsize(FONT_START, FONT_END, fb!!.getFontsizeFB())
            } else {
                fontsPopup =
                    object : FontsPopup(context, BookApplication.from(requireContext())?.ttf) {
                        override fun setFontsize(f: Int) {
                            val p = f / 10f
                            val shared = PreferenceManager.getDefaultSharedPreferences(context)
                            shared.edit {
                                putFloat(BookApplication.PREFERENCE_FONTSIZE_REFLOW, p)
                            }
                            fb!!.setFontsizeReflow(p)
                            updateToolbar()
                        }

                        override fun updateFontsize(f: Int) {
                            fontsizepopup_text.text = String.format("%.1f", f / 10f)
                        }
                    }
                fontsPopup!!.fontsFrame.visibility = View.GONE
                fontsPopup!!.updateFontsize(
                    REFLOW_START,
                    REFLOW_END,
                    (this.fontsizeReflow * 10).toInt()
                )
            }
            var v = MenuItemCompat.getActionView(item)
            if (v == null || !ViewCompat.isAttachedToWindow(v)) v = getOverflowMenuButton(
                requireActivity()
            )
            PopupWindowCompat.showAsTooltip(
                fontsPopup, v, Gravity.BOTTOM,
                ThemeUtils.getThemeColor(
                    context,
                    com.github.axet.androidlibrary.R.attr.colorButtonNormal
                ),  // v has overflow ThemedContext
                ThemeUtils.dp2px(context, 300f)
            )
        }
        if (id == R.id.action_rtl) {
            fb?.app?.BookTextView?.rtlMode = !fb!!.app.BookTextView.rtlMode
            fb?.reset()
            updateToolbar()
        }
        if (id == R.id.action_mode) {
            val m = if (fb!!.widget is ScrollWidget) Widgets.PAGING else Widgets.CONTINUOUS
            val shared = PreferenceManager.getDefaultSharedPreferences(context)
            shared.edit {
                putString(BookApplication.PREFERENCE_VIEW_MODE, m.toString())
            }
            fb?.setWidget(m)
            fb?.reset()
            updateToolbar()
        }
        if (id == R.id.action_tts) {
            if (fb!!.tts != null) {
                fb?.tts?.dismiss()
                fb?.tts = null
            } else {
                fb?.ttsOpen()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun updateTheme() {
        fb?.updateTheme()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        invalidateOptionsMenu =
            InvalidateOptionsMenuCompat.onCreateOptionsMenu(this, menu, inflater)

        val homeMenu = menu.findItem(R.id.action_home)
        val tocMenu = menu.findItem(R.id.action_toc)
        searchMenu = menu.findItem(R.id.action_search)
        val reflow = menu.findItem(R.id.action_reflow)
        val debug = menu.findItem(R.id.action_debug)
        val bookmarksMenu = menu.findItem(R.id.action_bm)
        val fontsize = menu.findItem(R.id.action_fontsize)
        val rtl = menu.findItem(R.id.action_rtl)
        val grid = menu.findItem(R.id.action_grid)
        val mode = menu.findItem(R.id.action_mode)
        val theme = menu.findItem(R.id.action_theme)
        val sort = menu.findItem(R.id.action_sort)
        val tts = menu.findItem(R.id.action_tts)

        val search: Boolean

        if (fb!!.pluginview == null) {
            search = true
        } else {
            val s = fb!!.pluginview.search("")
            if (s == null) {
                search = false
            } else {
                s.close()
                search = true
            }
            if (fb!!.pluginview.reflow || fb!!.pluginview is ComicsView) tts.isVisible =
                false // TODO reflow - possible and can be very practical
        }

        grid.isVisible = false
        homeMenu.isVisible = false
        sort.isVisible = false
        tocMenu.isVisible =
            fb!!.app.Model != null && fb!!.app.Model.TOCTree != null && fb!!.app.Model.TOCTree.hasChildren()
        searchMenu!!.isVisible = search
        reflow.isVisible = fb!!.pluginview != null && fb!!.pluginview !is ComicsView

        debug.isVisible =
            BuildConfig.DEBUG && fb!!.pluginview != null && (fb!!.pluginview !is ComicsView)

        fontsize.isVisible = fb!!.pluginview == null || fb!!.pluginview.reflow
        if (fb!!.pluginview == null) {
            (MenuItemCompat.getActionView(fontsize) as ToolbarButtonView).text.text =
                "" + (if (fb!!.book == null) "" else fb!!.getFontsizeFB())
        } // call before onCreateView
        else {
            (MenuItemCompat.getActionView(fontsize) as ToolbarButtonView).text.text = String.format(
                "%.1f",
                this.fontsizeReflow
            )
        }
        MenuItemCompat.getActionView(fontsize).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                onOptionsItemSelected(fontsize)
            }
        })

        mode.setIcon(if (fb!!.widget is ScrollWidget) R.drawable.ic_view_day_black_24dp else R.drawable.ic_view_carousel_black_24dp) // icon current
        mode.setTitle(if (fb!!.widget is ScrollWidget) R.string.view_mode_paging else R.string.view_mode_continuous) // text next

        showRTL = showRTL or (!fb!!.app.BookTextView.rtlMode && fb!!.app.BookTextView.rtlDetected)
        rtl.isVisible = showRTL
        MenuItemCompat.getActionView(rtl).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                onOptionsItemSelected(rtl)
            }
        })
        rtl.title = if (fb!!.app.BookTextView.rtlMode) "RTL" else "LTR"
        (MenuItemCompat.getActionView(rtl) as ToolbarButtonView).text.text =
            if (fb!!.app.BookTextView.rtlMode) "RTL" else "LTR"
        if (fb!!.book != null)  // call before onCreateView
            bookmarksMenu.isVisible =
                fb!!.book.info.bookmarks != null && fb!!.book.info.bookmarks.isNotEmpty()

        if (fb!!.pluginview is ComicsView) theme.isVisible = false
    }

    fun showTOC() {
        val builder = AlertDialog.Builder(requireContext())
        val current = fb!!.app.currentTOCElement
        val a = TOCAdapter(fb!!.app.Model.TOCTree.subtrees(), current)
        val tree = TreeRecyclerView(requireContext())
        tree.setAdapter(a)
        builder.setView(tree)
        builder.setPositiveButton(
            android.R.string.cancel,
            object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                }
            })
        tocdialog = builder.create()
        tocdialog?.setOnShowListener(object : OnShowListener {
            override fun onShow(dialog: DialogInterface?) {
                val i = a.getCurrent() - 1
                if (i > 0) tree.setSelection(i)
            }
        })
        tocdialog?.show()
    }

    override fun search(s: String?) {
        fb?.app?.runAction(ActionCode.SEARCH, s)
    }

    override fun searchClose() {
        fb?.searchClose()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key == BookApplication.PREFERENCE_VIEW_MODE) {
            fb?.configWidget(sharedPreferences)
            fb?.showControls()
        }
        if (key == BookApplication.PREFERENCE_THEME) {
            fb?.configColorProfile(sharedPreferences) // FIXME Воронин тут ловил баг
        }
    }

    override val hint: String?
        get() = getString(R.string.search_book)

    override fun onFullscreenChanged(f: Boolean) {
        fb?.onConfigurationChanged(null)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            fb!!.app.runAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD)
            return true
        }
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            fb!!.app.runAction(ActionCode.VOLUME_KEY_SCROLL_BACK)
            return true
        }
        return false
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) return true
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)) return true
        return false
    }

    override fun onBackPressed(): Boolean {
        if (fb!!.isPinch()) {
            fb!!.pinchClose()
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (fontsPopup != null && fontsPopup!!.choicer != null) fontsPopup!!.choicer.onActivityResult(
            resultCode,
            data
        )
    }

    class TOCHolder(itemView: View) : TreeRecyclerView.TreeHolder(itemView) {
        val i: ImageView = itemView.findViewById<View?>(R.id.image) as ImageView
        val textView: TextView = itemView.findViewById<View?>(R.id.text) as TextView
    }

    inner class TOCAdapter(
        ll: MutableList<TOCTree>,
        val current: TOCTree?,
    ) :
        TreeRecyclerView.TreeAdapter<TOCHolder?>() {

        init {
            loadTOC(root, ll)
            load()
        }

        fun loadTOC(r: TreeListView.TreeNode, tree: MutableList<TOCTree>) {
            for (t in tree) {
                val n = TreeListView.TreeNode(r, t)
                r.nodes.add(n)
                if (equals(t, current)) {
                    n.selected = true // current selected
                    r.expanded = true // parent expanded
                }
                if (t.hasChildren()) {
                    loadTOC(n, t.subtrees())
                    if (n.expanded) {
                        n.selected = true
                        r.expanded = true
                    }
                }
            }
        }

        fun getCurrent(): Int {
            for (i in 0..<itemCount) {
                val t = getItem(i).tag as TOCTree?
                if (equals(t, current)) return i
            }
            return -1
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TOCHolder {
            val inflater = LayoutInflater.from(parent.context)
            val convertView = inflater.inflate(R.layout.toc_item, null)
            return TOCHolder(convertView)
        }

        override fun onBindViewHolder(h: TOCHolder, position: Int) {
            val t = getItem(h.getAdapterPosition(this))
            val tt = t.tag as TOCTree
            val ex = h.itemView.findViewById<View?>(R.id.expand) as ImageView
            if (t.nodes.isEmpty()) ex.setVisibility(View.INVISIBLE)
            else ex.setVisibility(View.VISIBLE)
            ex.setImageResource(
                if (t.expanded) R.drawable.ic_expand_less_black_24dp else R.drawable.ic_expand_more_black_24dp
            )
            h.itemView.setPadding(20 * t.level, 0, 0, 0)
            if (t.selected) {
                h.textView.setTypeface(null, Typeface.BOLD)
                h.i.colorFilter = null
            } else {
                h.i.setColorFilter(Color.GRAY)
                h.textView.setTypeface(null, Typeface.NORMAL)
            }
            h.textView.text = tt.text
            h.itemView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    val n = getItem(h.getAdapterPosition(this@TOCAdapter)).tag as TOCTree
                    if (n.hasChildren()) return
                    fb!!.gotoPosition(n.reference)
                    tocdialog!!.dismiss()
                }
            })
        }

        fun equals(t: TOCTree?, t2: TOCTree?): Boolean {
            if (t == null || t2 == null) return false
            val r1 = t.reference
            val r2 = t2.reference
            if (r1 == null || r2 == null) return false
            return r1.ParagraphIndex == r2.ParagraphIndex
        }
    }

    companion object {
        val TAG: String = ReaderFragment::class.java.getSimpleName()

        const val FONT_START: Int = 15
        const val FONT_END: Int = 100
        const val REFLOW_START: Int = 3
        const val REFLOW_END: Int = 15

        const val RESULT_FONTS: Int = 1

        fun getOverflowMenuButton(a: Activity): View? {
            return getOverflowMenuButton((a.findViewById<View?>(R.id.toolbar) as ViewGroup?)!!)
        }

        fun getOverflowMenuButton(p: ViewGroup): View? {
            for (i in 0..<p.childCount) {
                var v = p.getChildAt(i)
                if (v!!.javaClass.getCanonicalName().contains("OverflowMenuButton")) return v
                if (v is ViewGroup) {
                    v = getOverflowMenuButton(v)
                    if (v != null) return v
                }
            }
            return null
        }

        fun newInstance(uri: Uri?): ReaderFragment {
            val fragment = ReaderFragment()
            val args = Bundle()
            args.putParcelable("uri", uri)
            fragment.setArguments(args)
            return fragment
        }

        fun newInstance(uri: Uri?, pos: ZLTextIndexPosition?): ReaderFragment {
            val fragment = ReaderFragment()
            val args = Bundle()
            args.putParcelable("uri", uri)
            args.putParcelable("pos", pos)
            fragment.setArguments(args)
            return fragment
        }
    }
}
