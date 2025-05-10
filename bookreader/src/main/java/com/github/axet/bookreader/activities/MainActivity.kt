package com.github.axet.bookreader.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.preference.PreferenceManager
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.github.axet.androidlibrary.app.MainApplication
import com.github.axet.androidlibrary.preferences.AboutPreferenceCompat
import com.github.axet.androidlibrary.preferences.RotatePreferenceCompat
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter
import com.github.axet.androidlibrary.widgets.OpenChoicer
import com.github.axet.androidlibrary.widgets.OpenFileDialog
import com.github.axet.androidlibrary.widgets.SearchView
import com.github.axet.androidlibrary.widgets.SearchView.OnCloseButtonListener
import com.github.axet.androidlibrary.widgets.SearchView.OnCollapsedListener
import com.github.axet.androidlibrary.widgets.ThemeUtils
import com.github.axet.androidlibrary.widgets.WebViewCustom
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.BookApplication
import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.app.Storage.RecentInfo
import com.github.axet.bookreader.fragments.LibraryFragment
import com.github.axet.bookreader.fragments.LibraryFragment.Companion.newInstance
import com.github.axet.bookreader.fragments.ReaderFragment
import com.github.axet.bookreader.widgets.FBReaderView
import com.github.axet.bookreader.widgets.FBReaderView.ZLTextIndexPosition
import com.google.android.material.navigation.NavigationView
import org.geometerplus.fbreader.fbreader.options.ImageOptions
import org.geometerplus.fbreader.fbreader.options.MiscOptions
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import java.util.Collections
import com.github.axet.androidlibrary.R as AxetR

class MainActivity : FullscreenActivity(), NavigationView.OnNavigationItemSelectedListener,
    OnSharedPreferenceChangeListener {
    @JvmField
    var volumeEnabled: Boolean = true // tmp enabled / disable volume keys

    private val storage: Storage by lazy { Storage(this) }

    var isRunning: Boolean = false
    var choicer: OpenChoicer? = null
    var lastSearch: String? = null

    val libraryFragment: LibraryFragment = newInstance()
    val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == FBReaderView.ACTION_MENU) toggle()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(
                receiver,
                IntentFilter(FBReaderView.ACTION_MENU),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(receiver, IntentFilter(FBReaderView.ACTION_MENU))
        }

        if (savedInstanceState == null && intent.getParcelableExtra<Parcelable?>(
                SAVE_INSTANCE_STATE
            ) == null
        ) {
            openLibrary()
            openIntent(intent)
        }

        RotatePreferenceCompat.onCreate(this, BookApplication.PREFERENCE_ROTATE)

        val shared = PreferenceManager.getDefaultSharedPreferences(this)
        shared.registerOnSharedPreferenceChangeListener(this)

        openIntent(intent)
    }

    @SuppressLint("RestrictedApi")
    override fun onBackPressed() {
        val fm = supportFragmentManager
        val ff = fm.fragments

        for (f in ff) {
            if (f != null && f.isVisible && f is OnBackPressed) {
                val s = f as OnBackPressed
                if (s.onBackPressed()) return
            }
        }

        super.onBackPressed()
        if (fm.backStackEntryCount == 0) onResume() // update theme if changed
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val searchMenu = menu.findItem(R.id.action_search)

        val shared = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = menu.findItem(R.id.action_theme)
        val t: String = shared.getString(BookApplication.PREFERENCE_THEME, "")!!
        if (t == getString(AxetR.string.Theme_System)) {
            theme.isVisible = false
        } else {
            theme.isVisible = true
            val d = getString(AxetR.string.Theme_Dark)
            theme.setIcon(
                if (t == d) R.drawable.ic_brightness_night_white_24dp else R.drawable.ic_brightness_day_white_24dp
            )
            val map = ResourcesMap(
                this,
                AxetR.array.themes_values,
                AxetR.array.themes_text
            )
            theme.title =
                map.get(getString(if (t == d) AxetR.string.Theme_Dark else AxetR.string.Theme_Light))
        }

        val searchView = MenuItemCompat.getActionView(searchMenu) as SearchView
        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            @SuppressLint("RestrictedApi")
            override fun onQueryTextSubmit(query: String?): Boolean {
                lastSearch = query
                searchView.clearFocus()
                val fm = supportFragmentManager
                for (f in fm.fragments) {
                    if (f != null && f.isVisible && f is SearchListener) {
                        val s = f as SearchListener
                        s.search(searchView.query.toString())
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        searchView.setOnSearchClickListener(object : View.OnClickListener {
            @SuppressLint("RestrictedApi")
            override fun onClick(v: View?) {
                if (lastSearch != null && !lastSearch!!.isEmpty()) searchView.setQuery(
                    lastSearch,
                    false
                )
                val fm = supportFragmentManager
                for (f in fm.fragments) {
                    if (f != null && f.isVisible && f is SearchListener) {
                        val s = f as SearchListener
                        searchView.setQueryHint(s.hint)
                    }
                }
            }
        })
        searchView.setOnCollapsedListener(object : OnCollapsedListener {
            @SuppressLint("RestrictedApi")
            override fun onCollapsed() {
                val fm = supportFragmentManager
                for (f in fm.fragments) {
                    if (f != null && f.isVisible && f is SearchListener) {
                        val s = f as SearchListener
                        s.searchClose()
                    }
                }
            }
        })
        searchView.setOnCloseButtonListener(OnCloseButtonListener { lastSearch = "" })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_about) {
            AboutPreferenceCompat.buildDialog(this, R.raw.about).show()
            return true
        }

        if (id == R.id.action_settings) {
            SettingsActivity.startActivity(this)
            return true
        }

        val shared = PreferenceManager.getDefaultSharedPreferences(this)
        if (id == R.id.action_file) {
            val last = shared.getString(BookApplication.PREFERENCE_LAST_PATH, null)
            var old: Uri? = null
            if (last != null) {
                old = last.toUri()
                var f = Storage.getFile(old)
                while (f != null && !f.exists()) f = f.getParentFile()
                if (f != null) old = Uri.fromFile(f)
            } else {
                old = (ContentResolver.SCHEME_CONTENT + Storage.CSS).toUri() // show SAF default
            }
            choicer = object : OpenChoicer(OpenFileDialog.DIALOG_TYPE.FILE_DIALOG, true) {
                override fun onResult(uri: Uri) {
                    val s = uri.scheme
                    if (s == ContentResolver.SCHEME_FILE) {
                        var f = Storage.getFile(uri)
                        f = f!!.getParentFile()
                        shared.edit(commit = true) {
                            putString(BookApplication.PREFERENCE_LAST_PATH, f.toString())
                        }
                    }
                    loadBookFromUri(uri)
                }
            }
            choicer?.setStorageAccessFramework(this, RESULT_FILE)
            choicer?.setPermissionsDialog(this, Storage.PERMISSIONS_RO, RESULT_FILE)
            choicer?.show(old)
        }

        if (id == R.id.action_theme) {
            shared.edit(commit = true) {
                val t: String = shared.getString(BookApplication.PREFERENCE_THEME, "").orEmpty()
                val d = getString(AxetR.string.Theme_Dark)
                putString(
                    BookApplication.PREFERENCE_THEME,
                    if (t == d) getString(AxetR.string.Theme_Light) else d
                )
            }
            restartActivity()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.nav_library) openLibrary()

        return true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openIntent(intent)
    }

    fun openIntent(intent: Intent?) {
        if (intent == null) return
        val a = intent.action
        if (a == null) return
        var u = intent.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)
        if (u == null) u = intent.data
        if (u == null) {
            val t = intent.getStringExtra(Intent.EXTRA_TEXT) // handling SEND intents
            if (t != null && t.startsWith(WebViewCustom.SCHEME_HTTP)) {
                u = t.toUri()
            }
        }
        if (u == null) return
        loadBookFromUri(u)
    }

    /**
     * Доработка Воронин
     */
    fun loadBookFromUri(u: Uri?) {
        val book = Storage.Book(applicationContext, u)
        loadBookFromUri(book)
    }

    @SuppressLint("RestrictedApi")
    fun loadBookFromUri(book: Storage.Book) {
        val uu = storage.recentUris(book)
        if (uu.size > 1) {
            val inflater = LayoutInflater.from(this)
            val builder = AlertDialog.Builder(this)
            val selected = ArrayList<ZLTextPosition?>()

            selected.clear()
            selected.add(book.info.position)

            val fbook = storage.read(book)

            val done = Runnable {
                fbook.close()
                for (u in uu) {
                    try {
                        val info = RecentInfo(this@MainActivity, u)
                        book.info.merge(info)
                    } catch (e: Exception) {
                        Log.d(TAG, "unable to merge info", e)
                    }
                    Storage.delete(this@MainActivity, u)
                }
                book.info.position = selected[0]
                storage.save(book)
                openBook(book.url)
            }

            builder.setTitle(R.string.sync_conflict)

            val v = inflater.inflate(R.layout.recent, null)

            val r = v.findViewById<View?>(R.id.recent_fbview) as FBReaderView
            r.config.setValue(
                r.app.MiscOptions.WordTappingAction,
                MiscOptions.WordTappingActionEnum.doNothing
            )
            r.config.setValue(r.app.ImageOptions.TapAction, ImageOptions.TapActionEnum.doNothing)

            val shared = PreferenceManager.getDefaultSharedPreferences(this)
            val mode: String = shared.getString(BookApplication.PREFERENCE_VIEW_MODE, "")!!
            r.setWidget(if (mode == FBReaderView.Widgets.CONTINUOUS.toString()) FBReaderView.Widgets.CONTINUOUS else FBReaderView.Widgets.PAGING)

            r.loadBook(fbook)

            val pages = v.findViewById<View?>(R.id.recent_pages) as LinearLayout

            val rr: MutableList<RecentInfo> = ArrayList<RecentInfo>()

            for (u in uu) {
                try {
                    val info = RecentInfo(this@MainActivity, u)
                    if (info.position != null) {
                        var found = false
                        for (i in rr.indices) {
                            val ii = rr[i]
                            if (ii.position.paragraphIndex == info.position.paragraphIndex &&
                                ii.position.elementIndex == info.position.elementIndex
                            ) found = true
                        }
                        if (!found) {
                            rr.add(info)
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Unable to read info", e)
                }
            }

            Collections.sort<RecentInfo?>(rr, SortByPage())

            if (rr.size == 1) {
                done.run()
                return
            }

            for (i in rr.indices) {
                val info = rr[i]
                val p = inflater.inflate(R.layout.recent_item, pages, false) as TextView
                if (info.position != null) {
                    val text =
                        info.position.paragraphIndex.toString() + "." + info.position.elementIndex
                    p.text = text
                }
                p.setOnClickListener(View.OnClickListener { v1: View? ->
                    r.gotoPosition(info.position)
                    selected.clear()
                    selected.add(info.position)
                })
                pages.addView(p)
            }

            builder.setView(v)

            builder.setOnDismissListener(DialogInterface.OnDismissListener { dialog: DialogInterface? -> fbook.close() })

            builder.setNegativeButton(
                android.R.string.cancel,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> })
            builder.setPositiveButton(
                android.R.string.ok,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> done.run() })

            builder.show()
            return
        }
        openBook(book.url)
    }

    @SuppressLint("RestrictedApi")
    fun popBackStack(tag: String?, flags: Int) { // only pop existing TAG
        val fm = supportFragmentManager
        if (tag == null) {
            fm.popBackStack(null, flags)
            return
        }
        for (i in 0..<fm.backStackEntryCount) {
            val n = fm.getBackStackEntryAt(i).name
            if (n != null && n == tag) {
                fm.popBackStack(tag, flags)
                return
            }
        }
    }

    fun openBook(uri: Uri?) {
        popBackStack(ReaderFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        addFragment(ReaderFragment.newInstance(uri), ReaderFragment.TAG).commit()
    }

    fun openBook(uri: Uri?, pos: ZLTextIndexPosition?) {
        popBackStack(ReaderFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        addFragment(ReaderFragment.newInstance(uri, pos), ReaderFragment.TAG).commit()
    }

    fun openLibrary() {
        supportFragmentManager
        popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        openFragment(libraryFragment, LibraryFragment.TAG).commit()
        onResume() // update theme if changed
    }

    fun addFragment(f: Fragment, tag: String?): FragmentTransaction {
        return openFragment(f, tag).addToBackStack(tag)
    }

    fun openFragment(f: Fragment, tag: String?): FragmentTransaction {
        val fm = supportFragmentManager
        return fm.beginTransaction().replace(R.id.main_content, f, tag)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        RotatePreferenceCompat.onDestroy(this)
        val shared = PreferenceManager.getDefaultSharedPreferences(this)
        shared.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        isRunning = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RESULT_FILE, RESULT_ADD_CATALOG -> if (choicer != null)  // called twice or activity reacated
                choicer!!.onRequestPermissionsResult(permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RESULT_FILE, RESULT_ADD_CATALOG -> if (choicer != null)  // called twice or activity reacated
                choicer!!.onActivityResult(resultCode, data)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val shared = PreferenceManager.getDefaultSharedPreferences(this)
        if (volumeEnabled && shared.getBoolean(BookApplication.PREFERENCE_VOLUME_KEYS, false)) {
            val fm = supportFragmentManager
            for (f in fm.fragments) {
                if (f != null && f.isVisible && f is ReaderFragment) {
                    if (f.onKeyDown(keyCode, event)) return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    @SuppressLint("RestrictedApi")
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val shared = PreferenceManager.getDefaultSharedPreferences(this)
        if (volumeEnabled && shared.getBoolean(BookApplication.PREFERENCE_VOLUME_KEYS, false)) {
            val fm = supportFragmentManager
            for (f in fm.fragments) {
                if (f != null && f.isVisible && f is ReaderFragment) {
                    if (f.onKeyUp(keyCode, event)) return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        isRunning = true
        RotatePreferenceCompat.onResume(this, BookApplication.PREFERENCE_ROTATE)
        CacheImagesAdapter.cacheClear(this)
        BookApplication.from(this)?.ttf?.preloadFonts()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key == BookApplication.PREFERENCE_THEME) invalidateOptionsMenu()
    }

    interface SearchListener {
        val hint: String?

        fun search(s: String?)

        fun searchClose()
    }

    interface OnBackPressed {
        fun onBackPressed(): Boolean
    }

    class SortByPage : Comparator<RecentInfo> {
        override fun compare(o1: RecentInfo, o2: RecentInfo): Int {
            val r = o1.position.paragraphIndex.compareTo(o2.position.paragraphIndex)
            if (r != 0) return r

            return o1.position.elementIndex.compareTo(o2.position.elementIndex)
        }
    }

    class ResourcesMap(context: Context, k: Int, v: Int) : HashMap<String?, String?>() {
        init {
            val kk = context.resources.getStringArray(k)
            val vv = context.resources.getStringArray(v)
            for (i in kk.indices) put(kk[i], vv[i])
        }
    }

    class ProgressDialog(context: Context) : AlertDialog.Builder(context) {
        val handler: Handler = Handler()
        var load: ProgressBar
        var v: ProgressBar
        var text: TextView
        var dialog: AlertDialog? = null

        var progress: Storage.Progress = object : Storage.Progress() {
            override fun progress(bytes: Long, total: Long) {
                handler.post(Runnable { this@ProgressDialog.progress(bytes, total) })
            }
        }

        init {
            val dp10 = ThemeUtils.dp2px(context, 10f)

            val ll = LinearLayout(context)
            ll.orientation = LinearLayout.VERTICAL
            v = ProgressBar(context)
            v.isIndeterminate = true
            v.setPadding(dp10, dp10, dp10, dp10)
            ll.addView(v)
            load = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
            load.setPadding(dp10, dp10, dp10, dp10)
            load.setMax(100)
            ll.addView(load)
            text = TextView(context)
            text.setPadding(dp10, dp10, dp10, dp10)
            ll.addView(text)
            load.visibility = View.GONE
            text.visibility = View.GONE

            setTitle(R.string.loading_book)
            setView(ll)
            setCancelable(false)
            setPositiveButton(
                android.R.string.cancel,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> })
        }

        fun progress(bytes: Long, total: Long) {
            var str = MainApplication.formatSize(context, bytes)
            if (total > 0) {
                str += " / " + MainApplication.formatSize(context, total)
                load.progress = (bytes * 100 / total).toInt()
                load.visibility = View.VISIBLE
                v.visibility = View.GONE
            } else {
                load.visibility = View.GONE
                v.visibility = View.VISIBLE
            }
            str += String.format(
                " (%s%s)",
                MainApplication.formatSize(context, progress.info.getCurrentSpeed().toLong()),
                context.getString(R.string.per_second)
            )
            text.text = str
            text.visibility = View.VISIBLE
        }

        fun build() {
            dialog = super.create()
        }
    }

    companion object {
        private val TAG: String = MainActivity::class.java.getSimpleName()

        private const val RESULT_FILE: Int = 1
        private const val RESULT_ADD_CATALOG: Int = 2
    }
}
