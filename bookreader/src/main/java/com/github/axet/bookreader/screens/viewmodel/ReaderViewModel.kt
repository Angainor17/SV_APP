package com.github.axet.bookreader.screens.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.ReaderPreferences
import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.widgets.FBReaderView
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.geometerplus.fbreader.fbreader.ActionCode
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import su.sv.managers.OnBookPagerManager
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel для экрана чтения книги.
 *
 * Использует делегаты для разделения ответственности:
 * - [ReaderSelectionDelegate] — выделение текста
 * - [ReaderBookmarksDelegate] — закладки
 * - [ReaderSearchDelegate] — поиск
 * - [ReaderDisplayDelegate] — режимы отображения
 */
@HiltViewModel
@Suppress("StaticFieldLeak", "DEPRECATION")
class ReaderViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val onBookPagerManager: OnBookPagerManager,
) : ViewModel() {

    private val _state = MutableStateFlow<ReaderState>(ReaderState.Loading)
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private var storage: Storage = Storage(context)
    private var currentBook: Storage.Book? = null
    private var currentFBook: Storage.FBook? = null

    /** Ссылка на FBReaderView (управляется из Compose) */
    var fbReaderView: FBReaderView? = null

    /** Сохранённая позиция для восстановления при пересоздании FBReaderView */
    private var savedPosition: FBReaderView.ZLTextIndexPosition? = null

    /** Настройки */
    val shared: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /** Флаг для управления клавишами громкости */
    var volumeKeysEnabled: Boolean = true

    // ==================== Делегаты ====================

    private val selectionDelegate = ReaderSelectionDelegate(
        getState = { _state.value as? ReaderState.Content },
        updateState = { _state.value = it },
        getFBReaderView = { fbReaderView },
    )

    private val bookmarksDelegate = ReaderBookmarksDelegate(
        getState = { _state.value as? ReaderState.Content },
        updateState = { _state.value = it },
        getFBReaderView = { fbReaderView },
        getCurrentBook = { currentBook },
        getStorage = { storage },
        onSavePosition = { savePosition() },
    )

    val searchDelegate = ReaderSearchDelegate(
        getState = { _state.value as? ReaderState.Content },
        updateState = { _state.value = it },
        getFBReaderView = { fbReaderView },
    )

    private val displayDelegate = ReaderDisplayDelegate(
        getState = { _state.value as? ReaderState.Content },
        updateState = { _state.value = it },
        getFBReaderView = { fbReaderView },
        sharedPreferences = shared,
        onHideSelectionPanel = { selectionDelegate.hidePanel() },
    )

    // ==================== Публичный API ====================

    fun getOnBookPagerManager(): OnBookPagerManager = onBookPagerManager

    fun getFBook(): Storage.FBook? = currentFBook
    fun getCurrentBook(): Storage.Book? = currentBook

    fun getSavedPosition(): FBReaderView.ZLTextIndexPosition? = savedPosition

    fun clearSavedPosition() {
        savedPosition = null
    }

    fun updateCanChangeFont() {
        val canChange = fbReaderView?.canChangeFont() ?: true
        val currentState = _state.value as? ReaderState.Content ?: return
        if (currentState.canChangeFont != canChange) {
            _state.value = currentState.copy(canChangeFont = canChange)
        }
    }

    // ==================== Actions ====================

    fun onAction(action: ReaderActions) {
        when (action) {
            // Загрузка книги
            is ReaderActions.LoadBook -> loadBook(
                action.uri,
                action.position,
                action.bookCoverUrl,
                action.bookTitle,
                action.bookAuthor
            )

            // Сохранение позиции
            ReaderActions.SavePosition -> savePosition()

            // Навигация
            ReaderActions.NavigateBack -> { /* Обрабатывается на уровне Screen */
            }

            ReaderActions.NavigateToSettings -> { /* Обрабатывается на уровне Screen */
            }

            is ReaderActions.GoToPosition -> goToPosition(action.position)
            is ReaderActions.GoToBookmark -> goToBookmark(action.bookmark)

            // Отображение — делегировано
            ReaderActions.ToggleFullscreen -> displayDelegate.toggleFullscreen()
            ReaderActions.ToggleViewMode -> displayDelegate.toggleViewMode()
            ReaderActions.ToggleReflow -> displayDelegate.toggleReflow()
            ReaderActions.MarkControlsHintShown -> displayDelegate.markControlsHintShown()
            is ReaderActions.SetFullscreen -> displayDelegate.setFullscreen(action.isFullscreen)

            // Диалоги
            ReaderActions.ToggleToc -> toggleToc()
            ReaderActions.ToggleBookmarks -> toggleBookmarks()
            ReaderActions.ToggleFontSettings -> toggleFontSettings()
            ReaderActions.ToggleNavigation -> toggleNavigation()
            is ReaderActions.GoToPage -> goToPage(action.page)
            ReaderActions.HideDialogs -> hideDialogs()

            // Выделение — делегировано
            is ReaderActions.ShowSelection -> selectionDelegate.show(action.startY, action.endY)
            ReaderActions.HideSelection -> {
                fbReaderView?.app?.runAction(ActionCode.SELECTION_CLEAR)
            }

            ReaderActions.SelectionCopy -> selectionDelegate.copy()
            ReaderActions.SelectionShare -> selectionDelegate.share()
            ReaderActions.SelectionBookmark -> selectionDelegate.bookmark()
            ReaderActions.SelectionQuestion -> selectionDelegate.question()
            ReaderActions.SelectionAlert -> selectionDelegate.alert()

            // Закладки — делегировано
            is ReaderActions.EditBookmark -> bookmarksDelegate.edit(action.bookmark)
            is ReaderActions.SaveBookmarkEdit -> bookmarksDelegate.saveEdit(
                action.bookmark,
                action.name,
                action.color
            )

            is ReaderActions.AddBookmark -> bookmarksDelegate.add(action.bookmark)
            is ReaderActions.DeleteBookmark -> bookmarksDelegate.delete(action.bookmark)

            // Шрифты
            is ReaderActions.SetFontSize -> setFontSize(action.size)
            is ReaderActions.SetReflowFontSize -> setReflowFontSize(action.size)
            is ReaderActions.SetFontFamily -> setFontFamily(action.family)
            is ReaderActions.SetIgnoreEmbeddedFonts -> setIgnoreEmbeddedFonts(action.ignore)

            // Поиск — делегирован
            is ReaderActions.Search -> searchDelegate.search(action.query)
            ReaderActions.SearchNext -> searchDelegate.next()
            ReaderActions.SearchPrevious -> searchDelegate.previous()
            ReaderActions.SearchClose -> searchDelegate.close()

            // Zoom
            is ReaderActions.ZoomUpdate -> zoomUpdate(action.scale, action.pivotX, action.pivotY)
            ReaderActions.ZoomReset -> zoomReset()
        }
    }

    // ==================== Загрузка книги ====================

    private fun loadBook(
        uri: Uri,
        position: FBReaderView.ZLTextIndexPosition?,
        bookCoverUrl: String?,
        bookTitle: String?,
        bookAuthor: String?
    ) {
        Timber.tag("voronin").d("=== loadBook START ===")
        Timber.tag("voronin").d("uri=$uri, position=$position")

        viewModelScope.launch {
            _state.value = ReaderState.Loading
            if (position != null) savedPosition = position

            try {
                val inputStream = try {
                    context.contentResolver.openInputStream(uri)
                } catch (e: SecurityException) {
                    Timber.tag("voronin").e(e, "Security exception accessing file: $uri")
                    _state.value =
                        ReaderState.Error(context.getString(R.string.sv_error_file_access))
                    return@launch
                } catch (e: Exception) {
                    Timber.tag("voronin").e(e, "Error accessing file: $uri")
                    null
                }

                if (inputStream == null) {
                    _state.value =
                        ReaderState.Error(context.getString(R.string.sv_error_file_not_found))
                    return@launch
                }
                inputStream.close()

                currentBook = storage.load(uri)
                if (bookCoverUrl != null) currentBook?.info?.coverUrl = bookCoverUrl
                if (bookTitle != null) currentBook?.info?.title = bookTitle
                if (bookAuthor != null) currentBook?.info?.authors = bookAuthor

                currentFBook = storage.read(currentBook)
                ensureCoverCreated(currentBook, currentFBook)

                _state.value = ReaderState.Content(
                    book = currentBook!!,
                    positionText = "",
                    isFullscreen = false,
                    viewMode = displayDelegate.getViewModeFromPrefs(),
                )
            } catch (e: Exception) {
                Timber.tag("voronin").e(e, "=== loadBook FAILED ===")
                val errorMessage = when {
                    e.message?.contains("EACCES") == true -> context.getString(R.string.sv_error_file_access)
                    e.message?.contains("ENOENT") == true || e.message?.contains("No such file") == true ->
                        context.getString(R.string.sv_error_file_not_found)

                    else -> e.message ?: context.getString(R.string.sv_error_open_book)
                }
                _state.value = ReaderState.Error(errorMessage)
            }
        }
    }

    private fun ensureCoverCreated(book: Storage.Book?, fbook: Storage.FBook?) {
        if (book == null || fbook == null) return
        try {
            var needSave = false
            if (book.info?.coverUrl == null) {
                val coverFile = Storage.coverFile(context, book)
                if (coverFile != null && !coverFile.exists()) {
                    storage.createCover(fbook, coverFile)
                }
                if (coverFile?.exists() == true) {
                    book.info?.coverUrl = coverFile.absolutePath
                    needSave = true
                }
            }
            if (book.info?.bookFileUri == null && book.url != null) {
                book.info?.bookFileUri = book.url.toString()
                needSave = true
            }
            if (needSave) storage.save(book)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create cover or save bookFileUri")
        }
    }

    // ==================== Сохранение позиции ====================

    fun savePosition() {
        val book = currentBook ?: return
        val fb = fbReaderView ?: return
        val fbBook = fb.book ?: return

        try {
            savedPosition = fb.position as? FBReaderView.ZLTextIndexPosition
            val save = Storage.RecentInfo(fbBook.info)
            save.position = fb.position

            val uri = storage.recentUri(book)
            if (Storage.exists(context, uri)) {
                try {
                    val info = Storage.RecentInfo(context, uri)
                    if (info.position != null && save.position != null &&
                        save.position!!.samePositionAs(info.position)
                    ) {
                        if (save.fontsize == null || info.fontsize != null && save.fontsize == info.fontsize) {
                            if (save.equals(info.fontsizes)) {
                                if (save.bookmarks == null || info.bookmarks != null && save.bookmarks == info.bookmarks) {
                                    return
                                }
                            }
                        }
                    }
                    if (book.info.last != info.last) {
                        storage.move(uri, storage.storagePath)
                    }
                    save.merge(info.fontsizes, info.last)
                } catch (e: RuntimeException) {
                    Timber.d(e, "Unable to load JSON")
                }
            }

            book.info = save
            storage.save(book)
            Timber.d("Position saved: ${save.position}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save position")
        }
    }

    // ==================== Навигация ====================

    private fun goToPosition(position: ZLTextPosition) {
        fbReaderView?.gotoPosition(position)
    }

    private fun goToBookmark(bookmark: Storage.Bookmark) {
        fbReaderView?.gotoPosition(FBReaderView.ZLTextIndexPosition(bookmark.start, bookmark.end))
    }

    // ==================== Диалоги ====================

    private fun toggleToc() {
        val s = _state.value as? ReaderState.Content ?: return
        _state.value = s.copy(showToc = !s.showToc, showBookmarks = false, showFontSettings = false)
    }

    private fun toggleBookmarks() {
        val s = _state.value as? ReaderState.Content ?: return
        _state.value = s.copy(
            showToc = false,
            showBookmarks = !s.showBookmarks,
            showFontSettings = false,
            showNavigation = false
        )
    }

    private fun toggleNavigation() {
        val s = _state.value as? ReaderState.Content ?: return
        _state.value = s.copy(
            showToc = false,
            showBookmarks = false,
            showFontSettings = false,
            showNavigation = !s.showNavigation
        )
    }

    private fun toggleFontSettings() {
        val s = _state.value as? ReaderState.Content ?: return
        _state.value = s.copy(
            showToc = false,
            showBookmarks = false,
            showFontSettings = !s.showFontSettings,
            showNavigation = false
        )
    }

    fun hideDialogs() {
        val s = _state.value as? ReaderState.Content ?: return
        _state.value = s.copy(
            showToc = false, showBookmarks = false, showFontSettings = false,
            showBookmarkEdit = false, showNavigation = false, editingBookmark = null
        )
    }

    private fun goToPage(page: Int) {
        fbReaderView?.let { view ->
            val textView = view.app?.getTextView() ?: return
            if (page == 1) textView.gotoHome() else textView.gotoPage(page)
            view.app?.getViewWidget()?.reset()
            view.app?.getViewWidget()?.repaint()
        }
    }

    // ==================== Выделение (прокси к делегату) ====================

    fun hideSelection() = selectionDelegate.hide()

    // ==================== Закладки (прокси к делегату) ====================

    fun syncBookmarksFromFBook() = bookmarksDelegate.syncFromFBook()
    fun migrateBookmarksContextAsync() {
        val bookmarks = currentBook?.info?.bookmarks ?: return
        val hasUnmigrated = bookmarks.any { it.sentenceBefore == null && it.sentenceAfter == null }
        if (!hasUnmigrated) return
        bookmarksDelegate.migrateContext()
    }

    // ==================== Шрифты ====================

    private fun setFontSize(size: Int) {
        shared.edit { putInt(ReaderPreferences.PREFERENCE_FONTSIZE_FBREADER, size) }
        fbReaderView?.setFontsizeFB(size)
    }

    private fun setReflowFontSize(size: Float) {
        shared.edit { putFloat(ReaderPreferences.PREFERENCE_FONTSIZE_REFLOW, size) }
        fbReaderView?.setFontsizeReflow(size)
    }

    private fun setFontFamily(family: String) {
        shared.edit { putString(ReaderPreferences.PREFERENCE_FONTFAMILY_FBREADER, family) }
        fbReaderView?.setFontFB(family)
    }

    private fun setIgnoreEmbeddedFonts(ignore: Boolean) {
        shared.edit { putBoolean(ReaderPreferences.PREFERENCE_IGNORE_EMBEDDED_FONTS, ignore) }
        fbReaderView?.setIgnoreCssFonts(ignore)
    }

    // ==================== Zoom ====================

    private fun zoomUpdate(scale: Float, pivotX: Float, pivotY: Float) {
        val s = _state.value as? ReaderState.Content ?: return
        _state.value = s.copy(
            zoomScale = scale,
            zoomPivotX = pivotX,
            zoomPivotY = pivotY,
            isInZoom = scale > 1.0f
        )
    }

    private fun zoomReset() {
        val s = _state.value as? ReaderState.Content ?: return
        fbReaderView?.resetZoom()
        _state.value = s.copy(zoomScale = 1.0f, zoomPivotX = 0f, zoomPivotY = 0f, isInZoom = false)
    }

    // ==================== Жизненный цикл ====================

    fun closeBook() {
        savePosition()
        fbReaderView?.closeBook()
        currentFBook?.close()
        currentFBook = null
        currentBook = null
    }

    override fun onCleared() {
        super.onCleared()
        closeBook()
    }
}
