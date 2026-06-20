package com.github.axet.bookreader.screens

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Parcelable
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.axet.bookreader.app.BookApplication
import com.github.axet.bookreader.app.BookReaderInitializer
import com.github.axet.bookreader.screens.ui.ReaderTopBar
import com.github.axet.bookreader.screens.viewmodel.ReaderActions
import com.github.axet.bookreader.screens.viewmodel.ReaderState
import com.github.axet.bookreader.screens.viewmodel.ReaderViewModel
import com.github.axet.bookreader.widgets.BookmarksDialog
import com.github.axet.bookreader.widgets.FBReaderView
import com.github.axet.bookreader.widgets.FontsPopup
import org.geometerplus.fbreader.fbreader.ActionCode
import timber.log.Timber

/**
 * Контент экрана чтения книги
 *
 * @param bookUri URI файла книги
 * @param initialPosition Начальная позиция в книге
 * @param onNavigateToSettings Callback для перехода к настройкам
 * @param modifier Modifier
 */
@Composable
fun ReaderContent(
    bookUri: Uri,
    initialPosition: Parcelable?,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

    // Состояние для FBReaderView
    var fbReaderView by remember { mutableStateOf<FBReaderView?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    // Загрузка книги при первом отображении
    LaunchedEffect(bookUri) {
        val position = initialPosition as? FBReaderView.ZLTextIndexPosition
        viewModel.onAction(ReaderActions.LoadBook(bookUri, position))
    }

    // Обработка батареи
    BatteryReceiver(fbReaderView)

    // Обработка клавиш громкости
    VolumeKeysHandler(fbReaderView, viewModel)

    // Обработка диалогов
    val currentState = state as? ReaderState.Content
    if (currentState != null) {
        BookmarksDialogHandler(
            state = currentState,
            viewModel = viewModel,
        )
        FontsPopupHandler(
            state = currentState,
            fbReaderView = fbReaderView,
            viewModel = viewModel,
        )
        TocDialogHandler(
            state = currentState,
            fbReaderView = fbReaderView,
            viewModel = viewModel,
        )
    }

    // Сохранение позиции при выходе
    DisposableEffect(Unit) {
        onDispose {
            viewModel.savePosition()
            viewModel.closeBook()
        }
    }

    when (val currentState = state) {
        is ReaderState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is ReaderState.Content -> {
            Scaffold(
                topBar = {
                    if (!currentState.isFullscreen) {
                        ReaderTopBar(
                            state = currentState,
                            onAction = { action ->
                                when (action) {
                                    ReaderActions.NavigateToSettings -> onNavigateToSettings()
                                    else -> viewModel.onAction(action)
                                }
                            },
                        )
                    }
                },
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // FBReaderView через AndroidView
                    AndroidView(
                        factory = { ctx ->
                            Timber.d("Creating FBReaderView")
                            FBReaderView(ctx).apply {
                                // Устанавливаем listener
                                listener = object : FBReaderView.Listener {
                                    override fun onScrollingFinished(index: org.geometerplus.zlibrary.core.view.ZLViewEnums.PageIndex?) {
                                        viewModel.savePosition()
                                    }

                                    override fun onSearchClose() {
                                        // TODO: Закрыть поиск
                                    }

                                    override fun onBookmarksUpdate() {
                                        // Обновление UI
                                    }

                                    override fun onDismissDialog() {
                                        // Скрытие системного UI
                                    }

                                    override fun ttsStatus(speaking: Boolean) {
                                        viewModel.volumeKeysEnabled = !speaking
                                    }
                                }

                                // Устанавливаем режим просмотра
                                val viewMode = currentState.viewMode
                                setWidget(
                                    if (viewMode.name == "CONTINUOUS") FBReaderView.Widgets.CONTINUOUS
                                    else FBReaderView.Widgets.PAGING
                                )

                                // Устанавливаем Activity и Window
                                if (context is Activity) {
                                    setWindow(context.window)

                                    // Устанавливаем Activity для FBReaderView
                                    // OnBookPagerManager будет инжектирован через Hilt
                                    setActivity(context, null)
                                }

                                fbReaderView = this
                                viewModel.fbReaderView = this

                                // Загружаем книгу
                                val fbook = viewModel.getFBook()
                                if (fbook != null) {
                                    try {
                                        loadBook(fbook)
                                        isLoaded = true
                                        Timber.d("Book loaded successfully")
                                    } catch (e: Exception) {
                                        Timber.e(e, "Failed to load book in FBReaderView")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { fbView ->
                            // Обновление при изменении состояния
                            if (isLoaded) {
                                // Обновление темы и других настроек
                            }
                        }
                    )
                }
            }
        }

        is ReaderState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Ошибка: ${currentState.message}")
            }
        }
    }
}

/**
 * Приёмник изменений уровня батареи
 */
@Composable
private fun BatteryReceiver(fbReaderView: FBReaderView?) {
    val context = LocalContext.current

    DisposableEffect(fbReaderView) {
        if (fbReaderView == null) return@DisposableEffect onDispose {}

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                fbReaderView.battery = level * 100 / scale
                fbReaderView.invalidateFooter()
            }
        }

        // Регистрируем приёмник
        val batteryIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        }

        // Сразу отправляем текущий уровень
        receiver.onReceive(context, batteryIntent ?: return@DisposableEffect onDispose {
            context.unregisterReceiver(receiver)
        })

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Timber.e(e, "Failed to unregister battery receiver")
            }
        }
    }
}

/**
 * Обработчик клавиш громкости для навигации по страницам
 */
@Composable
private fun VolumeKeysHandler(
    fbReaderView: FBReaderView?,
    viewModel: ReaderViewModel
) {
    val view = LocalView.current
    val context = LocalContext.current

    // Проверяем настройку клавиш громкости
    val volumeKeysEnabled = remember {
        val shared = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
        shared.getBoolean(BookApplication.PREFERENCE_VOLUME_KEYS, false)
    }

    DisposableEffect(fbReaderView, volumeKeysEnabled) {
        if (fbReaderView == null || !volumeKeysEnabled) {
            return@DisposableEffect onDispose {}
        }

        val keyListener = View.OnKeyListener { _, keyCode, event ->
            if (!viewModel.volumeKeysEnabled) return@OnKeyListener false

            when {
                keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN &&
                    event.action == android.view.KeyEvent.ACTION_DOWN -> {
                    fbReaderView.app?.runAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD)
                    true
                }
                keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP &&
                    event.action == android.view.KeyEvent.ACTION_DOWN -> {
                    fbReaderView.app?.runAction(ActionCode.VOLUME_KEY_SCROLL_BACK)
                    true
                }
                else -> false
            }
        }

        view.isFocusableInTouchMode = true
        view.setOnKeyListener(keyListener)

        onDispose {
            view.setOnKeyListener(null)
        }
    }
}

/**
 * Обработчик диалога закладок
 */
@Composable
private fun BookmarksDialogHandler(
    state: ReaderState.Content,
    viewModel: ReaderViewModel,
) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<AlertDialog?>(null) }

    DisposableEffect(state.showBookmarks) {
        if (state.showBookmarks && dialog == null) {
            val book = viewModel.getCurrentBook()
            if (book != null) {
                val bookmarksDialog = object : BookmarksDialog(context) {
                    override fun onSelected(b: com.github.axet.bookreader.app.Storage.Bookmark) {
                        viewModel.onAction(ReaderActions.GoToBookmark(b))
                    }

                    override fun onDelete(b: com.github.axet.bookreader.app.Storage.Bookmark) {
                        viewModel.onAction(ReaderActions.DeleteBookmark(b))
                    }
                }
                bookmarksDialog.load(book.info.bookmarks)
                dialog = bookmarksDialog.show()
                dialog?.setOnDismissListener {
                    viewModel.onAction(ReaderActions.HideDialogs)
                    dialog = null
                }
            }
        }

        onDispose {
            dialog?.dismiss()
            dialog = null
        }
    }
}

/**
 * Обработчик popup со шрифтами
 */
@Composable
private fun FontsPopupHandler(
    state: ReaderState.Content,
    fbReaderView: FBReaderView?,
    viewModel: ReaderViewModel,
) {
    val context = LocalContext.current
    var popup by remember { mutableStateOf<FontsPopup?>(null) }

    DisposableEffect(state.showFontSettings) {
        if (state.showFontSettings && popup == null && fbReaderView != null) {
            val ttf = BookReaderInitializer.getTTFManager()
            if (ttf != null) {
                val fontsPopup = object : FontsPopup(context, ttf) {
                    override fun setFontsize(f: Int) {
                        viewModel.onAction(ReaderActions.SetFontSize(f))
                    }

                    override fun setFont(str: String) {
                        viewModel.onAction(ReaderActions.SetFontFamily(str))
                    }

                    override fun setIgnoreEmbeddedFonts(f: Boolean) {
                        viewModel.onAction(ReaderActions.SetIgnoreEmbeddedFonts(f))
                    }

                    override fun updateFontsize(f: Int) {
                        // Обновление UI слайдера
                    }
                }

                // Настраиваем popup
                val shared = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                val currentSize = shared.getInt(BookApplication.PREFERENCE_FONTSIZE_FBREADER, 16)
                fontsPopup.updateFontsize(8, 48, currentSize)
                fontsPopup.loadFonts()

                val currentFont = shared.getString(BookApplication.PREFERENCE_FONTFAMILY_FBREADER, "sans-serif") ?: "sans-serif"
                fontsPopup.fonts.select(currentFont)

                val ignoreEmbedded = shared.getBoolean(BookApplication.PREFERENCE_IGNORE_EMBEDDED_FONTS, false)
                fontsPopup.ignoreEmbeddedFonts.isChecked = ignoreEmbedded

                // Показываем popup
                val parent = FrameLayout(context)
                fontsPopup.showAtLocation(parent, android.view.Gravity.BOTTOM, 0, 0)
                popup = fontsPopup

                fontsPopup.setOnDismissListener {
                    viewModel.onAction(ReaderActions.HideDialogs)
                    popup = null
                }
            }
        }

        onDispose {
            popup?.dismiss()
            popup = null
        }
    }
}

/**
 * Обработчик диалога содержания (TOC)
 */
@Composable
private fun TocDialogHandler(
    state: ReaderState.Content,
    fbReaderView: FBReaderView?,
    viewModel: ReaderViewModel,
) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<AlertDialog?>(null) }

    DisposableEffect(state.showToc) {
        if (state.showToc && fbReaderView != null && dialog == null) {
            // Получаем TOC из книги
            val tocTree = fbReaderView.app?.Model?.TOCTree
            if (tocTree != null) {
                // Собираем все элементы TOC в плоский список
                data class TocItem(
                    val title: String,
                    val position: org.geometerplus.zlibrary.text.view.ZLTextPosition,
                )

                val tocItems = mutableListOf<TocItem>()
                fun collectTocItems(tree: org.geometerplus.fbreader.bookmodel.TOCTree, level: Int = 0) {
                    for (child in tree.subtrees()) {
                        val text = child.text
                        val ref = child.reference
                        if (text != null && ref != null) {
                            tocItems.add(
                                TocItem(
                                    title = "${"  ".repeat(level)}$text",
                                    position = org.geometerplus.zlibrary.text.view.ZLTextFixedPosition(
                                        ref.ParagraphIndex,
                                        0,
                                        0
                                    )
                                )
                            )
                        }
                        collectTocItems(child, level + 1)
                    }
                }
                collectTocItems(tocTree)

                if (tocItems.isNotEmpty()) {
                    val items = tocItems.map { it.title }.toTypedArray()

                    dialog = androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Содержание")
                        .setItems(items) { _, which ->
                            val selected = tocItems[which]
                            fbReaderView.gotoPosition(selected.position)
                            viewModel.onAction(ReaderActions.HideDialogs)
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            viewModel.onAction(ReaderActions.HideDialogs)
                        }
                        .setOnDismissListener {
                            viewModel.onAction(ReaderActions.HideDialogs)
                            dialog = null
                        }
                        .create()
                    dialog?.show()
                } else {
                    // Нет элементов TOC
                    viewModel.onAction(ReaderActions.HideDialogs)
                }
            } else {
                viewModel.onAction(ReaderActions.HideDialogs)
            }
        }

        onDispose {
            dialog?.dismiss()
            dialog = null
        }
    }
}
