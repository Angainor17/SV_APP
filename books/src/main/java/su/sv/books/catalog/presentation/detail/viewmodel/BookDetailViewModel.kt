package su.sv.books.catalog.presentation.detail.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import su.sv.books.R
import su.sv.books.catalog.data.receivers.BookDownloadedActionHandler
import su.sv.books.catalog.domain.DownloadBookUseCase
import su.sv.books.catalog.domain.GetBookUriUseCase
import su.sv.books.catalog.presentation.CommonDownloadBookStates
import su.sv.books.catalog.presentation.base.BaseBookViewModel
import su.sv.books.catalog.presentation.detail.actions.DetailBookActions
import su.sv.books.catalog.presentation.detail.actions.DetailBooksActionsHandler
import su.sv.books.catalog.presentation.detail.effects.BookDetailOneTimeEffect
import su.sv.books.catalog.presentation.detail.mapper.UiDetailBookMapper
import su.sv.books.catalog.presentation.detail.model.UiBookDetailState
import su.sv.commonui.managers.ResourcesRepository
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook
import javax.inject.Inject

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val uiMapper: UiDetailBookMapper,
    private val downloadBookStates: CommonDownloadBookStates,

    private val resourcesRepository: Lazy<ResourcesRepository>,
    private val downloadBookUseCase: Lazy<DownloadBookUseCase>,

    val bookDownloadedActionHandler: Lazy<BookDownloadedActionHandler>,

    getBookUriUseCase: Lazy<GetBookUriUseCase>,
) : BaseBookViewModel(
    downloadBookStates = downloadBookStates,
    getBookUriUseCase = getBookUriUseCase,
), DetailBooksActionsHandler {

    /** Контент экрана */
    private val _state = MutableStateFlow<UiBookDetailState>(UiBookDetailState.NoContent)
    val state: StateFlow<UiBookDetailState> get() = _state

    /** Одноразовые события */
    private val _oneTimeEffect = Channel<BookDetailOneTimeEffect>(capacity = Channel.BUFFERED)
    val oneTimeEffect: Flow<BookDetailOneTimeEffect> get() = _oneTimeEffect.receiveAsFlow()

    override fun onAction(action: DetailBookActions) {
        when (action) {
            is DetailBookActions.LoadState -> {
                _state.tryEmit(uiMapper.createState(action.book))
            }

            is DetailBookActions.OnActionClick -> {
                handleActionClick(action)
            }

            is DetailBookActions.OnBookStateHandle -> {
                handleDownloadedBook(action.bookState)
            }
        }
    }

    private fun handleActionClick(action: DetailBookActions.OnActionClick) {
        val book = action.book
        val hasDownloadedBook = book.fileUri != null

        if (hasDownloadedBook) {
            _oneTimeEffect.trySend(BookDetailOneTimeEffect.OpenBook(book))
        } else {
            downloadBook(book)
        }
    }

    private fun downloadBook(book: UiBook) {
        viewModelScope.launch {
            showBookLoadingState(book)

            try {
                val downloadId = downloadBookUseCase.get().execute(
                    DownloadBookUseCase.Params(
                        url = book.downloadUrl,
                        bookTitle = book.title,
                        fileNameWithExt = book.fileNameWithExt,
                    )
                )
                downloadBookStates.loadingInProgressMap[downloadId] = book.id
            } catch (_: Exception) {
                showErrorSnack(R.string.books_download_snack_error)
            } finally {
                updateBookState { oldBook ->
                    getBookWithActualDownloadState(oldBook)
                }
            }
        }
    }

    // конец загрузки
    override fun handleBookDownloadEnd() {
        updateState { state ->
            uiMapper.createStateAfterDownload(state)
        }
    }

    private fun showBookLoadingState(book: UiBook) {
        updateState { state ->
            state.copy(
                isActionLoading = true,
                book = book.copy(
                    downloadState = UIBookState.DOWNLOADING,
                )
            )
        }
    }

    override fun updateBookState(action: (UiBook) -> UiBook) {
        updateState { state ->
            state.copy(book = action(state.book))
        }
    }

    override fun showErrorSnack(textResId: Int) {
        _oneTimeEffect.trySend(
            BookDetailOneTimeEffect.ShowErrorSnackBar(
                text = resourcesRepository.get().getString(textResId)
            )
        )
    }

    private fun updateState(action: (UiBookDetailState.Content) -> UiBookDetailState.Content) {
        _state.update { state ->
            if (state is UiBookDetailState.Content) action.invoke(state) else state
        }
    }
}
