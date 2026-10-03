package com.github.axet.bookreader.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.screens.ui.BatteryReceiver
import com.github.axet.bookreader.screens.ui.BookmarkBottomSheet
import com.github.axet.bookreader.screens.ui.BookmarksComposeDialog
import com.github.axet.bookreader.screens.ui.FontsComposeBottomSheet
import com.github.axet.bookreader.screens.ui.NavigationComposeDialog
import com.github.axet.bookreader.screens.ui.ReaderTopBar
import com.github.axet.bookreader.screens.ui.SelectionComposePanel
import com.github.axet.bookreader.screens.ui.TocComposeDialog
import com.github.axet.bookreader.screens.ui.VolumeKeysHandler
import com.github.axet.bookreader.screens.viewmodel.ReaderActions
import com.github.axet.bookreader.screens.viewmodel.ReaderState
import com.github.axet.bookreader.screens.viewmodel.ReaderViewModel
import com.github.axet.bookreader.widgets.FBReaderView
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
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
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
            viewModel.onAction(
                ReaderActions.LoadBook(
                    uri = bookUri,
                    position = position,
                    bookCoverUrl = bookCoverUrl,
                    bookTitle = bookTitle,
                    bookAuthor = bookAuthor
                )
            )
        }
    }

    // Обработка батареи
    BatteryReceiver(fbReaderView)

    // Обработка клавиш громкости
    VolumeKeysHandler(fbReaderView, viewModel)

    // Сохранение позиции при уходе с экрана
    DisposableEffect(Unit) {
        onDispose {
            viewModel.savePosition()
            // Выходим из fullscreen режима при закрытии экрана
            fbReaderView?.exitFullscreen()
            // Закрываем View — освобождаем pluginview/tts. Сама книга (currentFBook)
            // остаётся в ViewModel и перезагрузится при возврате на экран.
            fbReaderView?.closeBook()
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
                val pagePosition = fbReaderView?.app?.textView?.pagePosition()
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
                    bookmarkText = currentState.editingBookmark.text.orEmpty(),
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
                                            Timber.tag("voronin")
                                                .d("NavigateBack in fullscreen - exiting fullscreen first")
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
                                    override fun onScrollingFinished(index: ZLViewEnums.PageIndex?) {
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

                                    override fun onEditBookmark(bookmark: Storage.Bookmark) {
                                        viewModel.onAction(ReaderActions.EditBookmark(bookmark))
                                    }

                                    override fun onFullscreenToggle(isFullscreen: Boolean) {
                                        Timber.tag("voronin")
                                            .d("=== ReaderContent: onFullscreenToggle($isFullscreen) ===")
                                        Timber.tag("voronin")
                                            .d("Current viewMode: ${currentState.viewMode}")
                                        Timber.tag("voronin")
                                            .d("Scaffold topBar visible: ${!currentState.isFullscreen}")
                                        viewModel.onAction(ReaderActions.SetFullscreen(isFullscreen))
                                    }

                                    override fun onNavigationRequest() {
                                        viewModel.onAction(ReaderActions.ToggleNavigation)
                                    }

                                    override fun onSelectionShow(startY: Int, endY: Int) {
                                        viewModel.onAction(
                                            ReaderActions.ShowSelection(
                                                startY,
                                                endY
                                            )
                                        )
                                    }

                                    override fun onSelectionHide() {
                                        // Вызываем hideSelection напрямую, не через action, чтобы избежать цикла
                                        viewModel.hideSelection()
                                    }

                                    override fun onZoomChange(
                                        scale: Float,
                                        pivotX: Float,
                                        pivotY: Float
                                    ) {
                                        // Zoom is applied directly to FBReaderView via scaleX/Y
                                        // Optionally notify ViewModel for UI state (zoom indicator)
                                        Timber.tag("voronin")
                                            .d("ReaderContent: onZoomChange scale=$scale pivot=$pivotX,$pivotY")
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
                                        // Если есть позиция закладки — инжектируем её в fbook.info.position
                                        // ДО вызова loadBook, чтобы библиотека навигировала туда синхронно
                                        // (тот же путь что и "продолжить чтение" — работает корректно для PDF)
                                        val savedPos = viewModel.getSavedPosition()
                                        if (savedPos != null) {
                                            viewModel.clearSavedPosition()
                                            if (fbook.info == null) fbook.info =
                                                Storage.RecentInfo()
                                            fbook.info!!.position = savedPos
                                        }

                                        loadBook(fbook)
                                        // Устанавливаем режим просмотра
                                        val viewMode = currentState.viewMode
                                        setWidget(
                                            if (viewMode.name == "CONTINUOUS") FBReaderView.Widgets.CONTINUOUS
                                            else FBReaderView.Widgets.PAGING
                                        )

                                        // Обновляем возможность смены шрифта
                                        viewModel.updateCanChangeFont()

                                        // Миграция контекста для старых заметок (после инициализации BookTextView)
                                        viewModel.migrateBookmarksContextAsync()

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
                                val desiredWidget =
                                    if (viewMode.name == "CONTINUOUS") FBReaderView.Widgets.CONTINUOUS
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
