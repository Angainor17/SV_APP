/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader

import android.annotation.TargetApi
import android.app.SearchManager
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.view.WindowManager
import android.widget.RelativeLayout
import org.fbreader.util.Boolean3
import org.geometerplus.R
import org.geometerplus.android.fbreader.api.ApiListener
import org.geometerplus.android.fbreader.api.ApiServerImplementation
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.api.MenuNode
import org.geometerplus.android.fbreader.api.PluginApi
import org.geometerplus.android.fbreader.dict.DictionaryUtil
import org.geometerplus.android.fbreader.formatPlugin.PluginUtil
import org.geometerplus.android.fbreader.httpd.DataService
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow
import org.geometerplus.android.fbreader.sync.SyncOperations
import org.geometerplus.android.fbreader.tips.TipsActivity
import org.geometerplus.android.util.DeviceType
import org.geometerplus.android.util.SearchDialogUtil
import org.geometerplus.android.util.UIMessageUtil
import org.geometerplus.android.util.UIUtil
import org.geometerplus.fbreader.Paths
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.fbreader.ActionCode
import org.geometerplus.fbreader.fbreader.DictionaryHighlighting
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.fbreader.fbreader.options.CancelMenuHelper
import org.geometerplus.fbreader.fbreader.options.ColorProfile
import org.geometerplus.fbreader.formats.ExternalFormatPlugin
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.fbreader.tips.TipsManager
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.view.ZLViewWidget
import org.geometerplus.zlibrary.text.view.ZLTextRegion
import org.geometerplus.zlibrary.ui.android.error.ErrorKeys
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget
import java.io.PrintWriter
import java.io.StringWriter

class FBReader : FBReaderMainActivity(), ZLApplicationWindow {

    private val myNotifier = AppNotifier(this)
    private val myPluginActions = mutableListOf<PluginApi.ActionInfo>()
    private val myMenuItemMap = HashMap<MenuItem, String>()

    @Volatile
    var isPaused = false
        private set

    @Volatile
    var onResumeAction: Runnable? = null

    private lateinit var myFBReaderApp: FBReaderApp

    private val myPluginInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val actions = getResultExtras(true).getParcelableArrayList<PluginApi.ActionInfo>(PluginApi.PluginInfo.KEY)
            if (actions != null) {
                synchronized(myPluginActions) {
                    var index = 0
                    while (index < myPluginActions.size) {
                        myFBReaderApp.removeAction(PLUGIN_ACTION_PREFIX + index++)
                    }
                    myPluginActions.addAll(actions)
                    index = 0
                    for (info in myPluginActions) {
                        myFBReaderApp.addAction(
                            PLUGIN_ACTION_PREFIX + index++,
                            RunPluginAction(this@FBReader, myFBReaderApp, info.getId())
                        )
                    }
                }
            }
        }
    }

    private val myMenuListener = MenuItem.OnMenuItemClickListener { item ->
        myFBReaderApp.runAction(myMenuItemMap[item]!!)
        true
    }

    @Volatile
    private var myBook: Book? = null
    private lateinit var myRootView: RelativeLayout
    private lateinit var myMainView: ZLAndroidWidget
    @Volatile
    private var myShowStatusBarFlag = false
    private var myMenuLanguage: String? = null
    @Volatile
    private var myResumeTimestamp: Long = 0
    private var myCancelIntent: Intent? = null
    private var myOpenBookIntent: Intent? = null
    private var myWakeLock: PowerManager.WakeLock? = null
    private var myWakeLockToCreate = false
    private var myStartTimer = false
    private var myBatteryLevel = 0

    private val myBatteryInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra("level", 100)
            setBatteryLevel(level)
            switchWakeLock(
                hasWindowFocus() &&
                    getZLibrary().BatteryLevelToTurnScreenOffOption.value < level
            )
        }
    }

    private val mySyncUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            myFBReaderApp.useSyncInfo(myResumeTimestamp + 10 * 1000 > System.currentTimeMillis(), myNotifier)
        }
    }

    val dataConnection = DataService.Connection()

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        bindService(
            Intent(this, DataService::class.java),
            dataConnection,
            Service.BIND_AUTO_CREATE
        )

        val config = Config.Instance()
        config?.runOnConnect {
            config.requestAllValuesForGroup("Options")
            config.requestAllValuesForGroup("Style")
            config.requestAllValuesForGroup("LookNFeel")
            config.requestAllValuesForGroup("Fonts")
            config.requestAllValuesForGroup("Colors")
            config.requestAllValuesForGroup("Files")
        }

        val zlibrary = getZLibrary()
        myShowStatusBarFlag = zlibrary.ShowStatusBarOption.value

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.main)
        myRootView = findViewById(R.id.root_view)
        myMainView = findViewById(R.id.main_view)
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL)

        myFBReaderApp = FBReaderApp(Paths.systemInfo(this), BookCollectionShadow())
        getCollection().bindToService(this, null)
        myBook = null

        myFBReaderApp.setWindow(this)
        myFBReaderApp.initWindow()

        myFBReaderApp.setExternalFileOpener(ExternalFileOpener(this))

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            if (myShowStatusBarFlag) 0 else WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        if (myFBReaderApp.getPopupById(TextSearchPopup.ID) == null) {
            TextSearchPopup(myFBReaderApp)
        }
        if (myFBReaderApp.getPopupById(NavigationPopup.ID) == null) {
            NavigationPopup(myFBReaderApp)
        }
        if (myFBReaderApp.getPopupById(SelectionPopup.ID) == null) {
            SelectionPopup(myFBReaderApp)
        }

        myFBReaderApp.addAction(ActionCode.SHOW_LIBRARY, ShowLibraryAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SHOW_PREFERENCES, ShowPreferencesAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SHOW_BOOK_INFO, ShowBookInfoAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SHOW_TOC, ShowTOCAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SHOW_BOOKMARKS, ShowBookmarksAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SHOW_NETWORK_LIBRARY, ShowNetworkLibraryAction(this, myFBReaderApp))

        myFBReaderApp.addAction(ActionCode.SHOW_MENU, ShowMenuAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SHOW_NAVIGATION, ShowNavigationAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SEARCH, SearchAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SHARE_BOOK, ShareBookAction(this, myFBReaderApp))

        myFBReaderApp.addAction(ActionCode.SELECTION_SHOW_PANEL, SelectionShowPanelAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SELECTION_HIDE_PANEL, SelectionHidePanelAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, SelectionCopyAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SELECTION_SHARE, SelectionShareAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SELECTION_TRANSLATE, SelectionTranslateAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.SELECTION_BOOKMARK, SelectionBookmarkAction(this, myFBReaderApp))

        myFBReaderApp.addAction(ActionCode.DISPLAY_BOOK_POPUP, DisplayBookPopupAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.PROCESS_HYPERLINK, ProcessHyperlinkAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.OPEN_VIDEO, OpenVideoAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.HIDE_TOAST, HideToastAction(this, myFBReaderApp))

        myFBReaderApp.addAction(ActionCode.SHOW_CANCEL_MENU, ShowCancelMenuAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.OPEN_START_SCREEN, StartScreenAction(this, myFBReaderApp))

        myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_SYSTEM, SetScreenOrientationAction(this, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_SYSTEM))
        myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_SENSOR, SetScreenOrientationAction(this, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_SENSOR))
        myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT, SetScreenOrientationAction(this, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_PORTRAIT))
        myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE, SetScreenOrientationAction(this, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_LANDSCAPE))
        if (getZLibrary().supportsAllOrientations()) {
            myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT, SetScreenOrientationAction(this, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_REVERSE_PORTRAIT))
            myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE, SetScreenOrientationAction(this, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_REVERSE_LANDSCAPE))
        }
        myFBReaderApp.addAction(ActionCode.OPEN_WEB_HELP, OpenWebHelpAction(this, myFBReaderApp))
        myFBReaderApp.addAction(ActionCode.INSTALL_PLUGINS, InstallPluginsAction(this, myFBReaderApp))

        myFBReaderApp.addAction(ActionCode.SWITCH_TO_DAY_PROFILE, SwitchProfileAction(this, myFBReaderApp, ColorProfile.DAY))
        myFBReaderApp.addAction(ActionCode.SWITCH_TO_NIGHT_PROFILE, SwitchProfileAction(this, myFBReaderApp, ColorProfile.NIGHT))

        val intent = intent
        val action = intent.action

        myOpenBookIntent = intent
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0) {
            if (FBReaderIntents.Action.CLOSE == action) {
                myCancelIntent = intent
                myOpenBookIntent = null
            } else if (FBReaderIntents.Action.PLUGIN_CRASH == action) {
                myFBReaderApp.externalBook = null
                myOpenBookIntent = null
                getCollection().bindToService(this) {
                    myFBReaderApp.openBook(null, null, null, myNotifier)
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        setStatusBarVisibility(true)
        setupMenu(menu)

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsMenuClosed(menu: Menu) {
        super.onOptionsMenuClosed(menu)
        setStatusBarVisibility(false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        setStatusBarVisibility(false)
        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data

        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            super.onNewIntent(intent)
        } else if (Intent.ACTION_VIEW == action && data != null && "fbreader-action" == data.scheme) {
            myFBReaderApp.runAction(data.encodedSchemeSpecificPart, data.fragment)
        } else if (Intent.ACTION_VIEW == action || FBReaderIntents.Action.VIEW == action) {
            myOpenBookIntent = intent
            if (myFBReaderApp.model == null && myFBReaderApp.externalBook != null) {
                val collection = getCollection()
                val b = FBReaderIntents.getBookExtra(intent, collection)
                val externalBook = myFBReaderApp.externalBook
                if (externalBook != null && b != null && !collection.sameBook(b, externalBook)) {
                    try {
                        val plugin = BookUtil.getPlugin(
                            PluginCollection.Instance(Paths.systemInfo(this)),
                            externalBook
                        ) as ExternalFormatPlugin
                        startActivity(PluginUtil.createIntent(plugin, FBReaderIntents.Action.PLUGIN_KILL))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else if (FBReaderIntents.Action.PLUGIN == action) {
            RunPluginAction(this, myFBReaderApp, data).checkAndRun()
        } else if (Intent.ACTION_SEARCH == action) {
            val pattern = intent.getStringExtra(SearchManager.QUERY)
            if (pattern != null) {
                val runnable = Runnable {
                    val popup = myFBReaderApp.getPopupById(TextSearchPopup.ID) as TextSearchPopup
                    popup.initPosition()
                    myFBReaderApp.miscOptions.textSearchPattern.value = pattern
                    if (myFBReaderApp.getTextView().search(pattern, true, false, false, false) != 0) {
                        runOnUiThread {
                            myFBReaderApp.showPopup(popup.id)
                        }
                    } else {
                        runOnUiThread {
                            UIMessageUtil.showErrorMessage(this@FBReader, "textNotFound")
                            popup.startPosition = null
                        }
                    }
                }
                UIUtil.wait("search", runnable, this)
            }
        } else if (FBReaderIntents.Action.CLOSE == intent.action) {
            myCancelIntent = intent
            myOpenBookIntent = null
        } else if (FBReaderIntents.Action.PLUGIN_CRASH == intent.action) {
            val book = FBReaderIntents.getBookExtra(intent, myFBReaderApp.collection)
            myFBReaderApp.externalBook = null
            myOpenBookIntent = null
            getCollection().bindToService(this) {
                val collection = getCollection()
                var b = collection.getRecentBook(0)
                if (book != null && b != null && collection.sameBook(b, book)) {
                    b = collection.getRecentBook(1)
                }
                myFBReaderApp.openBook(b, null, null, myNotifier)
            }
        } else {
            super.onNewIntent(intent)
        }
    }

    override fun onStart() {
        super.onStart()

        getCollection().bindToService(this) {
            Thread {
                getPostponedInitAction().run()
            }.start()

            myFBReaderApp.viewWidget.repaint()
        }

        initPluginActions()

        val zlibrary = getZLibrary()

        Config.Instance()?.runOnConnect {
            val showStatusBar = zlibrary.ShowStatusBarOption.value
            if (showStatusBar != myShowStatusBarFlag) {
                finish()
                startActivity(Intent(this@FBReader, FBReader::class.java))
            }
            zlibrary.ShowStatusBarOption.saveSpecialValue()
            myFBReaderApp.viewOptions.colorProfileName.saveSpecialValue()
            SetScreenOrientationAction.setOrientation(this@FBReader, getZLibrary().orientationOption.value)
        }

        (myFBReaderApp.getPopupById(TextSearchPopup.ID) as PopupPanel).setPanelInfo(this, myRootView)
        (myFBReaderApp.getPopupById(NavigationPopup.ID) as NavigationPopup).setPanelInfo(this, myRootView)
        (myFBReaderApp.getPopupById(SelectionPopup.ID) as PopupPanel).setPanelInfo(this, myRootView)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        switchWakeLock(
            hasFocus && getZLibrary().BatteryLevelToTurnScreenOffOption.value < batteryLevel
        )
    }

    private fun initPluginActions() {
        synchronized(myPluginActions) {
            var index = 0
            while (index < myPluginActions.size) {
                myFBReaderApp.removeAction(PLUGIN_ACTION_PREFIX + index++)
            }
            myPluginActions.clear()
        }

        sendOrderedBroadcast(
            Intent(PluginApi.ACTION_REGISTER),
            null,
            myPluginInfoReceiver,
            null,
            RESULT_OK,
            null,
            null
        )
    }

    override fun onResume() {
        super.onResume()

        myStartTimer = true
        Config.Instance()?.runOnConnect {
            SyncOperations.enableSync(this@FBReader, myFBReaderApp.syncOptions)

            val brightnessLevel = getZLibrary().ScreenBrightnessLevelOption.value
            if (brightnessLevel != 0) {
                viewWidget.setScreenBrightness(brightnessLevel)
            } else {
                setScreenBrightnessAuto()
            }
            if (getZLibrary().DisableButtonLightsOption.value) {
                setButtonLight(false)
            }

            getCollection().bindToService(this@FBReader) {
                val model = myFBReaderApp.model
                if (model == null || model.book == null) {
                    return@bindToService
                }
                val book = myFBReaderApp.collection.getBookById(model.book.id)
                if (book != null) {
                    onPreferencesUpdate(book)
                }
            }
        }

        registerReceiver(myBatteryInfoReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        isPaused = false
        myResumeTimestamp = System.currentTimeMillis()
        onResumeAction?.let {
            val action = it
            onResumeAction = null
            action.run()
        }

        registerReceiver(mySyncUpdateReceiver, IntentFilter(FBReaderIntents.Event.SYNC_UPDATED))

        SetScreenOrientationAction.setOrientation(this, getZLibrary().orientationOption.value)
        myCancelIntent?.let { intent ->
            myCancelIntent = null
            getCollection().bindToService(this) {
                runCancelAction(intent)
            }
            return
        } ?: myOpenBookIntent?.let { intent ->
            myOpenBookIntent = null
            getCollection().bindToService(this) {
                openBook(intent, null, true)
            }
        } ?: if (myFBReaderApp.getCurrentServerBook(null) != null) {
            getCollection().bindToService(this) {
                myFBReaderApp.useSyncInfo(true, myNotifier)
            }
        } else if (myFBReaderApp.model == null && myFBReaderApp.externalBook != null) {
            getCollection().bindToService(this) {
                myFBReaderApp.openBook(myFBReaderApp.externalBook, null, null, myNotifier)
            }
        } else {
            getCollection().bindToService(this) {
                myFBReaderApp.useSyncInfo(true, myNotifier)
            }
        }

        PopupPanel.restoreVisibilities(myFBReaderApp)
        ApiServerImplementation.sendEvent(this, ApiListener.EVENT_READ_MODE_OPENED)
    }

    override fun onPause() {
        SyncOperations.quickSync(this, myFBReaderApp.syncOptions)

        isPaused = true
        try {
            unregisterReceiver(mySyncUpdateReceiver)
        } catch (e: IllegalArgumentException) {
        }

        try {
            unregisterReceiver(myBatteryInfoReceiver)
        } catch (e: IllegalArgumentException) {
            // do nothing, this exception means that myBatteryInfoReceiver was not registered
        }

        myFBReaderApp.stopTimer()
        if (getZLibrary().DisableButtonLightsOption.value) {
            setButtonLight(true)
        }
        myFBReaderApp.onWindowClosing()

        super.onPause()
    }

    override fun onStop() {
        ApiServerImplementation.sendEvent(this, ApiListener.EVENT_READ_MODE_CLOSED)
        PopupPanel.removeAllWindows(myFBReaderApp, this)
        super.onStop()
    }

    override fun onDestroy() {
        getCollection().unbind()
        unbindService(dataConnection)
        super.onDestroy()
    }

    override fun onLowMemory() {
        myFBReaderApp.onWindowClosing()
        super.onLowMemory()
    }

    override fun onSearchRequested(): Boolean {
        val popup = myFBReaderApp.activePopup
        myFBReaderApp.hideActivePopup()
        if (DeviceType.Instance().hasStandardSearchDialog()) {
            val manager = getSystemService(SEARCH_SERVICE) as SearchManager
            manager.setOnCancelListener(object : SearchManager.OnCancelListener {
                override fun onCancel() {
                    popup?.let { myFBReaderApp.showPopup(it.id) }
                    manager.setOnCancelListener(null)
                }
            })
            startSearch(myFBReaderApp.miscOptions.textSearchPattern.value, true, null, false)
        } else {
            SearchDialogUtil.showDialog(
                this, FBReader::class.java, myFBReaderApp.miscOptions.textSearchPattern.value,
                DialogInterface.OnCancelListener { popup?.let { myFBReaderApp.showPopup(it.id) } }
            )
        }
        return true
    }

    fun showSelectionPanel() {
        val view = myFBReaderApp.getTextView()
        (myFBReaderApp.getPopupById(SelectionPopup.ID) as SelectionPopup)
            .move(view.selectionStartY, view.selectionEndY)
        myFBReaderApp.showPopup(SelectionPopup.ID)
    }

    fun hideSelectionPanel() {
        val popup = myFBReaderApp.activePopup
        if (popup != null && popup.id == SelectionPopup.ID) {
            myFBReaderApp.hideActivePopup()
        }
    }

    private fun onPreferencesUpdate(book: Book) {
        AndroidFontUtil.clearFontCache()
        myFBReaderApp.onBookUpdated(book)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_PREFERENCES -> {
                if (resultCode != RESULT_DO_NOTHING && data != null) {
                    val book = FBReaderIntents.getBookExtra(data, myFBReaderApp.collection)
                    if (book != null) {
                        getCollection().bindToService(this) {
                            onPreferencesUpdate(book)
                        }
                    }
                }
            }
            REQUEST_CANCEL_MENU -> {
                data?.let { runCancelAction(it) }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun runCancelAction(intent: Intent) {
        val type: CancelMenuHelper.ActionType = try {
            CancelMenuHelper.ActionType.valueOf(
                intent.getStringExtra(FBReaderIntents.Key.TYPE) ?: return
            )
        } catch (e: Exception) {
            // invalid (or null) type value
            return
        }
        var bookmark: Bookmark? = null
        if (type == CancelMenuHelper.ActionType.returnTo) {
            bookmark = FBReaderIntents.getBookmarkExtra(intent)
            if (bookmark == null) {
                return
            }
        }
        myFBReaderApp.runCancelAction(type, bookmark)
    }

    fun navigate() {
        (myFBReaderApp.getPopupById(NavigationPopup.ID) as NavigationPopup).runNavigation()
    }

    private fun addSubmenu(menu: Menu, id: String): Menu {
        return menu.addSubMenu(ZLResource.resource("menu").getResource(id).value)
    }

    private fun addMenuItem(menu: Menu, actionId: String, iconId: Int?, name: String?) {
        val itemName = name ?: ZLResource.resource("menu").getResource(actionId).value
        val menuItem = menu.add(itemName)
        iconId?.let { menuItem.setIcon(it) }
        menuItem.setOnMenuItemClickListener(myMenuListener)
        myMenuItemMap[menuItem] = actionId
    }

    private fun addMenuItem(menu: Menu, actionId: String, name: String) {
        addMenuItem(menu, actionId, null, name)
    }

    private fun addMenuItem(menu: Menu, actionId: String, iconId: Int) {
        addMenuItem(menu, actionId, iconId, null)
    }

    private fun addMenuItem(menu: Menu, actionId: String) {
        addMenuItem(menu, actionId, null, null)
    }

    private fun fillMenu(menu: Menu, nodes: List<MenuNode>) {
        for (n in nodes) {
            if (n is MenuNode.Item) {
                val iconId = n.iconId
                if (iconId != null) {
                    addMenuItem(menu, n.code, iconId)
                } else {
                    addMenuItem(menu, n.code)
                }
            } else {
                val submenu = addSubmenu(menu, n.code)
                fillMenu(submenu, (n as MenuNode.Submenu).children)
            }
        }
    }

    private fun setupMenu(menu: Menu) {
        val menuLanguage = ZLResource.getLanguageOption().value
        if (menuLanguage == myMenuLanguage) {
            return
        }
        myMenuLanguage = menuLanguage

        menu.clear()
        fillMenu(menu, MenuData.topLevelNodes())
        synchronized(myPluginActions) {
            var index = 0
            for (info in myPluginActions) {
                if (info is PluginApi.MenuActionInfo) {
                    addMenuItem(
                        menu,
                        PLUGIN_ACTION_PREFIX + index++,
                        info.menuItemName ?: ""
                    )
                }
            }
        }

        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        setupMenu(menu)

        return true
    }

    protected fun onPluginNotFound(book: Book) {
        val collection = getCollection()
        collection.bindToService(this) {
            val recent = collection.getRecentBook(0)
            if (recent != null && !collection.sameBook(recent, book)) {
                myFBReaderApp.openBook(recent, null, null, null)
            } else {
                myFBReaderApp.openHelpBook()
            }
        }
    }

    fun onPluginNotFoundInternal(book: Book) {
        onPluginNotFound(book)
    }

    private fun setStatusBarVisibility(visible: Boolean) {
        val zlibrary = getZLibrary()
        if (DeviceType.Instance() != DeviceType.KINDLE_FIRE_1ST_GENERATION && !myShowStatusBarFlag) {
            if (visible) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return (::myMainView.isInitialized && myMainView.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return (::myMainView.isInitialized && myMainView.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event)
    }

    private fun setButtonLight(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            setButtonLightInternal(enabled)
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private fun setButtonLightInternal(enabled: Boolean) {
        val attrs = window.attributes
        attrs.buttonBrightness = if (enabled) -1.0f else 0.0f
        window.attributes = attrs
    }

    fun createWakeLock() {
        if (myWakeLockToCreate) {
            synchronized(this) {
                if (myWakeLockToCreate) {
                    myWakeLockToCreate = false
                    myWakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
                        .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "FBReader")
                    myWakeLock!!.acquire()
                }
            }
        }
        if (myStartTimer) {
            myFBReaderApp.startTimer()
            myStartTimer = false
        }
    }

    private fun switchWakeLock(on: Boolean) {
        if (on) {
            if (myWakeLock == null) {
                myWakeLockToCreate = true
            }
        } else {
            myWakeLock?.let {
                synchronized(this) {
                    myWakeLock?.release()
                    myWakeLock = null
                }
            }
        }
    }

    private fun getCollection(): BookCollectionShadow {
        return myFBReaderApp.collection as BookCollectionShadow
    }

    // methods from ZLApplicationWindow interface
    override fun showErrorMessage(key: String) {
        UIMessageUtil.showErrorMessage(this, key)
    }

    override fun showErrorMessage(key: String, parameter: String) {
        UIMessageUtil.showErrorMessage(this, key, parameter)
    }

    override fun createExecutor(key: String): ZLApplication.SynchronousExecutor {
        return UIUtil.createExecutor(this, key)
    }

    override val batteryLevel: Int
        get() = myBatteryLevel

    private fun setBatteryLevel(percent: Int) {
        myBatteryLevel = percent
    }

    override fun close() {
        finish()
    }

    override val viewWidget: ZLViewWidget
        get() = myMainView

    override fun refresh() {
        runOnUiThread {
            for ((menuItem, actionId) in myMenuItemMap) {
                menuItem.isVisible = myFBReaderApp.isActionVisible(actionId) && myFBReaderApp.isActionEnabled(actionId)
                when (myFBReaderApp.isActionChecked(actionId)) {
                    Boolean3.TRUE -> {
                        menuItem.isCheckable = true
                        menuItem.isChecked = true
                    }
                    Boolean3.FALSE -> {
                        menuItem.isCheckable = true
                        menuItem.isChecked = false
                    }
                    Boolean3.UNDEFINED -> {
                        menuItem.isCheckable = false
                    }
                }
            }
        }
    }

    override fun processException(exception: Exception) {
        exception.printStackTrace()

        val intent = Intent(
            FBReaderIntents.Action.ERROR,
            Uri.Builder().scheme(exception.javaClass.simpleName).build()
        )
        intent.setPackage(FBReaderIntents.DEFAULT_PACKAGE)
        intent.putExtra(ErrorKeys.MESSAGE, exception.message)
        val stackTrace = StringWriter()
        exception.printStackTrace(PrintWriter(stackTrace))
        intent.putExtra(ErrorKeys.STACKTRACE, stackTrace.toString())
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // ignore
            e.printStackTrace()
        }
    }

    override fun setWindowTitle(title: String) {
        runOnUiThread {
            setTitle(title)
        }
    }

    fun outlineRegion(soul: ZLTextRegion.Soul) {
        myFBReaderApp.getTextView().outlineRegion(soul)
        myFBReaderApp.viewWidget.repaint()
    }

    fun hideOutline() {
        myFBReaderApp.getTextView().hideOutline()
        myFBReaderApp.viewWidget.repaint()
    }

    override fun hideDictionarySelection() {
        myFBReaderApp.getTextView().hideOutline()
        myFBReaderApp.getTextView().removeHighlightings(DictionaryHighlighting::class.java)
        myFBReaderApp.viewWidget.reset()
        myFBReaderApp.viewWidget.repaint()
    }

    private fun getPostponedInitAction(): Runnable {
        return Runnable {
            runOnUiThread {
                TipRunner().start()
                DictionaryUtil.init(this@FBReader, null)
                val intent = intent
                if (intent != null && FBReaderIntents.Action.PLUGIN == intent.action) {
                    RunPluginAction(this@FBReader, myFBReaderApp, intent.data).checkAndRun()
                }
            }
        }
    }

    @Synchronized
    private fun openBook(intent: Intent, action: Runnable?, force: Boolean) {
        if (!force && myBook != null) {
            return
        }

        myBook = FBReaderIntents.getBookExtra(intent, myFBReaderApp.collection)
        val bookmark = FBReaderIntents.getBookmarkExtra(intent)
        if (myBook == null) {
            val data = intent.data
            if (data != null) {
                myBook = createBookForFile(ZLFile.createFileByPath(data.path))
            }
        }
        myBook?.let { book ->
            var file = BookUtil.fileByBook(book)
            if (!file.exists()) {
                if (file.physicalFile != null) {
                    file = file.physicalFile
                }
                UIMessageUtil.showErrorMessage(this, "fileNotFound", file.path)
                myBook = null
            } else {
                NotificationUtil.drop(this, book)
            }
        }
        Config.Instance()?.runOnConnect {
            myFBReaderApp.openBook(myBook, bookmark, action, myNotifier)
            AndroidFontUtil.clearFontCache()
        }
    }

    private fun createBookForFile(file: ZLFile?): Book? {
        if (file == null) {
            return null
        }
        var book = myFBReaderApp.collection.getBookByFile(file.path)
        if (book != null) {
            return book
        }
        if (file.isArchive) {
            for (child in file.children()) {
                book = myFBReaderApp.collection.getBookByFile(child.path)
                if (book != null) {
                    return book
                }
            }
        }
        return null
    }

    private inner class TipRunner : Thread() {
        init {
            priority = MIN_PRIORITY
        }

        override fun run() {
            val manager = TipsManager(Paths.systemInfo(this@FBReader))
            when (manager.requiredAction()) {
                TipsManager.Action.Initialize -> startActivity(
                    Intent(TipsActivity.INITIALIZE_ACTION, null, this@FBReader, TipsActivity::class.java)
                )
                TipsManager.Action.Show -> startActivity(
                    Intent(TipsActivity.SHOW_TIP_ACTION, null, this@FBReader, TipsActivity::class.java)
                )
                TipsManager.Action.Download -> manager.startDownloading()
                TipsManager.Action.None -> {}
            }
        }
    }

    companion object {
        @JvmField
        val RESULT_DO_NOTHING: Int = RESULT_FIRST_USER
        @JvmField
        val RESULT_REPAINT: Int = RESULT_FIRST_USER + 1
        private const val PLUGIN_ACTION_PREFIX = "___"

        fun defaultIntent(context: Context): Intent {
            return Intent(context, FBReader::class.java)
                .setAction(FBReaderIntents.Action.VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        fun openBookActivity(context: Context, book: Book, bookmark: Bookmark?) {
            val intent = defaultIntent(context)
            FBReaderIntents.putBookExtra(intent, book)
            if (bookmark != null) {
                FBReaderIntents.putBookmarkExtra(intent, bookmark)
            }
            context.startActivity(intent)
        }
    }
}
