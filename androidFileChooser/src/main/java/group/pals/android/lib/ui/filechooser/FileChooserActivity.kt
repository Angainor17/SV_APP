/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import group.pals.android.R
import group.pals.android.lib.ui.filechooser.FileChooserActivity.Companion._Rootpath
import group.pals.android.lib.ui.filechooser.FileChooserActivity.Companion._SelectFile
import group.pals.android.lib.ui.filechooser.io.IFile
import group.pals.android.lib.ui.filechooser.io.IFileFilter
import group.pals.android.lib.ui.filechooser.prefs.DisplayPrefs
import group.pals.android.lib.ui.filechooser.services.FileProviderService
import group.pals.android.lib.ui.filechooser.services.IFileProvider
import group.pals.android.lib.ui.filechooser.services.IFileProvider.FilterMode
import group.pals.android.lib.ui.filechooser.services.IFileProvider.SortOrder
import group.pals.android.lib.ui.filechooser.services.IFileProvider.SortType
import group.pals.android.lib.ui.filechooser.services.LocalFileProvider
import group.pals.android.lib.ui.filechooser.utils.ActivityCompat
import group.pals.android.lib.ui.filechooser.utils.E
import group.pals.android.lib.ui.filechooser.utils.FileComparator
import group.pals.android.lib.ui.filechooser.utils.FileUtils
import group.pals.android.lib.ui.filechooser.utils.Ui
import group.pals.android.lib.ui.filechooser.utils.Utils
import group.pals.android.lib.ui.filechooser.utils.history.History
import group.pals.android.lib.ui.filechooser.utils.history.HistoryFilter
import group.pals.android.lib.ui.filechooser.utils.history.HistoryListener
import group.pals.android.lib.ui.filechooser.utils.history.HistoryStore
import group.pals.android.lib.ui.filechooser.utils.ui.Dlg
import group.pals.android.lib.ui.filechooser.utils.ui.LoadingDialog
import group.pals.android.lib.ui.filechooser.utils.ui.TaskListener
import group.pals.android.lib.ui.filechooser.utils.ui.ViewFilesContextMenuUtils
import java.io.File
import java.util.Collections

/**
 * Главная активность этой библиотеки.
 *
 * **Примечания:**
 *
 * **I.** О ключах [_Rootpath], [_SelectFile] и настройке [DisplayPrefs.isRememberLastLocation],
 * приоритеты следующие:
 * 1. [_SelectFile]
 * 2. [DisplayPrefs.isRememberLastLocation]
 * 3. [_Rootpath]
 *
 * @author Hai Bison
 */
class FileChooserActivity : Activity() {

    companion object {
        /** Полное имя этого класса. Обычно используется для отладки. */
        const val _ClassName = "FileChooserActivity"

        /**
         * Устанавливает значение этого ключа в тему из `android.R.style.Theme_*`.
         *
         * @since v4.3 beta
         */
        const val _Theme = "$_ClassName.theme"

        /**
         * Ключ для хранения корневого пути.
         * Если используется [LocalFileProvider], то по умолчанию sdcard, если sdcard недоступна, будет использована "/".
         *
         * **Примечание:** Значение этого ключа - [IFile].
         */
        const val _Rootpath = "$_ClassName.rootpath"

        /**
         * Ключ для хранения класса сервиса, реализующего [IFileProvider].
         * По умолчанию [LocalFileProvider].
         */
        const val _FileProviderClass = "$_ClassName.file_provider_class"

        /**
         * Ключ для хранения [IFileProvider.FilterMode], по умолчанию [IFileProvider.FilterMode.FilesOnly].
         */
        @JvmField
        val _FilterMode = IFileProvider.FilterMode::class.java.name

        /**
         * Ключ для хранения максимального количества файлов, по умолчанию `1024`.
         */
        const val _MaxFileCount = "$_ClassName.max_file_count"

        /**
         * Ключ для хранения режима множественного выбора, по умолчанию `false`.
         */
        const val _MultiSelection = "$_ClassName.multi_selection"

        /**
         * Ключ для хранения regex фильтра имён файлов, по умолчанию `null`.
         */
        const val _RegexFilenameFilter = "$_ClassName.regex_filename_filter"

        /**
         * Ключ для хранения отображения скрытых файлов, по умолчанию `false`.
         */
        const val _DisplayHiddenFiles = "$_ClassName.display_hidden_files"

        /**
         * Установите в `true`, чтобы включить двойной тап для выбора файлов/директорий.
         * С версии v4.7 beta одинарный тап используется по умолчанию.
         *
         * @since v4.7 beta
         */
        const val _DoubleTapToChooseFiles = "$_ClassName.double_tap_to_choose_files"

        /**
         * Устанавливает файл, который нужно выбрать при запуске этой активности.
         *
         * @since v4.7 beta
         */
        const val _SelectFile = "$_ClassName.select_file"

        const val _TextResources = "$_ClassName.text_resources"
        const val _ShowNewFolderButton = "$_ClassName.show_new_folder_button"
        const val _FilenameRegExp = "$_ClassName.file_regexp"

        /**
         * Ключ для хранения режима диалога сохранения, по умолчанию `false`.
         */
        const val _SaveDialog = "$_ClassName.save_dialog"

        const val _ActionBar = "$_ClassName.action_bar"

        /**
         * Ключ для хранения имени файла по умолчанию, по умолчанию `null`.
         */
        const val _DefaultFilename = "$_ClassName.default_filename"

        /**
         * Ключ для хранения результатов (может быть один или несколько файлов).
         */
        const val _Results = "$_ClassName.results"

        const val _FileSelectionMode = "$_ClassName.file_selection_mode"
        const val _FolderPath = "$_ClassName.folder_path"
        const val _SaveLastLocation = "$_ClassName.save_last_location"

        /**
         * Этот ключ хранит текущее местоположение ([IFile]) для восстановления
         * после изменения ориентации экрана.
         */
        internal const val CURRENT_LOCATION = "\$_ClassName.current_location"

        /**
         * Этот ключ хранит текущую историю ([History]<[IFile]>) для восстановления
         * после изменения ориентации экрана.
         */
        internal const val HISTORY = "\$_ClassName.history"

        /**
         * Этот ключ хранит полную историю ([History]<[IFile]>) для восстановления
         * после изменения ориентации экрана.
         */
        internal val FULL_HISTORY = "${History::class.java.getName()}_full"

        private val BTN_SORT_IDS = intArrayOf(
            R.id.afc_settings_sort_view_button_sort_by_name_asc,
            R.id.afc_settings_sort_view_button_sort_by_name_desc,
            R.id.afc_settings_sort_view_button_sort_by_size_asc,
            R.id.afc_settings_sort_view_button_sort_by_size_desc,
            R.id.afc_settings_sort_view_button_sort_by_date_asc,
            R.id.afc_settings_sort_view_button_sort_by_date_desc
        )
    }

    // ====================
    // "ПОСТОЯННЫЕ" ПЕРЕМЕННЫЕ

    private val btnCancelActionBarOnClickListener = View.OnClickListener {
        finish()
    }

    private var fileProviderServiceClass: Class<*>? = null

    /** Провайдер файлов. */
    private var fileProvider: IFileProvider? = null

    /** Соединение с сервисом. */
    private var serviceConnection: ServiceConnection? = null

    private var root: IFile? = null
    private var isMultiSelection = false
    private var isSaveDialog = false
    private var isActionBar = false
    private var isSaveLastLocation = false
    private var doubleTapToChooseFiles = false
    private var toast: Toast? = null

    /** История. */
    private var history: HistoryStore<IFile>? = null

    /** Полная история для хранения и показа пользователю всех посещённых мест. */
    private var fullHistory: HistoryStore<IFile>? = null

    /** Адаптер списка. */
    private var fileAdapter: IFileAdapter? = null

    // Элементы управления
    private var viewLocationsContainer: HorizontalScrollView? = null
    private var viewLocations: ViewGroup? = null

    private val btnLocationOnLongClickListener = View.OnLongClickListener { v ->
        if (IFileProvider.FilterMode.FilesOnly == fileProvider?.filterMode || isSaveDialog) {
            return@OnLongClickListener false
        }

        doFinish(v.tag as IFile)
        false
    }

    private val btnOkActionBarOnClickListener = View.OnClickListener {
        val location = location
        if (location is File) {
            if (fileProvider?.filterMode != IFileProvider.FilterMode.AnyDirectories) {
                if (!location.canWrite()) {
                    Dlg.toast(this@FileChooserActivity, R.string.afc_msg_app_cant_choose_folder, Dlg.LENGTH_SHORT)
                    return@OnClickListener
                }
            }
        }
        doFinish()
        finish()
    }

    private val viewFilesOnItemLongClickListener = AdapterView.OnItemLongClickListener { _, view, position, _ ->
        val data = fileAdapter?.getItem(position) ?: return@OnItemLongClickListener true

        if (doubleTapToChooseFiles) {
            // Ничего не делаем
        } else {
            if (!isSaveDialog && !isMultiSelection && data.file.isDirectory()&&
                (IFileProvider.FilterMode.DirectoriesOnly == fileProvider?.filterMode ||
                    IFileProvider.FilterMode.FilesAndDirectories == fileProvider?.filterMode)
            ) {
                doFinish(data.file)
            }
        }

        true
    }

    private var viewFilesContainer: ViewGroup? = null
    private var txtFullDirName: TextView? = null
    private var viewFiles: AbsListView? = null

    private val btnSaveOpenDialogOnClickListener = View.OnClickListener {
        val list = ArrayList<IFile>()
        for (i in 0 until viewFiles!!.adapter.count) {
            val obj = viewFiles!!.adapter.getItem(i)
            if (obj is IFileDataModel) {
                if (obj.isSelected) {
                    list.add(obj.file)
                }
            }
        }
        doFinish(list)
    }

    private var footerView: TextView? = null
    private var btnSave: Button? = null
    private var btnOk: Button? = null
    private var btnCancel: Button? = null
    private var txtSaveas: EditText? = null

    private val txtFilenameOnEditorActionListener = TextView.OnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            Ui.hideSoftKeyboard(this@FileChooserActivity, txtSaveas?.windowToken)
            btnSave?.performClick()
            return@OnEditorActionListener true
        }
        false
    }

    private val btnSaveSaveDialogOnClickListener = View.OnClickListener {
        Ui.hideSoftKeyboard(this@FileChooserActivity, txtSaveas?.windowToken)
        val filename = txtSaveas?.text?.toString()?.trim() ?: ""
        doCheckSaveasFilenameAndFinish(filename)
    }

    private var viewGoBack: ImageView? = null
    private var viewGoForward: ImageView? = null
    private var viewCreateFolder: ImageView? = null
    private var viewFoldersView: ImageView? = null
    private var viewSort: ImageView? = null
    private var textResources: HashMap<String, String>? = null
    private var filenameRegexp: String? = null

    // ====================
    // СЛУШАТЕЛИ КНОПОК

    private val btnSortOnClickListener = View.OnClickListener {
        doResortViewFiles()
    }

    private val btnCreateFolderOnClickListener = View.OnClickListener {
        doCreateNewDir()
    }

    private val btnLocationOnClickListener = View.OnClickListener { v ->
        if (v.tag is IFile) {
            goTo(v.tag as IFile)
        }
    }

    private val viewFilesOnItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
        val data = fileAdapter?.getItem(position) ?: return@OnItemClickListener

        if (data.file.isDirectory()) {
            goTo(data.file)
            return@OnItemClickListener
        }

        if (isSaveDialog) {
            txtSaveas?.setText(data.file.getName())
        }

        if (doubleTapToChooseFiles) {
            return@OnItemClickListener
        } else {
            if (isMultiSelection) {
                return@OnItemClickListener
            }

            if (isSaveDialog) {
                doCheckSaveasFilenameAndFinish(data.file.getName())
            } else {
                val bag = view.tag as? IFileAdapter.Bag
                if (bag != null && bag.isAccessible) {
                    doFinish(data.file)
                }
            }
        }
    }

    private val btnGoBackOnClickListener = View.OnClickListener {
        // Если пользователь удалил директорию, которая была в истории,
        // могут быть дубликаты, проверяем и удаляем их
        val currentLoc = location ?: return@OnClickListener
        var preLoc: IFile? = null
        while (currentLoc.equalsToPath(history?.prevOf(currentLoc).also { preLoc = it })) {
            history?.remove(preLoc!!)
        }

        if (preLoc != null) {
            setLocation(preLoc, object : TaskListener {
                override fun onFinish(ok: Boolean, any: Any?) {
                    if (ok) {
                        viewGoBack?.isEnabled = history?.prevOf(location!!) != null
                        viewGoForward?.isEnabled = true
                        fullHistory?.push(any as IFile)
                    }
                }
            })
        } else {
            viewGoBack?.isEnabled = false
        }
    }

    private val btnGoForwardOnClickListener = View.OnClickListener {
        val currentLoc = location ?: return@OnClickListener
        var nextLoc: IFile? = null
        while (currentLoc.equalsToPath(history?.nextOf(currentLoc).also { nextLoc = it })) {
            history?.remove(nextLoc!!)
        }

        if (nextLoc != null) {
            setLocation(nextLoc, object : TaskListener {
                override fun onFinish(ok: Boolean, any: Any?) {
                    if (ok) {
                        viewGoBack?.isEnabled = true
                        viewGoForward?.isEnabled = history?.nextOf(location!!) != null
                        fullHistory?.push(any as IFile)
                    }
                }
            })
        } else {
            viewGoForward?.isEnabled = false
        }
    }

    private val btnGoBackForwardOnLongClickListener = View.OnLongClickListener {
        ViewFilesContextMenuUtils.doShowHistoryContents(
            this@FileChooserActivity,
            fileProvider!!,
            fullHistory!!,
            location,
            object : TaskListener {
                override fun onFinish(ok: Boolean, any: Any?) {
                    history?.removeAll(object : HistoryFilter<IFile> {
                        override fun accept(item: IFile): Boolean {
                            return fullHistory!!.indexOf(item) < 0
                        }
                    })

                    if (any is IFile) {
                        setLocation(any, object : TaskListener {
                            override fun onFinish(ok: Boolean, any: Any?) {
                                if (ok) {
                                    history?.notifyHistoryChanged()
                                }
                            }
                        })
                    } else if (history?.isEmpty() == true) {
                        history?.push(location!!)
                        fullHistory?.push(location!!)
                    }
                }
            })
        false
    }

    private var listviewFilesGestureDetector: GestureDetector? = null

    @SuppressLint("ResourceType")
    private val btnFoldersViewOnClickListener = View.OnClickListener {
        doSwitchViewType()
        if (viewFoldersView?.id == R.drawable.afc_selector_button_folders_view_list) {
            viewFoldersView?.setImageDrawable(resources.getDrawable(R.drawable.afc_selector_button_folders_view_grid))
            viewFoldersView?.setId(R.drawable.afc_selector_button_folders_view_grid)
        } else {
            viewFoldersView?.setImageDrawable(resources.getDrawable(R.drawable.afc_selector_button_folders_view_list))
            viewFoldersView?.setId(R.drawable.afc_selector_button_folders_view_list)
        }
    }

    /**
     * Вызывается при первом создании активности.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.afc_file_chooser)

        initGestureDetector()

        // Загружаем конфигурации
        fileProviderServiceClass = intent.getSerializableExtra(_FileProviderClass) as Class<*>?
        if (fileProviderServiceClass == null) {
            fileProviderServiceClass = LocalFileProvider::class.java
        }

        isMultiSelection = intent.getBooleanExtra(_MultiSelection, false)
        isActionBar = intent.getBooleanExtra(_ActionBar, false)
        isSaveDialog = intent.getBooleanExtra(_SaveDialog, false)
        if (isSaveDialog) {
            isMultiSelection = false
        }

        isSaveLastLocation = intent.getBooleanExtra(_SaveLastLocation, true)
        if (!isSaveLastLocation) {
            DisplayPrefs.setRememberLastLocation(this, false)
        }
        doubleTapToChooseFiles = intent.getBooleanExtra(_DoubleTapToChooseFiles, false)

        @Suppress("UNCHECKED_CAST")
        textResources = intent.getSerializableExtra(_TextResources) as? HashMap<String, String>

        // Загружаем элементы управления
        viewSort = findViewById(R.id.afc_filechooser_activity_button_sort)
        viewFoldersView = findViewById(R.id.afc_filechooser_activity_button_folders_view)
        viewCreateFolder = findViewById(R.id.afc_filechooser_activity_button_create_folder)
        if (!intent.getBooleanExtra(_ShowNewFolderButton, true)) {
            viewCreateFolder?.visibility = View.GONE
        }
        filenameRegexp = intent.getStringExtra(_FilenameRegExp)
        viewGoBack = findViewById(R.id.afc_filechooser_activity_button_go_back)
        viewGoForward = findViewById(R.id.afc_filechooser_activity_button_go_forward)
        viewLocations = findViewById(R.id.afc_filechooser_activity_view_locations)
        viewLocationsContainer = findViewById(R.id.afc_filechooser_activity_view_locations_container)
        txtFullDirName = findViewById(R.id.afc_filechooser_activity_textview_full_dir_name)
        viewFilesContainer = findViewById(R.id.afc_filechooser_activity_view_files_container)
        footerView = findViewById(R.id.afc_filechooser_activity_view_files_footer_view)
        txtSaveas = findViewById(R.id.afc_filechooser_activity_textview_saveas_filename)
        btnSave = findViewById(R.id.afc_filechooser_activity_button_save)
        btnOk = findViewById(R.id.afc_filechooser_activity_button_ok)
        btnCancel = findViewById(R.id.afc_filechooser_activity_button_cancel)

        // История
        if (savedInstanceState != null && savedInstanceState.get(HISTORY) is HistoryStore<*>) {
            @Suppress("UNCHECKED_CAST")
            history = savedInstanceState.getParcelable(HISTORY)
        } else {
            history = HistoryStore(DisplayPrefs.DEF_HISTORY_CAPACITY)
        }
        history?.addListener(object : HistoryListener<IFile> {
            override fun onChanged(history: History<IFile>) {
                val idx = history.indexOf(location!!)
                viewGoBack?.isEnabled = idx > 0
                viewGoForward?.isEnabled = idx >= 0 && idx < history.size() - 1
            }
        })

        // Полная история
        if (savedInstanceState != null && savedInstanceState.get(FULL_HISTORY) is HistoryStore<*>) {
            @Suppress("UNCHECKED_CAST")
            fullHistory = savedInstanceState.getParcelable(FULL_HISTORY)
        } else {
            fullHistory = object : HistoryStore<IFile>(DisplayPrefs.DEF_HISTORY_CAPACITY) {
                override fun push(newItem: IFile) {
                    val i = indexOf(newItem)
                    if (i >= 0) {
                        if (i == size() - 1) return
                        remove(newItem)
                    }
                    super.push(newItem)
                }
            }
        }

        // Убеждаемся, что RESULT_CANCELED - значение по умолчанию
        setResult(RESULT_CANCELED)

        bindService(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.afc_file_chooser_activity, menu)
        var item = menu.findItem(R.id.afc_filechooser_activity_menuitem_home)
        item?.title = textResources?.get("menuOrigin")
        item = menu.findItem(R.id.afc_filechooser_activity_menuitem_reload)
        item?.title = textResources?.get("menuReload")
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean = true

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.afc_filechooser_activity_menuitem_home -> doGoHome()
            R.id.afc_filechooser_activity_menuitem_reload -> doReloadCurrentLocation()
        }
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(CURRENT_LOCATION, location)
        outState.putParcelable(HISTORY, history)
        outState.putParcelable(FULL_HISTORY, fullHistory)
    }

    override fun onStart() {
        super.onStart()
        if (!isMultiSelection && !isSaveDialog && doubleTapToChooseFiles) {
            Dlg.toast(this, R.string.afc_hint_double_tap_to_select_file, Dlg.LENGTH_SHORT)
        }
    }

    override fun onBackPressed() {
        val currentLoc = location ?: run {
            super.onBackPressed()
            return
        }
        if (history == null) {
            super.onBackPressed()
            return
        }

        var preLoc: IFile? = null
        while (currentLoc.equalsToPath(history?.prevOf(currentLoc).also { preLoc = it })) {
            history?.remove(preLoc!!)
        }
        if (preLoc != null) {
            goTo(preLoc)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (fileProvider != null) {
            try {
                unbindService(serviceConnection!!)
            } catch (t: Throwable) {
                Log.e(_ClassName, "onDestroy() - unbindService() - exception: $t")
            }

            try {
                stopService(Intent(this, fileProviderServiceClass!!))
            } catch (e: SecurityException) {
                // У нас есть разрешение на остановку нашего сервиса
            }
        }

        super.onDestroy()
    }

    /**
     * Подключается к сервису провайдера файлов, затем загружает корневую директорию.
     */
    private fun bindService(savedInstanceState: Bundle?) {
        if (startService(Intent(this, fileProviderServiceClass!!)) == null) {
            doShowCannotConnectToServiceAndFinish()
            return
        }

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                try {
                    fileProvider = (service as FileProviderService.LocalBinder).service
                } catch (t: Throwable) {
                    Log.e(_ClassName, "serviceConnection.onServiceConnected() -> $t")
                }
            }

            override fun onServiceDisconnected(className: ComponentName) {
                fileProvider = null
            }
        }

        bindService(Intent(this, fileProviderServiceClass!!), serviceConnection!!, Context.BIND_AUTO_CREATE)

        object : LoadingDialog(this@FileChooserActivity as Context, getString(R.string.afc_msg_loading), false) {
            private val WAIT_TIME = 200
            private val MAX_WAIT_TIME = 3000 // 3 секунды

            override fun doInBackground(vararg params: Void): Any? {
                var totalWaitTime = 0
                while (fileProvider == null) {
                    try {
                        totalWaitTime += WAIT_TIME
                        Thread.sleep(WAIT_TIME.toLong())
                        if (totalWaitTime >= MAX_WAIT_TIME) break
                    } catch (e: InterruptedException) {
                        break
                    }
                }
                return null
            }

            override fun onPostExecute(result: Any?) {
                super.onPostExecute(result)

                if (fileProvider == null) {
                    doShowCannotConnectToServiceAndFinish()
                } else {
                    setupService()
                    setupHeader()
                    setupViewFiles()
                    setupFooter()

                    /*
                     * Приоритеты для начального пути:
                     * 1. Текущее местоположение (если активность была уничтожена после изменения конфигурации).
                     * 2. Выбранный файл из ключа _SelectFile.
                     * 3. Последнее местоположение.
                     * 4. Корневой путь из ключа _Rootpath.
                     */

                    // Текущее местоположение
                    var path: IFile? = savedInstanceState?.get(CURRENT_LOCATION) as? IFile

                    // Выбранный файл
                    var selectedFile: IFile? = null
                    if (path == null) {
                        selectedFile = intent.getParcelableExtra(_SelectFile)
                        if (selectedFile != null && selectedFile.exists()) {
                            path = selectedFile.parentFile()
                        }
                        if (path == null) {
                            selectedFile = null
                        }
                    }

                    // Последнее местоположение
                    if (path == null && DisplayPrefs.isRememberLastLocation(this@FileChooserActivity)) {
                        val lastLocation = DisplayPrefs.getLastLocation(this@FileChooserActivity)
                        if (lastLocation != null) {
                            path = fileProvider!!.fromPath(lastLocation)
                        }
                    }

                    val finalSelectedFile = selectedFile

                    // Или корневой путь
                    setLocation(
                        if (path != null && path.isDirectory()) path else root!!,
                        object : TaskListener {
                            override fun onFinish(ok: Boolean, any: Any?) {
                                if (ok && finalSelectedFile != null && finalSelectedFile.isFile()&& isSaveDialog) {
                                    txtSaveas?.setText(finalSelectedFile.getName())
                                }

                                val isCurrentLocation = savedInstanceState != null &&
                                    any == savedInstanceState.get(CURRENT_LOCATION)
                                if (isCurrentLocation) {
                                    history?.notifyHistoryChanged()
                                } else {
                                    history?.push(any as IFile)
                                    fullHistory?.push(any as IFile)
                                }
                            }
                        },
                        selectedFile
                    )
                }
            }
        }.execute()
    }

    /**
     * Настраивает провайдер файлов:
     * - режим фильтрации;
     * - отображение скрытых файлов;
     * - максимальное количество файлов;
     * - ...
     */
    private fun setupService() {
        // Устанавливаем корневой путь
        if (intent.getParcelableExtra<IFile>(_Rootpath) != null) {
            root = intent.getSerializableExtra(_Rootpath) as? IFile
        }
        if (root == null || !root!!.isDirectory()) {
            root = fileProvider!!.defaultPath()
        }

        var filterMode = intent.getSerializableExtra(_FilterMode) as? IFileProvider.FilterMode
        if (filterMode == null) {
            filterMode = IFileProvider.FilterMode.DirectoriesOnly
        }

        val sortType = DisplayPrefs.getSortType(this)
        val sortAscending = DisplayPrefs.isSortAscending(this)

        fileProvider!!.displayHiddenFiles = intent.getBooleanExtra(_DisplayHiddenFiles, false)
        fileProvider!!.filterModeValue = if (isSaveDialog) IFileProvider.FilterMode.FilesOnly else filterMode
        fileProvider!!.maxFileCountValue = intent.getIntExtra(_MaxFileCount, 1024)
        fileProvider!!.regexFilenameFilterValue = intent.getStringExtra(_RegexFilenameFilter)
        fileProvider!!.sortOrderValue = if (sortAscending) SortOrder.Ascending else SortOrder.Descending
        fileProvider!!.sortTypeValue = sortType
    }

    /**
     * Настраивает:
     * - заголовок активности;
     * - кнопку "назад";
     * - кнопку местоположения;
     * - кнопку "вперёд".
     */
    @SuppressLint("ResourceType")
    private fun setupHeader() {
        title = textResources?.get("title")

        viewSort?.setOnClickListener(btnSortOnClickListener)
        if (DisplayPrefs.isSortAscending(this)) {
            viewSort?.setImageDrawable(resources.getDrawable(R.drawable.afc_selector_button_sort_as))
            viewSort?.setId(R.drawable.afc_selector_button_sort_as)
        } else {
            viewSort?.setImageDrawable(resources.getDrawable(R.drawable.afc_selector_button_sort_de))
            viewSort?.setId(R.drawable.afc_selector_button_sort_de)
        }

        viewFoldersView?.setOnClickListener(btnFoldersViewOnClickListener)
        when (DisplayPrefs.getViewType(this)) {
            ViewType.List -> {
                viewFoldersView?.setImageDrawable(resources.getDrawable(R.drawable.afc_selector_button_folders_view_grid))
                viewFoldersView?.setId(R.drawable.afc_selector_button_folders_view_grid)
            }
            ViewType.Grid -> {
                viewFoldersView?.setImageDrawable(resources.getDrawable(R.drawable.afc_selector_button_folders_view_list))
                viewFoldersView?.setId(R.drawable.afc_selector_button_folders_view_list)
            }
        }
        viewCreateFolder?.setOnClickListener(btnCreateFolderOnClickListener)

        viewGoBack?.isEnabled = false
        viewGoBack?.setOnClickListener(btnGoBackOnClickListener)

        viewGoForward?.isEnabled = false
        viewGoForward?.setOnClickListener(btnGoForwardOnClickListener)

        for (v in arrayOf(viewGoBack, viewGoForward)) {
            v?.setOnLongClickListener(btnGoBackForwardOnLongClickListener)
        }
    }

    /**
     * Настраивает [viewFiles], [viewFilesContainer], [fileAdapter].
     */
    private fun setupViewFiles() {
        viewFiles = when (DisplayPrefs.getViewType(this)) {
            ViewType.Grid -> layoutInflater.inflate(R.layout.afc_gridview_files, null) as AbsListView
            ViewType.List -> layoutInflater.inflate(R.layout.afc_listview_files, null) as AbsListView
        }

        viewFilesContainer?.removeAllViews()
        viewFilesContainer?.addView(
            viewFiles,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        viewFiles?.onItemClickListener = viewFilesOnItemClickListener
        viewFiles?.onItemLongClickListener = viewFilesOnItemLongClickListener
        viewFiles?.setOnTouchListener { _, event ->
            listviewFilesGestureDetector?.onTouchEvent(event) ?: false
        }

        createIFileAdapter()

        footerView?.setOnLongClickListener {
            E.show(this@FileChooserActivity)
            false
        }
    }

    /**
     * Создаёт [IFileAdapter] и назначает его списку файлов.
     */
    private fun createIFileAdapter() {
        fileAdapter?.clear()

        fileAdapter = IFileAdapter(
            this,
            ArrayList(),
            fileProvider!!.filterMode,
            filenameRegexp,
            isMultiSelection
        )

        when (viewFiles) {
            is ListView -> (viewFiles as ListView).adapter = fileAdapter
            is GridView -> (viewFiles as GridView).adapter = fileAdapter
        }
    }

    /**
     * Настраивает:
     * - кнопку "Отмена";
     * - текстовое поле "сохранить как";
     * - кнопку "ОК".
     */
    private fun setupFooter() {
        val viewGroupFooterContainer = findViewById<ViewGroup>(R.id.afc_filechooser_activity_viewgroup_footer_container)
        val viewGroupFooter = findViewById<ViewGroup>(R.id.afc_filechooser_activity_viewgroup_footer2)
        val viewGroupFooterBottom = findViewById<ViewGroup>(R.id.afc_filechooser_activity_viewgroup_footer_bottom)

        if (isSaveDialog) {
            viewGroupFooterContainer.visibility = View.VISIBLE
            viewGroupFooter.visibility = View.VISIBLE

            txtSaveas?.visibility = View.VISIBLE
            txtSaveas?.setText(intent.getStringExtra(_DefaultFilename))
            txtSaveas?.setOnEditorActionListener(txtFilenameOnEditorActionListener)

            btnSave?.visibility = View.VISIBLE
            btnSave?.setOnClickListener(btnSaveSaveDialogOnClickListener)
            btnSave?.setBackgroundResource(R.drawable.afc_selector_button_ok_saveas)

            val size = resources.getDimensionPixelSize(R.dimen.afc_button_ok_saveas_size)
            val lp = btnSave?.layoutParams as LinearLayout.LayoutParams
            lp.width = size
            lp.height = size
            btnSave?.layoutParams = lp
        }

        if (isActionBar) {
            viewGroupFooterContainer.visibility = View.VISIBLE
            viewGroupFooterBottom.visibility = View.VISIBLE
            if (fileProvider?.filterMode != IFileProvider.FilterMode.FilesOnly) {
                btnOk?.visibility = View.VISIBLE
                btnOk?.setOnClickListener(btnOkActionBarOnClickListener)
            } else {
                btnOk?.visibility = View.GONE
            }
            btnCancel?.visibility = View.VISIBLE
            btnCancel?.setOnClickListener(btnCancelActionBarOnClickListener)
            btnOk?.text = textResources?.get("ok")
            btnCancel?.text = textResources?.get("cancel")
        }

        if (isMultiSelection) {
            viewGroupFooterContainer.visibility = View.VISIBLE
            viewGroupFooter.visibility = View.VISIBLE

            val lp = viewGroupFooter.layoutParams
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            viewGroupFooter.layoutParams = lp

            btnSave?.minWidth = resources.getDimensionPixelSize(R.dimen.afc_single_button_min_width)
            btnSave?.setText(android.R.string.ok)
            btnSave?.visibility = View.VISIBLE
            btnSave?.setOnClickListener(btnSaveOpenDialogOnClickListener)
        }
    }

    private fun doReloadCurrentLocation() {
        setLocation(location!!, null)
    }

    private fun doShowCannotConnectToServiceAndFinish() {
        Dlg.showError(
            this@FileChooserActivity,
            R.string.afc_msg_cannot_connect_to_file_provider_service,
            DialogInterface.OnCancelListener {
                setResult(RESULT_CANCELED)
                finish()
            }
        )
    }

    private fun doGoHome() {
        goTo(root!!.clone())
    }

    /**
     * Показывает диалог с опциями сортировки и пересортировывает список файлов.
     */
    @SuppressLint("ResourceType")
    private fun doResortViewFiles() {
        val dialog = Dlg.newDlg(this)

        // Получаем индекс текущего типа сортировки
        var btnCurrentSortTypeIdx = when (DisplayPrefs.getSortType(this)) {
            SortType.SortByName -> 0
            SortType.SortBySize -> 2
            SortType.SortByDate -> 4
        }
        if (!DisplayPrefs.isSortAscending(this)) {
            btnCurrentSortTypeIdx++
        }

        val listener = View.OnClickListener { v ->
            dialog.dismiss()

            val c = this@FileChooserActivity

            when (v.id) {
                R.id.afc_settings_sort_view_button_sort_by_name_asc -> {
                    DisplayPrefs.setSortType(c, SortType.SortByName)
                    DisplayPrefs.setSortAscending(c, true)
                }
                R.id.afc_settings_sort_view_button_sort_by_name_desc -> {
                    DisplayPrefs.setSortType(c, SortType.SortByName)
                    DisplayPrefs.setSortAscending(c, false)
                }
                R.id.afc_settings_sort_view_button_sort_by_size_asc -> {
                    DisplayPrefs.setSortType(c, SortType.SortBySize)
                    DisplayPrefs.setSortAscending(c, true)
                }
                R.id.afc_settings_sort_view_button_sort_by_size_desc -> {
                    DisplayPrefs.setSortType(c, SortType.SortBySize)
                    DisplayPrefs.setSortAscending(c, false)
                }
                R.id.afc_settings_sort_view_button_sort_by_date_asc -> {
                    DisplayPrefs.setSortType(c, SortType.SortByDate)
                    DisplayPrefs.setSortAscending(c, true)
                }
                R.id.afc_settings_sort_view_button_sort_by_date_desc -> {
                    DisplayPrefs.setSortType(c, SortType.SortByDate)
                    DisplayPrefs.setSortAscending(c, false)
                }
            }

            resortViewFiles()
            if (DisplayPrefs.isSortAscending(c)) {
                viewSort?.setImageDrawable(resources.getDrawable(R.drawable.afc_selector_button_sort_as))
                viewSort?.setId(R.drawable.afc_selector_button_sort_as)
            } else {
                viewSort?.setImageDrawable(resources.getDrawable(R.drawable.afc_selector_button_sort_de))
                viewSort?.setId(R.drawable.afc_selector_button_sort_de)
            }
        }

        val view = layoutInflater.inflate(R.layout.afc_settings_sort_view, null)
        var sortTitle = view.findViewById<TextView>(R.id.afc_settings_sort_view_textview_sort_by_name)
        sortTitle.text = textResources?.get("sortByName")
        sortTitle = view.findViewById(R.id.afc_settings_sort_view_textview_sort_by_size)
        sortTitle.text = textResources?.get("sortBySize")
        sortTitle = view.findViewById(R.id.afc_settings_sort_view_textview_sort_by_date)
        sortTitle.text = textResources?.get("sortByDate")

        for (i in BTN_SORT_IDS.indices) {
            val btn = view.findViewById<Button>(BTN_SORT_IDS[i])
            btn.setOnClickListener(listener)
            if (i == btnCurrentSortTypeIdx) {
                btn.isEnabled = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    btn.setText(R.string.afc_ellipsize)
                }
            }
        }

        dialog.setTitle(textResources?.get("sortBy"))
        dialog.setView(view)
        dialog.show()
    }

    /**
     * Пересортировывает список файлов.
     */
    private fun resortViewFiles() {
        if (fileProvider?.sortType == DisplayPrefs.getSortType(this) &&
            fileProvider?.sortOrder?.isAsc == DisplayPrefs.isSortAscending(this)
        ) {
            return
        }

        fileProvider?.sortTypeValue = DisplayPrefs.getSortType(this)
        fileProvider?.sortOrderValue = if (DisplayPrefs.isSortAscending(this)) SortOrder.Ascending else SortOrder.Descending
        doReloadCurrentLocation()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActivityCompat.invalidateOptionsMenu(this)
        }
    }

    /**
     * Переключает тип представления между [ViewType.List] и [ViewType.Grid].
     */
    private fun doSwitchViewType() {
        object : LoadingDialog(this@FileChooserActivity as Context, getString(R.string.afc_msg_loading), false) {
            override fun onPreExecute() {
                super.onPreExecute()

                when (DisplayPrefs.getViewType(this@FileChooserActivity)) {
                    ViewType.Grid -> DisplayPrefs.setViewType(this@FileChooserActivity, ViewType.List)
                    ViewType.List -> DisplayPrefs.setViewType(this@FileChooserActivity, ViewType.Grid)
                }

                setupViewFiles()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    ActivityCompat.invalidateOptionsMenu(this@FileChooserActivity)
                }

                doReloadCurrentLocation()
            }

            override fun doInBackground(vararg params: Void): Any? = null
        }.execute()
    }

    /**
     * Подтверждает создание новой директории.
     */
    private fun doCreateNewDir() {
        if (fileProvider is LocalFileProvider && !Utils.hasStoragePermissions(this)) {
            Dlg.toast(this, R.string.afc_msg_app_doesnot_have_permission_to_create_files, Dlg.LENGTH_SHORT)
            return
        }

        if (location is File) {
            if (!(location as File).canWrite()) {
                Dlg.toast(this, R.string.afc_msg_app_cant_create_folder, Dlg.LENGTH_SHORT)
                return
            }
        }

        val dlg = Dlg.newDlg(this)

        val view = layoutInflater.inflate(R.layout.afc_simple_text_input_view, null)
        val textFile = view.findViewById<EditText>(R.id.afc_simple_text_input_view_text1)
        textFile.hint = textResources?.get("folderNameHint")
        textFile.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                Ui.hideSoftKeyboard(this@FileChooserActivity, textFile.windowToken)
                dlg.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
                return@setOnEditorActionListener true
            }
            false
        }

        dlg.setView(view)
        dlg.setTitle(textResources?.get("newFolder"))
        dlg.setIcon(android.R.drawable.ic_menu_add)
        dlg.setButton(
            DialogInterface.BUTTON_POSITIVE,
            getString(android.R.string.ok)
        ) { _, _ ->
            val name = textFile.text.toString().trim()
            if (!FileUtils.isFilenameValid(name)) {
                Dlg.toast(
                    this@FileChooserActivity,
                    getString(R.string.afc_pmsg_filename_is_invalid, name),
                    Dlg.LENGTH_SHORT
                )
                return@setButton
            }

            val fileProvider = fileProvider ?: return@setButton
            val location = location ?: return@setButton

            val dir = fileProvider.fromPath("${location.getAbsolutePath()}/$name")
            if (dir.mkdir()) {
                Dlg.toast(this@FileChooserActivity, getString(R.string.afc_msg_done), Dlg.LENGTH_SHORT)
                setLocation(location, null)
            } else {
                Dlg.toast(
                    this@FileChooserActivity,
                    getString(R.string.afc_pmsg_cannot_create_folder, name),
                    Dlg.LENGTH_SHORT
                )
            }
        }
        dlg.show()

        val btnOk = dlg.getButton(DialogInterface.BUTTON_POSITIVE)
        btnOk.isEnabled = false

        textFile.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnOk.isEnabled = FileUtils.isFilenameValid(s.toString().trim())
            }
        })
    }

    /**
     * Обновляет UI, что `data` не будет удалена.
     */
    private fun notifyDataModelNotDeleted(data: IFileDataModel) {
        data.isTobeDeleted = false
        fileAdapter?.notifyDataSetChanged()
    }

    /**
     * Удаляет файл.
     */
    private fun doDeleteFile(data: IFileDataModel) {
        if (fileProvider is LocalFileProvider && !Utils.hasStoragePermissions(this)) {
            notifyDataModelNotDeleted(data)
            Dlg.toast(this, R.string.afc_msg_app_doesnot_have_permission_to_delete_files, Dlg.LENGTH_SHORT)
            return
        }

        Dlg.confirmYesno(
            this,
            getString(
                R.string.afc_pmsg_confirm_delete_file,
                if (data.file.isFile()) getString(R.string.afc_file) else getString(R.string.afc_folder),
                data.file.getName()
            ),
            DialogInterface.OnClickListener { _, _ ->
                object : LoadingDialog(
                    this@FileChooserActivity as Context,
                    getString(
                        R.string.afc_pmsg_deleting_file,
                        if (data.file.isFile()) getString(R.string.afc_file) else getString(R.string.afc_folder),
                        data.file.getName()
                    ),
                    true
                ) {
                    private val isFile = data.file.isFile()
                    private val thread = FileUtils.createDeleteFileThread(data.file, fileProvider!!, true)

                    private fun notifyFileDeleted() {
                        fileAdapter?.remove(data)
                        fileAdapter?.notifyDataSetChanged()

                        refreshHistories()

                        Dlg.toast(
                            this@FileChooserActivity,
                            getString(
                                R.string.afc_pmsg_file_has_been_deleted,
                                if (isFile) getString(R.string.afc_file) else getString(R.string.afc_folder),
                                data.file.getName()
                            ),
                            Dlg.LENGTH_SHORT
                        )
                    }

                    override fun onPreExecute() {
                        super.onPreExecute()
                        thread.start()
                    }

                    override fun doInBackground(vararg params: Void): Any? {
                        while (thread.isAlive) {
                            try {
                                thread.join(DisplayPrefs.DELAY_TIME_WAITING_THREADS.toLong())
                            } catch (e: InterruptedException) {
                                thread.interrupt()
                            }
                        }
                        return null
                    }

                    override fun onCancelled() {
                        thread.interrupt()

                        if (data.file.exists()) {
                            notifyDataModelNotDeleted(data)
                            Dlg.toast(this@FileChooserActivity, R.string.afc_msg_cancelled, Dlg.LENGTH_SHORT)
                        } else {
                            notifyFileDeleted()
                        }

                        super.onCancelled()
                    }

                    override fun onPostExecute(result: Any?) {
                        super.onPostExecute(result)

                        if (data.file.exists()) {
                            notifyDataModelNotDeleted(data)
                            Dlg.toast(
                                this@FileChooserActivity,
                                getString(
                                    R.string.afc_pmsg_cannot_delete_file,
                                    if (data.file.isFile()) getString(R.string.afc_file) else getString(R.string.afc_folder),
                                    data.file.getName()
                                ),
                                Dlg.LENGTH_SHORT
                            )
                        } else {
                            notifyFileDeleted()
                        }
                    }
                }.execute()
            },
            DialogInterface.OnCancelListener {
                notifyDataModelNotDeleted(data)
            }
        )
    }

    /**
     * Проверяет имя файла и завершает активность.
     */
    private fun doCheckSaveasFilenameAndFinish(filename: String) {
        if (filename.isEmpty()) {
            Dlg.toast(this, R.string.afc_msg_filename_is_empty, Dlg.LENGTH_SHORT)
            return
        }

        val file = fileProvider!!.fromPath(location!!.getAbsolutePath() + File.separator + filename)

        if (!FileUtils.isFilenameValid(filename)) {
            Dlg.toast(this, getString(R.string.afc_pmsg_filename_is_invalid, filename), Dlg.LENGTH_SHORT)
        } else if (file.isFile()) {
            Dlg.confirmYesno(
                this@FileChooserActivity,
                getString(R.string.afc_pmsg_confirm_replace_file, file.getName()),
                DialogInterface.OnClickListener { _, _ ->
                    doFinish(file)
                }
            )
        } else if (file.isDirectory()) {
            Dlg.toast(this, getString(R.string.afc_pmsg_filename_is_directory, file.getName()), Dlg.LENGTH_SHORT)
        } else {
            doFinish(file)
        }
    }

    /** Получает текущее местоположение. */
    private val location: IFile?
        get() = viewLocations?.tag as? IFile

    /**
     * Устанавливает текущее местоположение.
     */
    private fun setLocation(path: IFile, listener: TaskListener?) {
        setLocation(path, listener, null)
    }

    /**
     * Устанавливает текущее местоположение.
     */
    private fun setLocation(path: IFile, listener: TaskListener?, selectedFile: IFile?) {
        object : LoadingDialog(this@FileChooserActivity as Context, getString(R.string.afc_msg_loading), true) {
            var files: List<IFile>? = null
            var hasMoreFiles = false
            var shouldBeSelectedIdx = -1
            var lastPath = location?.getAbsolutePath()

            override fun doInBackground(vararg params: Void): Any? {
                try {
                    if (path.isDirectory()&& path.canRead()) {
                        files = ArrayList()
                        fileProvider!!.listAllFiles(path, object : IFileFilter {
                            override fun accept(pathname: IFile): Boolean {
                                if (fileProvider!!.accept(pathname)) {
                                    if (files!!.size < fileProvider!!.maxFileCount) {
                                        (files as ArrayList).add(pathname)
                                    } else {
                                        hasMoreFiles = true
                                    }
                                }
                                return false
                            }
                        })
                    } else {
                        files = null
                    }

                    if (files != null) {
                        Collections.sort(files, FileComparator(fileProvider!!.sortType, fileProvider!!.sortOrder))
                        if (selectedFile != null && selectedFile.exists() &&
                            selectedFile.parentFile()?.equalsToPath(path) == true
                        ) {
                            for (i in files!!.indices) {
                                if (files!![i].equalsToPath(selectedFile)) {
                                    shouldBeSelectedIdx = i
                                    break
                                }
                            }
                        } else if (lastPath != null && lastPath!!.length >= path.getAbsolutePath().length) {
                            for (i in files!!.indices) {
                                val f = files!![i]
                                if (f.isDirectory()&& lastPath!!.startsWith(f.getAbsolutePath())) {
                                    shouldBeSelectedIdx = i
                                    break
                                }
                            }
                        }
                    }
                } catch (t: Throwable) {
                    setLastException(t)
                    cancel(false)
                }
                return null
            }

            override fun onCancelled() {
                super.onCancelled()
                Dlg.toast(this@FileChooserActivity, R.string.afc_msg_cancelled, Dlg.LENGTH_SHORT)
            }

            override fun onPostExecute(result: Any?) {
                super.onPostExecute(result)

                if (files == null) {
                    Dlg.toast(this@FileChooserActivity, textResources?.get("permissionDenied") ?: "", Dlg.LENGTH_SHORT)
                    listener?.onFinish(false, path)
                    return
                }

                // Обновляем список
                createIFileAdapter()
                for (f in files!!) {
                    fileAdapter?.add(IFileDataModel(f))
                }
                fileAdapter?.notifyDataSetChanged()

                // Обновляем footer
                footerView?.visibility = if (hasMoreFiles || fileAdapter!!.isEmpty) View.VISIBLE else View.GONE
                if (hasMoreFiles) {
                    footerView?.text = getString(
                        R.string.afc_pmsg_max_file_count_allowed,
                        fileProvider!!.maxFileCount
                    )
                } else if (fileAdapter!!.isEmpty) {
                    footerView?.setText(R.string.afc_msg_empty)
                }

                viewFiles?.post {
                    if (shouldBeSelectedIdx >= 0 && shouldBeSelectedIdx < fileAdapter!!.count) {
                        viewFiles?.setSelection(shouldBeSelectedIdx)
                    } else if (fileAdapter?.isEmpty == false) {
                        viewFiles?.setSelection(0)
                    }
                }

                createLocationButtons(path)
                updateUI(path)

                listener?.onFinish(true, path)
            }
        }.execute()
    }

    /**
     * Переходит в указанное местоположение.
     */
    private fun goTo(dir: IFile): Boolean {
        if (dir.equalsToPath(location)) return false

        setLocation(dir, object : TaskListener {
            private val lastPath = location

            override fun onFinish(ok: Boolean, any: Any?) {
                if (ok) {
                    history?.truncateAfter(lastPath!!)
                    history?.push(dir)
                    fullHistory?.push(dir)
                }
            }
        })
        return true
    }

    private fun createLocationButtons(path: IFile) {
        viewLocations?.tag = path
        viewLocations?.removeAllViews()

        val lpBtnLoc = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lpBtnLoc.gravity = Gravity.CENTER

        val inflater = layoutInflater
        var currentPath: IFile? = path
        var count = 0
        while (currentPath != null) {
            val btnLoc = inflater.inflate(R.layout.afc_button_location, null) as TextView
            btnLoc.text = if (currentPath.parentFile() != null) "/${currentPath.getName()}" else textResources?.get("root")
            btnLoc.tag = currentPath
            btnLoc.setOnClickListener(btnLocationOnClickListener)
            btnLoc.setOnLongClickListener(btnLocationOnLongClickListener)
            viewLocations?.addView(btnLoc, 0, lpBtnLoc)

            if (count++ == 0) {
                val r = Rect()
                btnLoc.paint.getTextBounds(currentPath.getName(), 0, currentPath.getName().length, r)
                if (r.width() >= resources.getDimensionPixelSize(R.dimen.afc_button_location_max_width) -
                    btnLoc.paddingLeft - btnLoc.paddingRight
                ) {
                    txtFullDirName?.text = currentPath.getName()
                    txtFullDirName?.visibility = View.VISIBLE
                } else {
                    txtFullDirName?.visibility = View.GONE
                }
            }

            currentPath = currentPath.parentFile()
        }

        viewLocationsContainer?.postDelayed({
            viewLocationsContainer?.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
        }, 100)
    }

    /**
     * Обновляет все истории. Удаляет недействительные элементы (которые больше не существуют).
     */
    private fun refreshHistories() {
        val historyFilter = object : HistoryFilter<IFile> {
            override fun accept(item: IFile): Boolean = !item.isDirectory()        }

        history?.removeAll(historyFilter)
        fullHistory?.removeAll(historyFilter)
    }

    /**
     * Завершает эту активность.
     */
    private fun doFinish(vararg files: IFile) {
        val list = ArrayList<IFile>()
        for (f in files) {
            list.add(f)
        }
        doFinish(list)
    }

    /**
     * Завершает эту активность.
     */
    private fun doFinish(files: ArrayList<IFile>?) {
        var returnPath: String? = null

        when (fileProvider?.filterMode) {
            FilterMode.FilesOnly -> {
                if (files == null || files.isEmpty()) {
                    setResult(RESULT_CANCELED)
                    finish()
                    return
                }
            }
            FilterMode.DirectoriesOnly -> {
                val file = location as? File
                if (file != null && file.canWrite()) {
                    returnPath = location?.getAbsolutePath()
                }
            }
            FilterMode.FilesAndDirectories -> {
                if (files == null || files.isEmpty()) {
                    returnPath = location?.getAbsolutePath()
                }
            }
            else -> {
                returnPath = location?.getAbsolutePath()
            }
        }

        var hasData = false
        val intent = Intent()
        if (returnPath != null) {
            intent.putExtra(_FolderPath, returnPath)
            hasData = true
        }

        if (files != null) {
            intent.putExtra(_Results, files)
            hasData = true
        } else {
            intent.putExtra(_Results, ArrayList<IFile>())
        }

        if (!hasData) return

        intent.putExtra(_FilterMode, fileProvider?.filterMode)
        intent.putExtra(_SaveDialog, isSaveDialog)

        setResult(RESULT_OK, intent)

        if (DisplayPrefs.isRememberLastLocation(this) && location != null) {
            DisplayPrefs.setLastLocation(this, location?.getAbsolutePath())
        } else {
            DisplayPrefs.setLastLocation(this, null)
        }

        finish()
    }

    private fun updateUI(dir: IFile) {
        val isDirectoryWriteable = (dir as? File)?.canWrite() ?: false
        viewCreateFolder?.isEnabled = isDirectoryWriteable
        btnOk?.isEnabled = isDirectoryWriteable ||
            fileProvider?.filterMode == IFileProvider.FilterMode.AnyDirectories
    }

    private fun initGestureDetector() {
        listviewFilesGestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {

            private fun getData(x: Float, y: Float): Any? {
                val i = getSubViewId(x, y)
                if (i >= 0) {
                    return viewFiles?.getItemAtPosition(viewFiles!!.firstVisiblePosition + i)
                }
                return null
            }

            private fun getDataModel(e: MotionEvent): IFileDataModel? {
                val o = getData(e.x, e.y)
                return if (o is IFileDataModel) o else null
            }

            private fun getSubViewId(x: Float, y: Float): Int {
                val r = Rect()
                for (i in 0 until viewFiles!!.childCount) {
                    viewFiles!!.getChildAt(i).getHitRect(r)
                    if (r.contains(x.toInt(), y.toInt())) {
                        return i
                    }
                }
                return -1
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (doubleTapToChooseFiles) {
                    if (isMultiSelection) return false

                    val data = getDataModel(e) ?: return false

                    if (data.file.isDirectory()&& IFileProvider.FilterMode.FilesOnly == fileProvider?.filterMode) {
                        return false
                    }

                    if (isSaveDialog) {
                        if (data.file.isFile()) {
                            txtSaveas?.setText(data.file.getName())
                            doCheckSaveasFilenameAndFinish(data.file.getName())
                        } else {
                            return false
                        }
                    } else {
                        doFinish(data.file)
                    }
                } else {
                    return false
                }

                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val maxYDistance = 19
                val minXDistance = 80
                val minXVelocity = 200
                if (e1 != null &&
                    Math.abs(e1.y - e2.y) < maxYDistance &&
                    Math.abs(e1.x - e2.x) > minXDistance &&
                    Math.abs(velocityX) > minXVelocity
                ) {
                    val o = getData(e1.x, e1.y)
                    if (o is IFileDataModel) {
                        o.isTobeDeleted = true
                        fileAdapter?.notifyDataSetChanged()
                        doDeleteFile(o)
                    }
                }

                return false
            }
        })
    }

    /**
     * Типы представления.
     *
     * @author Hai Bison
     * @since v4.0 beta
     */
    enum class ViewType {
        /** Использовать [ListView] для отображения списка файлов. */
        List,
        /** Использовать [GridView] для отображения списка файлов. */
        Grid
    }
}
