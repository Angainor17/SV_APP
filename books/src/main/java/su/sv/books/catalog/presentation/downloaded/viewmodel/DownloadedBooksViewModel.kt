package su.sv.books.catalog.presentation.downloaded.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import su.sv.books.catalog.domain.DeleteBookUseCase
import su.sv.books.catalog.domain.GetDownloadedBooksUseCase
import su.sv.books.catalog.presentation.downloaded.actions.DownloadedBookActions
import su.sv.books.catalog.presentation.downloaded.effects.DownloadedBookEffect
import su.sv.books.catalog.presentation.downloaded.mapper.UiDownloadedBookMapper
import su.sv.books.catalog.presentation.downloaded.model.DeleteDialogState
import su.sv.books.catalog.presentation.downloaded.model.UiDownloadedBooksState
import su.sv.commonarchitecture.managers.ResourcesRepository
import javax.inject.Inject

@HiltViewModel
class DownloadedBooksViewModel @Inject constructor(
    private val getDownloadedBooksUseCase: GetDownloadedBooksUseCase,
    private val deleteBookUseCase: DeleteBookUseCase,
    private val uiMapper: UiDownloadedBookMapper,
    private val resourcesRepository: ResourcesRepository,
    private val sharedPreferences: SharedPreferences,
) : ViewModel() {

    companion object {
        private const val KEY_SWIPE_HINT_SHOWN = "downloaded_books_swipe_hint_shown"
    }

    private val _state = MutableStateFlow<UiDownloadedBooksState>(UiDownloadedBooksState.Loading)
    val state: StateFlow<UiDownloadedBooksState> get() = _state

    private val _deleteDialogState = MutableStateFlow(DeleteDialogState())
    val deleteDialogState: StateFlow<DeleteDialogState> get() = _deleteDialogState

    private val _effect = Channel<DownloadedBookEffect>(capacity = Channel.BUFFERED)
    val effect: Flow<DownloadedBookEffect> get() = _effect.receiveAsFlow()

    init {
        loadBooks()
    }

    fun onAction(action: DownloadedBookActions) {
        when (action) {
            is DownloadedBookActions.OnBackClick -> {
                _effect.trySend(DownloadedBookEffect.NavigateBack)
            }

            is DownloadedBookActions.OnReadClick -> {
                _effect.trySend(DownloadedBookEffect.OpenBookDetail(action.book))
            }

            is DownloadedBookActions.OnDeleteRequest -> {
                showDeleteDialog(action.book)
            }

            DownloadedBookActions.OnDeleteConfirm -> {
                confirmDelete()
            }

            DownloadedBookActions.OnDeleteCancel -> {
                hideDeleteDialog(resetSwipe = true)
            }

            DownloadedBookActions.OnSwipeHintShown -> {
                onSwipeHintShown()
            }
        }
    }

    private fun loadBooks() {
        // Сразу показываем Loading - UI уже готов
        _state.value = UiDownloadedBooksState.Loading

        viewModelScope.launch {
            // Тяжёлые операции на IO dispatcher
            val result = withContext(Dispatchers.IO) {
                getDownloadedBooksUseCase.execute()
            }

            result.fold(
                onSuccess = { books ->
                    val uiBooks = uiMapper.mapToUi(books)
                    _state.value = if (uiBooks.isEmpty()) {
                        UiDownloadedBooksState.Empty
                    } else {
                        val showSwipeHint = !sharedPreferences.getBoolean(KEY_SWIPE_HINT_SHOWN, false)
                        UiDownloadedBooksState.Content(
                            books = uiBooks,
                            showSwipeHint = showSwipeHint,
                        )
                    }
                },
                onFailure = {
                    _effect.trySend(DownloadedBookEffect.ShowError(
                        resourcesRepository.getString(su.sv.books.R.string.books_download_snack_error)
                    ))
                    _state.value = UiDownloadedBooksState.Empty
                }
            )
        }
    }

    private fun showDeleteDialog(book: su.sv.books.catalog.presentation.downloaded.model.UiDownloadedBook) {
        // Отмечаем, что подсказка была показана
        sharedPreferences.edit { putBoolean(KEY_SWIPE_HINT_SHOWN, true) }

        _deleteDialogState.value = DeleteDialogState(
            book = book,
            isVisible = true,
        )

        // Скрываем подсказку
        _state.update { state ->
            if (state is UiDownloadedBooksState.Content) {
                state.copy(showSwipeHint = false)
            } else {
                state
            }
        }
    }

    private fun hideDeleteDialog(resetSwipe: Boolean = false) {
        _deleteDialogState.value = DeleteDialogState()

        // Сбрасываем состояние свайпа при отмене
        if (resetSwipe) {
            _state.update { state ->
                if (state is UiDownloadedBooksState.Content) {
                    state.copy(resetKey = state.resetKey + 1)
                } else {
                    state
                }
            }
        }
    }

    private fun confirmDelete() {
        val bookToDelete = _deleteDialogState.value.book ?: return
        hideDeleteDialog()

        viewModelScope.launch {
            deleteBookUseCase.execute(bookToDelete.fileUri).fold(
                onSuccess = {
                    // Удаляем книгу из списка
                    _state.update { state ->
                        if (state is UiDownloadedBooksState.Content) {
                            val updatedBooks = state.books.filter { it.id != bookToDelete.id }
                            if (updatedBooks.isEmpty()) {
                                UiDownloadedBooksState.Empty
                            } else {
                                state.copy(books = updatedBooks)
                            }
                        } else {
                            state
                        }
                    }
                },
                onFailure = {
                    _effect.trySend(DownloadedBookEffect.ShowError(
                        resourcesRepository.getString(su.sv.books.R.string.books_download_snack_error)
                    ))
                }
            )
        }
    }

    private fun onSwipeHintShown() {
        sharedPreferences.edit { putBoolean(KEY_SWIPE_HINT_SHOWN, true) }
        _state.update { state ->
            if (state is UiDownloadedBooksState.Content) {
                state.copy(showSwipeHint = false)
            } else {
                state
            }
        }
    }
}
