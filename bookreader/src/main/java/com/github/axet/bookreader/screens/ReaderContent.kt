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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.BookApplication
import com.github.axet.bookreader.app.BookReaderInitializer
import com.github.axet.bookreader.screens.ui.BookmarkBottomSheet
import com.github.axet.bookreader.screens.ui.BookmarksComposeDialog
import com.github.axet.bookreader.screens.ui.ReaderTopBar
import com.github.axet.bookreader.screens.viewmodel.ReaderActions
import com.github.axet.bookreader.screens.viewmodel.ReaderState
import com.github.axet.bookreader.screens.viewmodel.ReaderViewModel
import com.github.axet.bookreader.widgets.FBReaderView
import org.geometerplus.fbreader.fbreader.ActionCode
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import timber.log.Timber

/**
 * Контент экрана чтения книги
 */
@Composable
fun ReaderContent(
    bookUri: Uri,
    initialPosition: Parcelable?,
    onNavigateBack: () -> Unit = {},
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

    // Загрузка книги только если ещё не загружена
    LaunchedEffect(bookUri) {
        // Проверяем, что книга ещё не загружена в ViewModel
        if (viewModel.getCurrentBook() == null) {
            val position = initialPosition as? FBReaderView.ZLTextIndexPosition
            viewModel.onAction(ReaderActions.LoadBook(bookUri, position))
        }
    }

    // Обработка батареи
    BatteryReceiver(fbReaderView)

    // Обработка клавиш громкости
    VolumeKeysHandler(fbReaderView, viewModel)

    // Сохранение позиции при уходе с экрана (не закрываем книгу!)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.savePosition()
            // НЕ вызываем closeBook() - книга должна оставаться открытой
            // closeBook() вызовется в ViewModel.onCleared() при уничтожении ViewModel
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
            // Диалоги
            if (currentState.showToc) {
                TocComposeDialog(
                    fbReaderView = fbReaderView,
                    onDismiss = { viewModel.onAction(ReaderActions.HideDialogs) },
                    onNavigate = { position ->
                        fbReaderView?.gotoPosition(position)
                        viewModel.onAction(ReaderActions.HideDialogs)
                    }
                )
            }

            if (currentState.showBookmarks) {
                BookmarksComposeDialog(
                    book = viewModel.getCurrentBook(),
                    fbReaderView = fbReaderView,
                    onDismiss = { viewModel.onAction(ReaderActions.HideDialogs) },
                    onDelete = { bookmark ->
                        viewModel.onAction(ReaderActions.DeleteBookmark(bookmark))
                    }
                )
            }

            if (currentState.showFontSettings) {
                FontsComposeBottomSheet(
                    fbReaderView = fbReaderView,
                    onDismiss = { viewModel.onAction(ReaderActions.HideDialogs) },
                    onFontSizeChange = { size ->
                        viewModel.onAction(ReaderActions.SetFontSize(size))
                    },
                    onFontFamilyChange = { family ->
                        viewModel.onAction(ReaderActions.SetFontFamily(family))
                    },
                    onIgnoreEmbeddedFontsChange = { ignore ->
                        viewModel.onAction(ReaderActions.SetIgnoreEmbeddedFonts(ignore))
                    }
                )
            }

            // Редактирование закладки
            if (currentState.showBookmarkEdit && currentState.editingBookmark != null) {
                BookmarkBottomSheet(
                    bookmarkText = currentState.editingBookmark.text,
                    initialName = currentState.editingBookmark.name,
                    initialColor = currentState.editingBookmark.color,
                    onDismiss = { viewModel.onAction(ReaderActions.HideDialogs) },
                    onSave = { name, color ->
                        viewModel.onAction(
                            ReaderActions.SaveBookmarkEdit(
                                currentState.editingBookmark,
                                name,
                                color
                            )
                        )
                    },
                    onDelete = {
                        viewModel.onAction(ReaderActions.DeleteBookmark(currentState.editingBookmark))
                        viewModel.onAction(ReaderActions.HideDialogs)
                    }
                )
            }

            Scaffold(
                topBar = {
                    if (!currentState.isFullscreen) {
                        ReaderTopBar(
                            state = currentState,
                            onAction = { action ->
                                when (action) {
                                    ReaderActions.NavigateBack -> onNavigateBack()
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
                            Timber.d("Creating FBReaderView for $bookUri")
                            FBReaderView(ctx).apply {
                                listener = object : FBReaderView.Listener {
                                    override fun onScrollingFinished(index: org.geometerplus.zlibrary.core.view.ZLViewEnums.PageIndex?) {
                                        viewModel.savePosition()
                                    }

                                    override fun onSearchClose() {}
                                    override fun onBookmarksUpdate() {
                                        viewModel.syncBookmarksFromFBook()
                                    }

                                    override fun onDismissDialog() {}
                                    override fun ttsStatus(speaking: Boolean) {
                                        viewModel.volumeKeysEnabled = !speaking
                                    }

                                    override fun onEditBookmark(bookmark: com.github.axet.bookreader.app.Storage.Bookmark) {
                                        viewModel.onAction(ReaderActions.EditBookmark(bookmark))
                                    }
                                }

                                if (context is Activity) {
                                    setWindow(context.window)
                                    setActivity(context, viewModel.getOnBookPagerManager())
                                }

                                fbReaderView = this
                                viewModel.fbReaderView = this

                                // Загружаем книгу
                                val fbook = viewModel.getFBook()
                                if (fbook != null) {
                                    try {
                                        loadBook(fbook)
                                        // Устанавливаем режим просмотра
                                        val viewMode = currentState.viewMode
                                        setWidget(
                                            if (viewMode.name == "CONTINUOUS") FBReaderView.Widgets.CONTINUOUS
                                            else FBReaderView.Widgets.PAGING
                                        )
                                        isLoaded = true
                                        Timber.d("Book loaded successfully")
                                    } catch (e: Exception) {
                                        Timber.e(e, "Failed to load book in FBReaderView")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { view ->
                            // Обновление view при изменении состояния (без пересоздания)
                            if (isLoaded) {
                                val viewMode = currentState.viewMode
                                view.setWidget(
                                    if (viewMode.name == "CONTINUOUS") FBReaderView.Widgets.CONTINUOUS
                                    else FBReaderView.Widgets.PAGING
                                )
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
                Text(stringResource(R.string.sv_error_prefix, currentState.message))
            }
        }
    }
}

/**
 * Compose диалог содержания (TOC)
 */
@Composable
private fun TocComposeDialog(
    fbReaderView: FBReaderView?,
    onDismiss: () -> Unit,
    onNavigate: (org.geometerplus.zlibrary.text.view.ZLTextPosition) -> Unit,
) {
    // Собираем TOC элементы
    val tocItems = remember(fbReaderView) {
        val items = mutableListOf<TocItem>()
        fbReaderView?.app?.Model?.TOCTree?.let { tree ->
            collectTocItems(tree, items, 0)
        }
        items
    }

    if (tocItems.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.sv_toc_title)) },
            text = { Text(stringResource(R.string.sv_toc_not_available)) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.sv_close))
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.sv_toc_title)) },
            text = {
                LazyColumn {
                    items(tocItems) { item ->
                        Text(
                            text = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate(item.position) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            fontWeight = if (item.level == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.sv_close))
                }
            }
        )
    }
}

private data class TocItem(
    val title: String,
    val position: org.geometerplus.zlibrary.text.view.ZLTextPosition,
    val level: Int = 0
)

private fun collectTocItems(
    tree: org.geometerplus.fbreader.bookmodel.TOCTree,
    items: MutableList<TocItem>,
    level: Int
) {
    for (child in tree.subtrees()) {
        val text = child.text
        val ref = child.reference
        if (text != null && ref != null) {
            items.add(
                TocItem(
                    title = "${"  ".repeat(level)}$text",
                    position = ZLTextFixedPosition(ref.ParagraphIndex, 0, 0),
                    level = level
                )
            )
        }
        collectTocItems(child, items, level + 1)
    }
}

/**
 * Compose BottomSheet для настроек шрифтов
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FontsComposeBottomSheet(
    fbReaderView: FBReaderView?,
    onDismiss: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onIgnoreEmbeddedFontsChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val shared = remember {
        android.preference.PreferenceManager.getDefaultSharedPreferences(context)
    }

    var fontSize by remember {
        mutableFloatStateOf(
            shared.getInt(
                BookApplication.PREFERENCE_FONTSIZE_FBREADER,
                16
            ).toFloat()
        )
    }
    var selectedFont by remember {
        mutableStateOf(
            shared.getString(
                BookApplication.PREFERENCE_FONTFAMILY_FBREADER,
                "sans-serif"
            ) ?: "sans-serif"
        )
    }
    var ignoreEmbeddedFonts by remember {
        mutableStateOf(
            shared.getBoolean(
                BookApplication.PREFERENCE_IGNORE_EMBEDDED_FONTS,
                false
            )
        )
    }

    // Получаем список доступных шрифтов
    val fonts = remember {
        val ttf = BookReaderInitializer.getTTFManager()
        val fontList = mutableListOf("sans-serif", "serif", "monospace")
        ttf?.let {
            // Добавляем системные шрифты
            org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil.ourFontFileMap.keys.forEach { name ->
                if (!fontList.contains(name)) {
                    fontList.add(name)
                }
            }
        }
        fontList.sorted()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Настройки шрифта",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Размер шрифта
            Text(
                text = "Размер шрифта: ${fontSize.toInt()}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = fontSize,
                onValueChange = { newSize ->
                    fontSize = newSize
                    onFontSizeChange(newSize.toInt())
                },
                valueRange = 8f..48f,
                steps = 40,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Выбор шрифта
            Text(
                text = "Шрифт",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                items(fonts) { font ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedFont = font
                                onFontFamilyChange(font)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFont == font,
                            onClick = {
                                selectedFont = font
                                onFontFamilyChange(font)
                            }
                        )
                        Text(
                            text = font,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Игнорировать встроенные шрифты
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        ignoreEmbeddedFonts = !ignoreEmbeddedFonts
                        onIgnoreEmbeddedFontsChange(ignoreEmbeddedFonts)
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = ignoreEmbeddedFonts,
                    onCheckedChange = { checked ->
                        ignoreEmbeddedFonts = checked
                        onIgnoreEmbeddedFontsChange(checked)
                    }
                )
                Text(
                    text = "Игнорировать встроенные шрифты",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
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

        val batteryIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
