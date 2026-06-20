package com.github.axet.bookreader.screens.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.preference.PreferenceManager
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.axet.bookreader.app.BookApplication
import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.widgets.FBReaderView
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel для экрана чтения книги
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<ReaderState>(ReaderState.Loading)
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private var storage: Storage = Storage(context)
    private var currentBook: Storage.Book? = null
    private var currentFBook: Storage.FBook? = null

    // Ссылка на FBReaderView (управляется из Compose)
    var fbReaderView: FBReaderView? = null

    // Настройки
    private val shared: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // Флаг для управления клавишами громкости
    var volumeKeysEnabled: Boolean = true

    /**
     * Обработка действий пользователя
     */
    fun onAction(action: ReaderActions) {
        when (action) {
            // Загрузка книги
            is ReaderActions.LoadBook -> loadBook(action.uri, action.position)

            // Сохранение позиции
            ReaderActions.SavePosition -> savePosition()

            // Навигация
            ReaderActions.NavigateBack -> { /* Обрабатывается на уровне Screen */ }
            ReaderActions.NavigateToSettings -> { /* Обрабатывается на уровне Screen */ }
            is ReaderActions.GoToPosition -> goToPosition(action.position)
            is ReaderActions.GoToBookmark -> goToBookmark(action.bookmark)

            // Отображение
            ReaderActions.ToggleFullscreen -> toggleFullscreen()
            ReaderActions.ToggleViewMode -> toggleViewMode()
            ReaderActions.ToggleRtl -> toggleRtl()
            ReaderActions.ToggleReflow -> toggleReflow()

            // Диалоги
            ReaderActions.ToggleToc -> toggleToc()
            ReaderActions.ToggleBookmarks -> toggleBookmarks()
            ReaderActions.ToggleFontSettings -> toggleFontSettings()
            ReaderActions.HideDialogs -> hideDialogs()

            // TTS
            ReaderActions.ToggleTts -> toggleTts()

            // Закладки
            is ReaderActions.AddBookmark -> addBookmark(action.bookmark)
            is ReaderActions.DeleteBookmark -> deleteBookmark(action.bookmark)

            // Шрифты
            is ReaderActions.SetFontSize -> setFontSize(action.size)
            is ReaderActions.SetReflowFontSize -> setReflowFontSize(action.size)
            is ReaderActions.SetFontFamily -> setFontFamily(action.family)
            is ReaderActions.SetIgnoreEmbeddedFonts -> setIgnoreEmbeddedFonts(action.ignore)

            // Поиск
            is ReaderActions.Search -> search(action.query)
            ReaderActions.SearchNext -> searchNext()
            ReaderActions.SearchPrevious -> searchPrevious()
            ReaderActions.SearchClose -> searchClose()
        }
    }

    // ==================== Загрузка книги ====================

    private fun loadBook(uri: Uri, position: FBReaderView.ZLTextIndexPosition?) {
        viewModelScope.launch {
            _state.value = ReaderState.Loading

            try {
                // Загружаем информацию о книге
                currentBook = storage.load(uri)

                // Открываем файл книги
                currentFBook = storage.read(currentBook)

                // Обновляем состояние
                val currentState = ReaderState.Content(
                    book = currentBook!!,
                    positionText = "",
                    isFullscreen = false,
                    viewMode = getViewModeFromPrefs(),
                )
                _state.value = currentState

                Timber.d("Book loaded: ${currentBook?.info?.title}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load book")
                _state.value = ReaderState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ==================== Сохранение позиции ====================

    fun savePosition() {
        val book = currentBook ?: return
        val fb = fbReaderView ?: return
        val fbBook = fb.book ?: return

        try {
            val save = Storage.RecentInfo(fbBook.info)
            save.position = fb.position

            val uri = storage.recentUri(book)
            if (Storage.exists(context, uri)) {
                try {
                    val info = Storage.RecentInfo(context, uri)
                    // Проверяем конфликты при синхронизации
                    if (info.position != null && save.position != null &&
                        save.position!!.samePositionAs(info.position)) {
                        if (save.fontsize == null || info.fontsize != null && save.fontsize == info.fontsize) {
                            if (save.equals(info.fontsizes)) {
                                if (save.bookmarks == null || info.bookmarks != null && save.bookmarks == info.bookmarks) {
                                    return // Нечего сохранять
                                }
                            }
                        }
                    }

                    // Файл изменился между сохранениями?
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
        fbReaderView?.gotoPosition(
            FBReaderView.ZLTextIndexPosition(bookmark.start, bookmark.end)
        )
    }

    // ==================== Отображение ====================

    private fun toggleFullscreen() {
        val currentState = _state.value as? ReaderState.Content ?: return
        _state.value = currentState.copy(isFullscreen = !currentState.isFullscreen)
    }

    private fun toggleViewMode() {
        val currentState = _state.value as? ReaderState.Content ?: return
        val newMode = if (currentState.viewMode == ViewMode.PAGING) {
            ViewMode.CONTINUOUS
        } else {
            ViewMode.PAGING
        }

        // Сохраняем в prefs
        shared.edit {
            putString(BookApplication.PREFERENCE_VIEW_MODE, newMode.toString())
        }

        // Применяем к FBReaderView
        fbReaderView?.setWidget(
            if (newMode == ViewMode.CONTINUOUS) FBReaderView.Widgets.CONTINUOUS
            else FBReaderView.Widgets.PAGING
        )

        _state.value = currentState.copy(viewMode = newMode)
    }

    private fun toggleRtl() {
        val currentState = _state.value as? ReaderState.Content ?: return
        fbReaderView?.app?.BookTextView?.let { view ->
            view.rtlMode = !view.rtlMode
            fbReaderView?.reset()
        }
        _state.value = currentState.copy(rtlMode = !currentState.rtlMode)
    }

    private fun toggleReflow() {
        val currentState = _state.value as? ReaderState.Content ?: return
        fbReaderView?.setReflow(!fbReaderView!!.isReflow)
        _state.value = currentState.copy(isReflow = !currentState.isReflow)
    }

    // ==================== Диалоги ====================

    private fun toggleToc() {
        val currentState = _state.value as? ReaderState.Content ?: return
        _state.value = currentState.copy(
            showToc = !currentState.showToc,
            showBookmarks = false,
            showFontSettings = false
        )
    }

    private fun toggleBookmarks() {
        val currentState = _state.value as? ReaderState.Content ?: return
        _state.value = currentState.copy(
            showToc = false,
            showBookmarks = !currentState.showBookmarks,
            showFontSettings = false
        )
    }

    private fun toggleFontSettings() {
        val currentState = _state.value as? ReaderState.Content ?: return
        _state.value = currentState.copy(
            showToc = false,
            showBookmarks = false,
            showFontSettings = !currentState.showFontSettings
        )
    }

    private fun hideDialogs() {
        val currentState = _state.value as? ReaderState.Content ?: return
        _state.value = currentState.copy(
            showToc = false,
            showBookmarks = false,
            showFontSettings = false
        )
    }

    // ==================== TTS ====================

    private fun toggleTts() {
        val currentState = _state.value as? ReaderState.Content ?: return
        val fb = fbReaderView ?: return

        if (fb.tts != null) {
            fb.tts?.dismiss()
            fb.tts = null
            _state.value = currentState.copy(ttsEnabled = false)
        } else {
            fb.ttsOpen()
            _state.value = currentState.copy(ttsEnabled = true)
        }
    }

    // ==================== Закладки ====================

    private fun addBookmark(bookmark: Storage.Bookmark) {
        val book = currentBook ?: return
        book.info.bookmarks.add(bookmark)
        storage.save(book)
        fbReaderView?.bookmarksUpdate()
        savePosition()
    }

    private fun deleteBookmark(bookmark: Storage.Bookmark) {
        val book = currentBook ?: return
        val index = book.info.bookmarks.indexOf(bookmark)
        if (index >= 0) {
            book.info.bookmarks.removeAt(index)
        }
        val fbIndex = fbReaderView?.book?.info?.bookmarks?.indexOf(bookmark) ?: -1
        if (fbIndex >= 0) {
            fbReaderView?.book?.info?.bookmarks?.removeAt(fbIndex)
        }
        fbReaderView?.bookmarksUpdate()
        storage.save(book)
    }

    // ==================== Шрифты ====================

    private fun setFontSize(size: Int) {
        shared.edit { putInt(BookApplication.PREFERENCE_FONTSIZE_FBREADER, size) }
        fbReaderView?.setFontsizeFB(size)
    }

    private fun setReflowFontSize(size: Float) {
        shared.edit { putFloat(BookApplication.PREFERENCE_FONTSIZE_REFLOW, size) }
        fbReaderView?.setFontsizeReflow(size)
    }

    private fun setFontFamily(family: String) {
        shared.edit { putString(BookApplication.PREFERENCE_FONTFAMILY_FBREADER, family) }
        fbReaderView?.setFontFB(family)
    }

    private fun setIgnoreEmbeddedFonts(ignore: Boolean) {
        shared.edit { putBoolean(BookApplication.PREFERENCE_IGNORE_EMBEDDED_FONTS, ignore) }
        fbReaderView?.setIgnoreCssFonts(ignore)
    }

    // ==================== Поиск ====================

    private fun search(query: String) {
        fbReaderView?.app?.runAction("search", query)
    }

    private fun searchNext() {
        fbReaderView?.app?.runAction("findNext")
    }

    private fun searchPrevious() {
        fbReaderView?.app?.runAction("findPrevious")
    }

    private fun searchClose() {
        fbReaderView?.searchClose()
    }

    // ==================== Вспомогательные методы ====================

    private fun getViewModeFromPrefs(): ViewMode {
        val mode = shared.getString(BookApplication.PREFERENCE_VIEW_MODE, "") ?: ""
        return if (mode == FBReaderView.Widgets.CONTINUOUS.toString()) {
            ViewMode.CONTINUOUS
        } else {
            ViewMode.PAGING
        }
    }

    /**
     * Получить FBook для передачи в FBReaderView
     */
    fun getFBook(): Storage.FBook? = currentFBook

    /**
     * Получить текущую книгу
     */
    fun getCurrentBook(): Storage.Book? = currentBook

    /**
     * Закрыть книгу при выходе
     */
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
