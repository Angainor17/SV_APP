package su.sv.books.catalog.presentation.bookmarks.viewmodel

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import su.sv.books.R
import su.sv.books.catalog.domain.CalculateBookMd5UseCase
import su.sv.books.catalog.domain.CheckBookFileExistsUseCase
import su.sv.books.catalog.domain.DeleteNoteUseCase
import su.sv.books.catalog.domain.GetAllNotesUseCase
import su.sv.books.catalog.domain.GetBooksWithNotesUseCase
import su.sv.books.catalog.domain.GetNotesForBookUseCase
import su.sv.books.catalog.presentation.bookmarks.data.BookmarksViewModePrefsRepository
import su.sv.books.catalog.presentation.bookmarks.mapper.UiBookmarkMapper
import su.sv.books.catalog.presentation.bookmarks.model.DeleteNoteDialogState
import su.sv.books.catalog.presentation.bookmarks.model.NotesViewMode
import su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote
import su.sv.books.catalog.presentation.bookmarks.model.UiBookmarksState
import su.sv.commonarchitecture.managers.ResourcesRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val getAllNotesUseCase: GetAllNotesUseCase,
    private val getNotesForBookUseCase: GetNotesForBookUseCase,
    private val getBooksWithNotesUseCase: GetBooksWithNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val checkBookFileExistsUseCase: CheckBookFileExistsUseCase,
    private val calculateMd5UseCase: CalculateBookMd5UseCase,
    private val mapper: UiBookmarkMapper,
    private val resourcesRepository: ResourcesRepository,
    private val viewModePrefsRepository: BookmarksViewModePrefsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiBookmarksState>(UiBookmarksState.Loading)
    val state: StateFlow<UiBookmarksState> get() = _state

    private val _deleteDialogState = MutableStateFlow(DeleteNoteDialogState())
    val deleteDialogState: StateFlow<DeleteNoteDialogState> get() = _deleteDialogState

    private val _effect = Channel<BookmarksEffect>(capacity = Channel.BUFFERED)
    val effect: Flow<BookmarksEffect> get() = _effect.receiveAsFlow()

    /** Выбранная заметка для detail панели (master-detail на планшетах) */
    private val _selectedNote = MutableStateFlow<UiBookmarkNote?>(null)
    val selectedNote: StateFlow<UiBookmarkNote?> get() = _selectedNote

    /** Фильтр по книге */
    private var filterBookTitle: String? = null

    private var currentViewMode: NotesViewMode = NotesViewMode.LIST

    init {
        // Загружаем сохранённый режим просмотра
        currentViewMode = when (viewModePrefsRepository.getViewMode()) {
            BookmarksViewModePrefsRepository.MODE_BY_BOOK -> NotesViewMode.BY_BOOK
            else -> NotesViewMode.LIST
        }
        // Данные загружаем после установки фильтра (если есть) или сразу
        // Фильтр устанавливается через SetBookFilter action
    }

    fun onAction(action: BookmarksAction) {
        when (action) {
            BookmarksAction.OnBackClick -> {
                handleBackClick()
            }

            BookmarksAction.OnToggleViewMode -> {
                toggleViewMode()
            }

            BookmarksAction.OnRetryClick -> {
                loadNotes()
            }

            BookmarksAction.LoadData -> {
                Timber.tag("voronin2").d("LoadData action received")
                if (currentViewMode == NotesViewMode.BY_BOOK) {
                    loadBooks()
                } else {
                    loadNotes()
                }
            }

            is BookmarksAction.OnNoteClick -> {
                // Проверяем существование файла книги
                val bookFileExists = checkBookFileExistsUseCase.execute(action.note.bookFileUri)
                Timber.d("Note click: bookFileUri=${action.note.bookFileUri}, exists=$bookFileExists")

                if (bookFileExists) {
                    // Файл существует - открываем читалку
                    _effect.trySend(BookmarksEffect.OpenReader(action.note))
                } else {
                    // Файл не существует - открываем карточку книги
                    _effect.trySend(BookmarksEffect.OpenBookCard(action.note))
                }
            }

            is BookmarksAction.OnBookCardClick -> {
                _effect.trySend(BookmarksEffect.OpenBookCard(action.note))
            }

            is BookmarksAction.OnDeleteNoteRequest -> {
                showDeleteDialog(action.note)
            }

            BookmarksAction.OnDeleteNoteConfirm -> {
                confirmDeleteNote()
            }

            BookmarksAction.OnDeleteNoteCancel -> {
                hideDeleteDialog()
            }

            is BookmarksAction.OnBookClick -> {
                loadNotesForBook(action.bookId)
            }

            is BookmarksAction.OnShareNote -> {
                _effect.trySend(BookmarksEffect.ShareNote(action.note.getShareText()))
            }

            is BookmarksAction.OnNoteSelect -> {
                _selectedNote.value = action.note
            }

            BookmarksAction.OnNoteDeselect -> {
                _selectedNote.value = null
            }

            is BookmarksAction.SetBookFilter -> {
                setBookFilter(action.bookFileUri, action.bookTitle)
            }
        }
    }

    private fun setBookFilter(bookFileUri: String, bookTitle: String?) {
        filterBookTitle = bookTitle
        Timber.d("Set book filter: bookFileUri=$bookFileUri, bookTitle=$bookTitle")
        // Вычисляем MD5 и загружаем заметки
        loadNotesFilteredByUri(bookFileUri)
    }

    private fun loadNotesFilteredByUri(bookFileUri: String) {
        viewModelScope.launch {
            _state.value = UiBookmarksState.Loading

            // Вычисляем MD5 из URI
            val bookId = calculateMd5UseCase.execute(bookFileUri.toUri()).getOrNull()

            if (bookId == null) {
                Timber.w("Failed to calculate MD5 for bookFileUri: $bookFileUri")
                _state.value = UiBookmarksState.Empty
                return@launch
            }

            Timber.d("Calculated MD5: $bookId")

            getNotesForBookUseCase.execute(bookId).fold(
                onSuccess = { notes ->
                    val uiNotes = mapper.mapNotes(notes)
                    Timber.d("Loaded ${uiNotes.size} notes for book $bookId")
                    if (uiNotes.isEmpty()) {
                        _state.value = UiBookmarksState.Empty
                    } else {
                        _state.value = UiBookmarksState.NotesList(
                            notes = uiNotes,
                            viewMode = currentViewMode,
                            filterBookTitle = filterBookTitle
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load notes for book $bookId")
                    _state.value = UiBookmarksState.Error(
                        resourcesRepository.getString(R.string.books_error_loading)
                    )
                }
            )
        }
    }

    private fun loadNotes() {
        viewModelScope.launch {
            Timber.tag("voronin2").d("loadNotes: START")
            _state.value = UiBookmarksState.Loading

            getAllNotesUseCase.execute().fold(
                onSuccess = { notes ->
                    Timber.tag("voronin2").d("loadNotes: got ${notes.size} notes")
                    val uiNotes = mapper.mapNotes(notes)
                    Timber.tag("voronin2").d("loadNotes: mapped to ${uiNotes.size} uiNotes")
                    if (uiNotes.isEmpty()) {
                        Timber.tag("voronin2").d("loadNotes: state -> Empty")
                        _state.value = UiBookmarksState.Empty
                    } else {
                        Timber.tag("voronin2").d("loadNotes: state -> NotesList")
                        _state.value = UiBookmarksState.NotesList(
                            notes = uiNotes,
                            viewMode = currentViewMode
                        )
                    }
                },
                onFailure = { error ->
                    Timber.tag("voronin2").e(error, "loadNotes: FAILED")
                    _state.value = UiBookmarksState.Error(
                        resourcesRepository.getString(R.string.books_error_loading)
                    )
                }
            )
        }
    }

    private fun loadBooks() {
        viewModelScope.launch {
            Timber.tag("voronin2").d("loadBooks: START")
            _state.value = UiBookmarksState.Loading

            getBooksWithNotesUseCase.execute().fold(
                onSuccess = { books ->
                    Timber.tag("voronin2").d("loadBooks: got ${books.size} books")
                    val uiBooks = mapper.mapBooksWithNotes(books)
                    Timber.tag("voronin2").d("loadBooks: mapped to ${uiBooks.size} uiBooks")
                    if (uiBooks.isEmpty()) {
                        Timber.tag("voronin2").d("loadBooks: state -> Empty")
                        _state.value = UiBookmarksState.Empty
                    } else {
                        Timber.tag("voronin2").d("loadBooks: state -> BooksList")
                        _state.value = UiBookmarksState.BooksList(books = uiBooks)
                    }
                },
                onFailure = { error ->
                    Timber.tag("voronin2").e(error, "loadBooks: FAILED")
                    _state.value = UiBookmarksState.Error(
                        resourcesRepository.getString(R.string.books_error_loading)
                    )
                }
            )
        }
    }

    private fun loadNotesForBook(bookId: String) {
        viewModelScope.launch {
            Timber.tag("voronin2").d("loadNotesForBook: START, bookId=$bookId")
            _state.value = UiBookmarksState.Loading

            // Сначала получаем книгу
            val booksResult = getBooksWithNotesUseCase.execute()
            val book = booksResult.getOrNull()?.find { it.bookId == bookId }
            Timber.tag("voronin2").d("loadNotesForBook: book found = ${book != null}")

            getNotesForBookUseCase.execute(bookId).fold(
                onSuccess = { notes ->
                    Timber.tag("voronin2").d("loadNotesForBook: got ${notes.size} notes")
                    val uiNotes = mapper.mapNotes(notes)
                    if (book != null) {
                        Timber.tag("voronin2").d("loadNotesForBook: state -> BookNotes")
                        _state.value = UiBookmarksState.BookNotes(
                            book = mapper.mapBookWithNotes(book),
                            notes = uiNotes
                        )
                    } else {
                        Timber.tag("voronin2").d("loadNotesForBook: state -> NotesList")
                        _state.value = UiBookmarksState.NotesList(notes = uiNotes)
                    }
                },
                onFailure = { error ->
                    Timber.tag("voronin2").e(error, "loadNotesForBook: FAILED")
                    _state.value = UiBookmarksState.Error(
                        resourcesRepository.getString(R.string.books_error_loading)
                    )
                }
            )
        }
    }

    private fun toggleViewMode() {
        currentViewMode = when (currentViewMode) {
            NotesViewMode.LIST -> NotesViewMode.BY_BOOK
            NotesViewMode.BY_BOOK -> NotesViewMode.LIST
        }

        // Сохраняем режим в SharedPreferences
        val modeString = when (currentViewMode) {
            NotesViewMode.LIST -> BookmarksViewModePrefsRepository.MODE_LIST
            NotesViewMode.BY_BOOK -> BookmarksViewModePrefsRepository.MODE_BY_BOOK
        }
        viewModePrefsRepository.saveViewMode(modeString)

        when (currentViewMode) {
            NotesViewMode.LIST -> loadNotes()
            NotesViewMode.BY_BOOK -> loadBooks()
        }
    }

    private fun handleBackClick() {
        val currentState = _state.value
        if (currentState is UiBookmarksState.BookNotes) {
            // Если просматриваем заметки книги - возвращаемся к списку книг
            loadBooks()
        } else {
            // Иначе - закрываем экран
            _effect.trySend(BookmarksEffect.NavigateBack)
        }
    }

    private fun showDeleteDialog(note: UiBookmarkNote) {
        _deleteDialogState.value = DeleteNoteDialogState(
            note = note,
            isVisible = true
        )
    }

    private fun hideDeleteDialog() {
        _deleteDialogState.value = DeleteNoteDialogState()
    }

    private fun confirmDeleteNote() {
        val noteToDelete = _deleteDialogState.value.note ?: return
        hideDeleteDialog()

        viewModelScope.launch {
            deleteNoteUseCase.execute(noteToDelete.id).fold(
                onSuccess = {
                    // Удаляем заметку из списка
                    _state.update { state ->
                        when (state) {
                            is UiBookmarksState.NotesList -> {
                                val updatedNotes = state.notes.filter { it.id != noteToDelete.id }
                                if (updatedNotes.isEmpty()) {
                                    UiBookmarksState.Empty
                                } else {
                                    state.copy(notes = updatedNotes)
                                }
                            }

                            is UiBookmarksState.BookNotes -> {
                                val updatedNotes = state.notes.filter { it.id != noteToDelete.id }
                                if (updatedNotes.isEmpty()) {
                                    // Если заметок больше нет, возвращаемся к списку книг
                                    UiBookmarksState.Empty
                                } else {
                                    state.copy(notes = updatedNotes)
                                }
                            }

                            else -> state
                        }
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to delete note")
                    _effect.trySend(
                        BookmarksEffect.ShowError(
                            resourcesRepository.getString(R.string.books_error_loading)
                        )
                    )
                }
            )
        }
    }
}

/**
 * Действия на экране заметок
 */
sealed class BookmarksAction {
    object OnBackClick : BookmarksAction()
    object OnToggleViewMode : BookmarksAction()
    object OnRetryClick : BookmarksAction()
    object LoadData : BookmarksAction()
    data class OnNoteClick(val note: UiBookmarkNote) : BookmarksAction()
    data class OnBookCardClick(val note: UiBookmarkNote) : BookmarksAction()
    data class OnDeleteNoteRequest(val note: UiBookmarkNote) : BookmarksAction()
    object OnDeleteNoteConfirm : BookmarksAction()
    object OnDeleteNoteCancel : BookmarksAction()
    data class OnBookClick(val bookId: String) : BookmarksAction()
    data class OnShareNote(val note: UiBookmarkNote) : BookmarksAction()

    /** Выбрать заметку для detail панели (master-detail на планшетах) */
    data class OnNoteSelect(val note: UiBookmarkNote) : BookmarksAction()

    /** Сбросить выбор заметки (master-detail на планшетах) */
    object OnNoteDeselect : BookmarksAction()

    /** Установить фильтр по книге */
    data class SetBookFilter(val bookFileUri: String, val bookTitle: String?) : BookmarksAction()
}

/**
 * Одноразовые эффекты
 */
sealed class BookmarksEffect {
    object NavigateBack : BookmarksEffect()
    data class OpenReader(val note: UiBookmarkNote) : BookmarksEffect()
    data class OpenBookCard(val note: UiBookmarkNote) : BookmarksEffect()
    data class ShareNote(val text: String) : BookmarksEffect()
    data class ShowError(val message: String) : BookmarksEffect()
}
