package su.sv.books.catalog.presentation.bookmarks.viewmodel

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
import su.sv.books.catalog.domain.DeleteNoteUseCase
import su.sv.books.catalog.domain.GetAllNotesUseCase
import su.sv.books.catalog.domain.GetBooksWithNotesUseCase
import su.sv.books.catalog.domain.GetNotesForBookUseCase
import su.sv.books.catalog.presentation.bookmarks.data.BookmarksViewModePrefsRepository
import su.sv.books.catalog.presentation.bookmarks.mapper.UiBookmarkMapper
import su.sv.books.catalog.presentation.bookmarks.model.DeleteNoteDialogState
import su.sv.books.catalog.presentation.bookmarks.model.NotesViewMode
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

    private var currentViewMode: NotesViewMode = NotesViewMode.LIST

    init {
        // Загружаем сохранённый режим просмотра
        currentViewMode = when (viewModePrefsRepository.getViewMode()) {
            BookmarksViewModePrefsRepository.MODE_BY_BOOK -> NotesViewMode.BY_BOOK
            else -> NotesViewMode.LIST
        }
        // Загружаем данные в соответствии с режимом
        when (currentViewMode) {
            NotesViewMode.LIST -> loadNotes()
            NotesViewMode.BY_BOOK -> loadBooks()
        }
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

            is BookmarksAction.OnNoteClick -> {
                _effect.trySend(BookmarksEffect.OpenReader(action.note))
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
        }
    }

    private fun loadNotes() {
        viewModelScope.launch {
            _state.value = UiBookmarksState.Loading

            getAllNotesUseCase.execute().fold(
                onSuccess = { notes ->
                    val uiNotes = mapper.mapNotes(notes)
                    if (uiNotes.isEmpty()) {
                        _state.value = UiBookmarksState.Empty
                    } else {
                        _state.value = UiBookmarksState.NotesList(
                            notes = uiNotes,
                            viewMode = currentViewMode
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load notes")
                    _state.value = UiBookmarksState.Error(
                        resourcesRepository.getString(su.sv.books.R.string.books_error_loading)
                    )
                }
            )
        }
    }

    private fun loadBooks() {
        viewModelScope.launch {
            _state.value = UiBookmarksState.Loading

            getBooksWithNotesUseCase.execute().fold(
                onSuccess = { books ->
                    val uiBooks = mapper.mapBooksWithNotes(books)
                    if (uiBooks.isEmpty()) {
                        _state.value = UiBookmarksState.Empty
                    } else {
                        _state.value = UiBookmarksState.BooksList(books = uiBooks)
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load books with notes")
                    _state.value = UiBookmarksState.Error(
                        resourcesRepository.getString(su.sv.books.R.string.books_error_loading)
                    )
                }
            )
        }
    }

    private fun loadNotesForBook(bookId: String) {
        viewModelScope.launch {
            _state.value = UiBookmarksState.Loading

            // Сначала получаем книгу
            val booksResult = getBooksWithNotesUseCase.execute()
            val book = booksResult.getOrNull()?.find { it.bookId == bookId }

            getNotesForBookUseCase.execute(bookId).fold(
                onSuccess = { notes ->
                    val uiNotes = mapper.mapNotes(notes)
                    if (book != null) {
                        _state.value = UiBookmarksState.BookNotes(
                            book = mapper.mapBookWithNotes(book),
                            notes = uiNotes
                        )
                    } else {
                        _state.value = UiBookmarksState.NotesList(notes = uiNotes)
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load notes for book: $bookId")
                    _state.value = UiBookmarksState.Error(
                        resourcesRepository.getString(su.sv.books.R.string.books_error_loading)
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

    private fun showDeleteDialog(note: su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote) {
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
                    _effect.trySend(BookmarksEffect.ShowError(
                        resourcesRepository.getString(su.sv.books.R.string.books_error_loading)
                    ))
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
    data class OnNoteClick(val note: su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote) : BookmarksAction()
    data class OnBookCardClick(val note: su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote) : BookmarksAction()
    data class OnDeleteNoteRequest(val note: su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote) : BookmarksAction()
    object OnDeleteNoteConfirm : BookmarksAction()
    object OnDeleteNoteCancel : BookmarksAction()
    data class OnBookClick(val bookId: String) : BookmarksAction()
    data class OnShareNote(val note: su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote) : BookmarksAction()
}

/**
 * Одноразовые эффекты
 */
sealed class BookmarksEffect {
    object NavigateBack : BookmarksEffect()
    data class OpenReader(val note: su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote) : BookmarksEffect()
    data class OpenBookCard(val note: su.sv.books.catalog.presentation.bookmarks.model.UiBookmarkNote) : BookmarksEffect()
    data class ShareNote(val text: String) : BookmarksEffect()
    data class ShowError(val message: String) : BookmarksEffect()
}
