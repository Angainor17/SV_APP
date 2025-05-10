package su.sv.books.catalog.presentation.root.viewmodel

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
import su.sv.books.catalog.domain.GetBooksListUseCase
import su.sv.books.catalog.presentation.CommonDownloadBookStates
import su.sv.books.catalog.presentation.base.BaseBookViewModel
import su.sv.books.catalog.presentation.root.mapper.UiBookMapper
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.books.catalog.presentation.root.viewmodel.effects.BooksListOneTimeEffect
import su.sv.commonui.managers.ResourcesRepository
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook
import javax.inject.Inject

/**
 * TODO: permission!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */
@HiltViewModel
class RootBooksCatalogViewModel @Inject constructor(
    private val getBooksListUseCase: GetBooksListUseCase,
    private val uiMapper: UiBookMapper,
    private val downloadBookStates: CommonDownloadBookStates,

    private val resourcesRepository: Lazy<ResourcesRepository>,
    private val downloadBookUseCase: Lazy<DownloadBookUseCase>,

    val bookDownloadedActionHandler: Lazy<BookDownloadedActionHandler>,

    getBookUriUseCase: Lazy<GetBookUriUseCase>,
) : BaseBookViewModel(
    downloadBookStates = downloadBookStates,
    getBookUriUseCase = getBookUriUseCase,
), RootBooksActions {

    /** Контент экрана */
    private val _state = MutableStateFlow<UiRootBooksState>(UiRootBooksState.Loading)
    val state: StateFlow<UiRootBooksState> get() = _state

    /** Одноразовые события */
    private val _oneTimeEffect = Channel<BooksListOneTimeEffect>(capacity = Channel.BUFFERED)
    val oneTimeEffect: Flow<BooksListOneTimeEffect> get() = _oneTimeEffect.receiveAsFlow()


    init {
        loadBooks()
    }

    private fun loadBooks() {
        _state.value = UiRootBooksState.Loading
        refreshList()
    }

    private fun refreshList() {
        viewModelScope.launch {
            getBooksListUseCase.execute().fold(
                onSuccess = { list ->
                    _state.value = if (list.isEmpty()) {
                        UiRootBooksState.EmptyState
                    } else {
                        UiRootBooksState.Content(
                            books = uiMapper.fromDomainToUi(list)
                        )
                    }
                },
                onFailure = {
                    _state.value = UiRootBooksState.Failure(it)
                },
            )
        }
    }

    override fun onAction(action: RootBookActions) {
        when (action) {
            RootBookActions.OnSwipeRefresh -> {
                updateState { contentState ->
                    contentState.copy(isRefreshing = true)
                }
                refreshList()
            }

            RootBookActions.OnRetryClick -> {
                loadBooks()
            }

            RootBookActions.UpdateStates -> {
                updateDownloadingStates()
            }

            is RootBookActions.OnBookStateHandle -> {
                handleDownloadedBook(action.bookState)
            }

            is RootBookActions.OnBookClick -> {
                _oneTimeEffect.trySend(BooksListOneTimeEffect.OpenBook(action.book))
            }

            is RootBookActions.OnToolbarBooksClick -> {
                _oneTimeEffect.trySend(BooksListOneTimeEffect.OpenStoredBooksList)
            }

            is RootBookActions.OnDownloadBookClick -> {
                loadBook(action.book)
            }
        }
    }

    private fun updateDownloadingStates() {
        updateState { state ->
            state.copy(
                books = state.books.map {
                    getBookWithActualDownloadState(it)
                }
            )
        }
    }

    private fun loadBook(book: UiBook) {
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
                    if (oldBook.id == book.id) {
                        getBookWithActualDownloadState(book)
                    } else {
                        oldBook
                    }
                }
            }
        }
    }

    private fun showBookLoadingState(book: UiBook) {
        updateBookState { oldBook ->
            if (oldBook.id == book.id) {
                book.copy(
                    downloadState = UIBookState.DOWNLOADING,
                )
            } else {
                oldBook
            }
        }
    }

    override fun updateBookState(action: (UiBook) -> UiBook) {
        updateState { state ->
            state.copy(
                books = state.books.map { action(it) },
            )
        }
    }

    override fun showErrorSnack(textResId: Int) {
        _oneTimeEffect.trySend(
            BooksListOneTimeEffect.ShowErrorSnackBar(
                text = resourcesRepository.get().getString(textResId)
            )
        )
    }

    private fun updateState(action: (UiRootBooksState.Content) -> UiRootBooksState.Content) {
        _state.update { state ->
            if (state is UiRootBooksState.Content) {
                action.invoke(state)
            } else {
                state
            }
        }
    }
}