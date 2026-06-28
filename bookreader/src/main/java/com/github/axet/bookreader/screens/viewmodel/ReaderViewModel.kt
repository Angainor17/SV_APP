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
import su.sv.managers.OnBookPagerManager
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel для экрана чтения книги
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val onBookPagerManager: OnBookPagerManager,
) : ViewModel() {

    private val _state = MutableStateFlow<ReaderState>(ReaderState.Loading)
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private var storage: Storage = Storage(context)
    private var currentBook: Storage.Book? = null
    private var currentFBook: Storage.FBook? = null

    // Ссылка на FBReaderView (управляется из Compose)
    var fbReaderView: FBReaderView? = null

    // Сохранённая позиция для восстановления при пересоздании FBReaderView
    private var savedPosition: FBReaderView.ZLTextIndexPosition? = null

    // Настройки
    private val shared: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // Флаг для управления клавишами громкости
    var volumeKeysEnabled: Boolean = true

    /**
     * Получить менеджер для обработки действий с книгой
     */
    fun getOnBookPagerManager(): OnBookPagerManager = onBookPagerManager

    /**
     * Обработка действий пользователя
     */
    fun onAction(action: ReaderActions) {
        when (action) {
            // Загрузка книги
            is ReaderActions.LoadBook -> loadBook(action.uri, action.position, action.bookCoverUrl, action.bookTitle, action.bookAuthor)

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
            ReaderActions.ToggleReflow -> toggleReflow()
            ReaderActions.MarkControlsHintShown -> markControlsHintShown()
            is ReaderActions.SetFullscreen -> setFullscreen(action.isFullscreen)

            // Диалоги
            ReaderActions.ToggleToc -> toggleToc()
            ReaderActions.ToggleBookmarks -> toggleBookmarks()
            ReaderActions.ToggleFontSettings -> toggleFontSettings()
            ReaderActions.HideDialogs -> hideDialogs()

            // Закладки
            is ReaderActions.EditBookmark -> editBookmark(action.bookmark)
            is ReaderActions.SaveBookmarkEdit -> saveBookmarkEdit(action.bookmark, action.name, action.color)
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

    private fun loadBook(uri: Uri, position: FBReaderView.ZLTextIndexPosition?, bookCoverUrl: String?, bookTitle: String?, bookAuthor: String?) {
        viewModelScope.launch {
            _state.value = ReaderState.Loading

            // Сохраняем переданную позицию для применения после загрузки книги
            if (position != null) {
                savedPosition = position
                Timber.d("Saved initial position from bookmark: $position")
            }

            try {
                // Проверяем доступность файла
                val inputStream = try {
                    context.contentResolver.openInputStream(uri)
                } catch (e: SecurityException) {
                    Timber.e(e, "Security exception accessing file: $uri")
                    _state.value = ReaderState.Error(
                        "Нет доступа к файлу. Файл был перемещён или удалён. Попробуйте скачать книгу заново."
                    )
                    return@launch
                } catch (e: Exception) {
                    Timber.e(e, "Error accessing file: $uri")
                    null
                }

                if (inputStream == null) {
                    _state.value = ReaderState.Error(
                        "Файл не найден или недоступен. Попробуйте скачать книгу заново."
                    )
                    return@launch
                }

                inputStream.close()

                // Загружаем информацию о книге
                currentBook = storage.load(uri)

                // Сохраняем URL обложки из API если передан
                if (bookCoverUrl != null) {
                    currentBook?.info?.coverUrl = bookCoverUrl
                }

                // Сохраняем название и автора из API если переданы (перезаписываем метаданные файла)
                if (bookTitle != null) {
                    currentBook?.info?.title = bookTitle
                }
                if (bookAuthor != null) {
                    currentBook?.info?.authors = bookAuthor
                }

                // Открываем файл книги
                currentFBook = storage.read(currentBook)

                // Создаём обложку если её нет
                ensureCoverCreated(currentBook, currentFBook)

                // Обновляем состояние
                val currentState = ReaderState.Content(
                    book = currentBook!!,
                    positionText = "",
                    isFullscreen = false,
                    viewMode = getViewModeFromPrefs(),
                )
                _state.value = currentState

                Timber.d("Book loaded: ${currentBook?.info?.title}, savedPosition=$savedPosition")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load book")
                val errorMessage = when {
                    e.message?.contains("EACCES") == true ->
                        "Нет доступа к файлу. Попробуйте скачать книгу заново."
                    e.message?.contains("ENOENT") == true ||
                    e.message?.contains("No such file") == true ->
                        "Файл не найден. Попробуйте скачать книгу заново."
                    else -> e.message ?: "Не удалось открыть книгу"
                }
                _state.value = ReaderState.Error(errorMessage)
            }
        }
    }

    /**
     * Создаёт обложку книги если её нет и сохраняет путь в book.info.coverUrl и bookFileUri
     */
    private fun ensureCoverCreated(book: Storage.Book?, fbook: Storage.FBook?) {
        if (book == null || fbook == null) return

        try {
            var needSave = false

            // Создаём файл обложки только если её нет
            if (book.info?.coverUrl == null) {
                val coverFile = Storage.coverFile(context, book)
                if (coverFile != null && !coverFile.exists()) {
                    storage.createCover(fbook, coverFile)
                }
                if (coverFile?.exists() == true) {
                    book.info?.coverUrl = coverFile.absolutePath
                    needSave = true
                    Timber.d("Cover created: ${coverFile.absolutePath}")
                }
            }

            // Всегда сохраняем bookFileUri если он не установлен
            if (book.info?.bookFileUri == null && book.url != null) {
                book.info?.bookFileUri = book.url.toString()
                needSave = true
                Timber.d("bookFileUri saved: ${book.url}")
            }

            // Сохраняем если что-то изменилось
            if (needSave) {
                storage.save(book)
                Timber.d("Book info saved with coverUrl=${book.info?.coverUrl}, bookFileUri=${book.info?.bookFileUri}")
            }
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
            // Сохраняем позицию для восстановления при пересоздании FBReaderView
            savedPosition = fb.position as? FBReaderView.ZLTextIndexPosition

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

    /**
     * Получить сохранённую позицию для восстановления при пересоздании FBReaderView
     */
    fun getSavedPosition(): FBReaderView.ZLTextIndexPosition? = savedPosition

    /**
     * Сбросить сохранённую позицию после применения
     */
    fun clearSavedPosition() {
        savedPosition = null
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

    private fun toggleReflow() {
        val currentState = _state.value as? ReaderState.Content ?: return
        fbReaderView?.setReflow(!fbReaderView!!.isReflow)
        _state.value = currentState.copy(isReflow = !currentState.isReflow)
    }

    /**
     * Отметить что подсказки зон касания были показаны
     */
    private fun markControlsHintShown() {
        val currentState = _state.value as? ReaderState.Content ?: return
        if (!currentState.hasShownControlsHint) {
            _state.value = currentState.copy(hasShownControlsHint = true)
        }
    }

    /**
     * Установить состояние fullscreen режима (вызывается из FBReaderView listener)
     */
    private fun setFullscreen(isFullscreen: Boolean) {
        val currentState = _state.value as? ReaderState.Content ?: return
        if (currentState.isFullscreen != isFullscreen) {
            _state.value = currentState.copy(isFullscreen = isFullscreen)
        }
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
            showFontSettings = false,
            showBookmarkEdit = false,
            editingBookmark = null
        )
    }

    // ==================== Закладки ====================

    /**
     * Синхронизировать закладки из FBook и обновить состояние
     */
    fun syncBookmarksFromFBook() {
        val book = currentBook ?: return
        val fbookBookmarks = fbReaderView?.book?.info?.bookmarks
        if (fbookBookmarks != null) {
            book.info.bookmarks = fbookBookmarks
        }
        // Триггерим обновление состояния для рекомпозиции диалога
        val currentState = _state.value as? ReaderState.Content ?: return
        _state.value = currentState.copy(book = book)
        storage.save(book)
    }

    /**
     * Открыть редактирование закладки
     */
    private fun editBookmark(bookmark: Storage.Bookmark) {
        val currentState = _state.value as? ReaderState.Content ?: return
        _state.value = currentState.copy(
            showBookmarkEdit = true,
            editingBookmark = bookmark,
            showToc = false,
            showBookmarks = false,
            showFontSettings = false
        )
    }

    /**
     * Сохранить изменения закладки
     */
    private fun saveBookmarkEdit(bookmark: Storage.Bookmark, name: String, color: Int) {
        // Обновляем данные в закладке
        bookmark.name = name.ifBlank { null }
        bookmark.color = color
        bookmark.last = System.currentTimeMillis()

        // Обновляем в FBook если есть
        val fbBookmark = fbReaderView?.book?.info?.bookmarks?.find { it.start.samePositionAs(bookmark.start) }
        if (fbBookmark != null) {
            fbBookmark.name = bookmark.name
            fbBookmark.color = bookmark.color
            fbBookmark.last = bookmark.last
        }

        // Сохраняем и обновляем отображение
        currentBook?.let { storage.save(it) }
        fbReaderView?.bookmarksUpdate()
        syncBookmarksFromFBook()

        // Закрываем диалог
        hideDialogs()
    }

    private fun addBookmark(bookmark: Storage.Bookmark) {
        val book = currentBook ?: return

        // Сохраняем coverUrl книги в закладке на момент создания
        bookmark.coverUrl = book.info?.coverUrl

        book.info.bookmarks.add(bookmark)
        storage.save(book)
        fbReaderView?.bookmarksUpdate()
        savePosition()
    }

    private fun deleteBookmark(bookmark: Storage.Bookmark) {
        val book = currentBook ?: return

        // Удаляем из Storage.Book по позиции и создаём новый список
        val index = book.info.bookmarks.indexOfFirst {
            it.start.samePositionAs(bookmark.start) && it.end.samePositionAs(bookmark.end)
        }
        if (index >= 0) {
            book.info.bookmarks.removeAt(index)
        }
        // Создаём новый объект списка для триггера рекомпозиции
        val newBookmarks = Storage.Bookmarks()
        newBookmarks.addAll(book.info.bookmarks)
        book.info.bookmarks = newBookmarks

        // Удаляем из FBook по позиции
        val fbBookmarks = fbReaderView?.book?.info?.bookmarks
        if (fbBookmarks != null) {
            val fbIndex = fbBookmarks.indexOfFirst {
                it.start.samePositionAs(bookmark.start) && it.end.samePositionAs(bookmark.end)
            }
            if (fbIndex >= 0) {
                fbBookmarks.removeAt(fbIndex)
            }
        }

        fbReaderView?.bookmarksUpdate()
        storage.save(book)

        // Обновляем состояние для рекомпозиции
        val currentState = _state.value as? ReaderState.Content ?: return
        _state.value = currentState.copy(book = book)
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
