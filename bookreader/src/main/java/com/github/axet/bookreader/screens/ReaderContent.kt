package com.github.axet.bookreader.screens

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.BookReaderInitializer
import com.github.axet.bookreader.app.ReaderPreferences
import com.github.axet.bookreader.screens.testing.ReaderTestTags
import com.github.axet.bookreader.screens.ui.BookmarkBottomSheet
import com.github.axet.bookreader.screens.ui.BookmarksComposeDialog
import com.github.axet.bookreader.screens.ui.NavigationComposeDialog
import com.github.axet.bookreader.screens.ui.ReaderTopBar
import com.github.axet.bookreader.screens.ui.SelectionComposePanel
import com.github.axet.bookreader.screens.viewmodel.ReaderActions
import com.github.axet.bookreader.screens.viewmodel.ReaderState
import com.github.axet.bookreader.screens.viewmodel.ReaderViewModel
import com.github.axet.bookreader.widgets.FBReaderView
import org.geometerplus.fbreader.fbreader.ActionCode
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import su.sv.managers.theme.ThemeViewModel
import timber.log.Timber

/**
 * Контент экрана чтения книги
 */
@Composable
fun ReaderContent(
    bookUri: Uri,
    bookCoverUrl: String?,
    bookTitle: String?,
    bookAuthor: String?,
    bookmarkPosition: BookmarkPosition?,
    onNavigateBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

    // Состояние для FBReaderView
    var fbReaderView by remember { mutableStateOf<FBReaderView?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    // Загрузка книги только если ещё не загружена
    LaunchedEffect(bookUri) {
        // Проверяем, что книга ещё не загружена в ViewModel
        if (viewModel.getCurrentBook() == null) {
            // Создаём позицию из BookmarkPosition если она есть
            val position = bookmarkPosition?.let { pos ->
                FBReaderView.ZLTextIndexPosition(
                    ZLTextFixedPosition(pos.startParagraph, pos.startElement, pos.startChar),
                    ZLTextFixedPosition(pos.endParagraph, pos.endElement, pos.endChar)
                )
            }
            viewModel.onAction(ReaderActions.LoadBook(bookUri, position, bookCoverUrl, bookTitle, bookAuthor))
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
            // Выходим из fullscreen режима при закрытии экрана
            fbReaderView?.exitFullscreen()
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

            // Навигация по страницам
            if (currentState.showNavigation && fbReaderView != null) {
                val pagePosition = fbReaderView?.app?.getTextView()?.pagePosition()
                val currentPage = pagePosition?.Current ?: 1
                val totalPages = pagePosition?.Total ?: 1
                val chapterTitle = fbReaderView?.app?.getCurrentTOCElement()?.text

                NavigationComposeDialog(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    chapterTitle = chapterTitle,
                    onPageChange = { page ->
                        viewModel.onAction(ReaderActions.GoToPage(page))
                    },
                    onConfirm = {
                        viewModel.savePosition()
                    },
                    onCancel = {
                        // Вернуться к исходной позиции
                    },
                    onDismiss = { viewModel.onAction(ReaderActions.HideDialogs) }
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
                containerColor = MaterialTheme.colorScheme.background,  // Мягкий светло-серый фон
                topBar = {
                    // Animated visibility for smooth fullscreen transition
                    AnimatedVisibility(
                        visible = !currentState.isFullscreen,
                        enter = fadeIn(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        ReaderTopBar(
                            state = currentState,
                            onAction = { action ->
                                when (action) {
                                    ReaderActions.NavigateBack -> {
                                        // Если fullscreen - сначала exit fullscreen
                                        if (currentState.isFullscreen) {
                                            Timber.tag("voronin").d("NavigateBack in fullscreen - exiting fullscreen first")
                                            viewModel.onAction(ReaderActions.SetFullscreen(false))
                                            fbReaderView?.exitFullscreen()
                                        } else {
                                            onNavigateBack()
                                        }
                                    }
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

                                    override fun onFullscreenToggle(isFullscreen: Boolean) {
                                        Timber.tag("voronin").d("=== ReaderContent: onFullscreenToggle($isFullscreen) ===")
                                        Timber.tag("voronin").d("Current viewMode: ${currentState.viewMode}")
                                        Timber.tag("voronin").d("Scaffold topBar visible: ${!currentState.isFullscreen}")
                                        viewModel.onAction(ReaderActions.SetFullscreen(isFullscreen))
                                    }

                                    override fun onNavigationRequest() {
                                        viewModel.onAction(ReaderActions.ToggleNavigation)
                                    }

                                    override fun onSelectionShow(startY: Int, endY: Int) {
                                        viewModel.onAction(ReaderActions.ShowSelection(startY, endY))
                                    }

                                    override fun onSelectionHide() {
                                        // Вызываем hideSelection напрямую, не через action, чтобы избежать цикла
                                        viewModel.hideSelection()
                                    }

                                    override fun onZoomChange(scale: Float, pivotX: Float, pivotY: Float) {
                                        // Zoom is applied directly to FBReaderView via scaleX/Y
                                        // Optionally notify ViewModel for UI state (zoom indicator)
                                        Timber.tag("voronin").d("ReaderContent: onZoomChange scale=$scale pivot=$pivotX,$pivotY")
                                    }

                                    override fun onZoomEnd() {
                                        Timber.tag("voronin").d("ReaderContent: onZoomEnd")
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

                                        // Применяем сохранённую позицию (из заметки)
                                        val savedPos = viewModel.getSavedPosition()
                                        if (savedPos != null) {
                                            Timber.d("Applying saved position from bookmark: $savedPos")
                                            gotoPosition(savedPos)
                                            // Сбрасываем savedPosition чтобы при следующем открытии использовалась сохранённая в файле
                                            viewModel.clearSavedPosition()
                                        }

                                        // Обновляем возможность смены шрифта
                                        viewModel.updateCanChangeFont()

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
                                val desiredWidget = if (viewMode.name == "CONTINUOUS") FBReaderView.Widgets.CONTINUOUS
                                else FBReaderView.Widgets.PAGING

                                // Только переключаем widget если он отличается от текущего
                                // Это предотвращает удаление SelectionView во время touch
                                val currentWidget = view.getWidgetType()
                                if (currentWidget != desiredWidget) {
                                    Timber.d("Switching widget from $currentWidget to $desiredWidget")
                                    view.setWidget(desiredWidget)
                                }

                                // Показываем подсказки зон касания при первом открытии
                                // Делаем это в update, когда view уже имеет размер
                                if (!currentState.hasShownControlsHint && view.width > 0) {
                                    view.postDelayed({
                                        view.showControls()
                                        viewModel.onAction(ReaderActions.MarkControlsHintShown)
                                    }, 300)
                                }
                            }
                        }
                    )

                    // Панель выделения текста (показывается поверх FBReaderView)
                    if (currentState.showSelection) {
                        val showAtBottom = currentState.selectionEndY > currentState.selectionStartY
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    bottom = if (showAtBottom) 50.dp else 0.dp,
                                    top = if (!showAtBottom) 100.dp else 0.dp
                                ),
                            contentAlignment = if (showAtBottom) {
                                Alignment.BottomCenter
                            } else {
                                Alignment.TopCenter
                            }
                        ) {
                            SelectionComposePanel(
                                onBookmark = { viewModel.onAction(ReaderActions.SelectionBookmark) },
                                onShare = { viewModel.onAction(ReaderActions.SelectionShare) },
                                onCopy = { viewModel.onAction(ReaderActions.SelectionCopy) },
                                onQuestion = { viewModel.onAction(ReaderActions.SelectionQuestion) },
                                onAlert = { viewModel.onAction(ReaderActions.SelectionAlert) },
                                onClose = { viewModel.onAction(ReaderActions.HideSelection) }
                            )
                        }
                    }
                }

                // BackHandler для system back button (fullscreen)
                BackHandler(enabled = currentState.isFullscreen) {
                    Timber.tag("voronin").d("System back in fullscreen - exiting fullscreen")
                    viewModel.onAction(ReaderActions.SetFullscreen(false))
                    fbReaderView?.exitFullscreen()
                }

                // BackHandler для zoom mode
                BackHandler(enabled = currentState.isInZoom) {
                    Timber.tag("voronin").d("System back in zoom - resetting zoom")
                    viewModel.onAction(ReaderActions.ZoomReset)
                }

                // BackHandler для search mode
                BackHandler(enabled = currentState.searchState.isActive) {
                    Timber.tag("voronin").d("System back in search - closing search")
                    viewModel.onAction(ReaderActions.SearchClose)
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
 * Compose диалог содержания (TOC) с вложенной иерархией
 */
@Composable
private fun TocComposeDialog(
    fbReaderView: FBReaderView?,
    onDismiss: () -> Unit,
    onNavigate: (org.geometerplus.zlibrary.text.view.ZLTextPosition) -> Unit,
) {
    // Собираем TOC элементы с информацией о дочерних элементах
    val tocItems = remember(fbReaderView) {
        val items = mutableListOf<ExpandableTocItem>()
        fbReaderView?.app?.Model?.TOCTree?.let { tree ->
            collectExpandableTocItems(tree, items, 0)
        }
        items
    }

    // Состояние раскрытия для каждого элемента
    val expandedStates = remember { mutableStateListOf<String>() }

    // Функция для проверки видимости элемента
    fun isItemVisible(item: ExpandableTocItem): Boolean {
        // Элементы уровня 0 всегда видимы
        if (item.parentId == null) return true
        // Проверяем все родительские цепочки
        var currentParentId: String? = item.parentId
        while (currentParentId != null) {
            if (!expandedStates.contains(currentParentId)) return false
            // Находим parentId родителя
            val parentItem = tocItems.find { it.id == currentParentId }
            currentParentId = parentItem?.parentId
        }
        return true
    }

    if (tocItems.isEmpty()) {
        AlertDialog(
            modifier = Modifier.testTag(ReaderTestTags.Toc.DIALOG),
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.sv_toc_title)) },
            text = {
                Text(
                    modifier = Modifier.testTag(ReaderTestTags.Toc.EMPTY_STATE),
                    text = stringResource(R.string.sv_toc_not_available)
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.sv_close))
                }
            }
        )
    } else {
        AlertDialog(
            modifier = Modifier.testTag(ReaderTestTags.Toc.DIALOG),
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.sv_toc_title)) },
            text = {
                LazyColumn {
                    items(tocItems, key = { it.id }) { item ->
                        // Анимированное появление/исчезновение
                        AnimatedVisibility(
                            visible = isItemVisible(item),
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            TocItemRow(
                                item = item,
                                isExpanded = expandedStates.contains(item.id),
                                onToggleExpand = {
                                    if (expandedStates.contains(item.id)) {
                                        expandedStates.remove(item.id)
                                    } else {
                                        expandedStates.add(item.id)
                                    }
                                },
                                onNavigate = onNavigate
                            )
                        }
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

/**
 * Элемент оглавления с возможностью раскрытия
 */
@Composable
private fun TocItemRow(
    item: ExpandableTocItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigate: (org.geometerplus.zlibrary.text.view.ZLTextPosition) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(item.position) }
            .padding(
                vertical = 12.dp,
                horizontal = 8.dp + (item.level * 16).dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Иконка главы
        Icon(
            imageVector = Icons.Default.List,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // Заголовок главы
        Text(
            text = item.title,
            fontWeight = if (item.level == 0) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        // Иконка раскрытия/закрытия для элементов с дочерними
        if (item.hasChildren) {
            IconButton(onClick = onToggleExpand) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Скрыть" else "Раскрыть",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Модель элемента оглавления с поддержкой раскрытия
 */
private data class ExpandableTocItem(
    val id: String,
    val title: String,
    val position: org.geometerplus.zlibrary.text.view.ZLTextPosition,
    val level: Int = 0,
    val hasChildren: Boolean = false,
    val parentId: String? = null,  // ID родительского элемента
)

private fun collectExpandableTocItems(
    tree: org.geometerplus.fbreader.bookmodel.TOCTree,
    items: MutableList<ExpandableTocItem>,
    level: Int,
    parentId: String? = null
) {
    for (child in tree.subtrees()) {
        val text = child.text
        val ref = child.reference
        if (text != null && ref != null) {
            val hasChildren = child.subtrees().iterator().hasNext()
            val itemId = "${level}_${ref.ParagraphIndex}_${text.hashCode()}"
            items.add(
                ExpandableTocItem(
                    id = itemId,
                    title = text,
                    position = ZLTextFixedPosition(ref.ParagraphIndex, 0, 0),
                    level = level,
                    hasChildren = hasChildren,
                    parentId = parentId
                )
            )
            // Рекурсивно собираем дочерние элементы с текущим parentId
            collectExpandableTocItems(child, items, level + 1, itemId)
        } else {
            // Если нет текста/рефа, продолжаем обход с тем же parentId
            collectExpandableTocItems(child, items, level, parentId)
        }
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
                ReaderPreferences.PREFERENCE_FONTSIZE_FBREADER,
                16
            ).toFloat()
        )
    }
    var selectedFont by remember {
        mutableStateOf(
            shared.getString(
                ReaderPreferences.PREFERENCE_FONTFAMILY_FBREADER,
                "sans-serif"
            ) ?: "sans-serif"
        )
    }
    var ignoreEmbeddedFonts by remember {
        mutableStateOf(
            shared.getBoolean(
                ReaderPreferences.PREFERENCE_IGNORE_EMBEDDED_FONTS,
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
        modifier = Modifier.testTag(ReaderTestTags.FontSettings.SHEET),
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
                modifier = Modifier.testTag(ReaderTestTags.FontSettings.SIZE_VALUE),
                text = "Размер шрифта: ${fontSize.toInt()}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                modifier = Modifier.testTag(ReaderTestTags.FontSettings.SIZE_SLIDER),
                value = fontSize,
                onValueChange = { newSize ->
                    fontSize = newSize
                    onFontSizeChange(newSize.toInt())
                },
                valueRange = 8f..48f,
                steps = 40
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
                items(
                    items = fonts,
                    key = { font -> font },
                ) { font ->
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
        shared.getBoolean(ReaderPreferences.PREFERENCE_VOLUME_KEYS, false)
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
