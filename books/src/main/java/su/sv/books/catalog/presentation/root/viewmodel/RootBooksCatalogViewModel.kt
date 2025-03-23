package su.sv.books.catalog.presentation.root.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import su.sv.books.R
import su.sv.books.catalog.data.receivers.BookDownloadedActionHandler
import su.sv.books.catalog.domain.DownloadBookUseCase
import su.sv.books.catalog.domain.GetBookUriUseCase
import su.sv.books.catalog.domain.GetBooksListUseCase
import su.sv.books.catalog.presentation.root.mapper.UiBookMapper
import su.sv.books.catalog.presentation.root.model.UIBookState
import su.sv.books.catalog.presentation.root.model.UiBook
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.books.catalog.presentation.root.viewmodel.effects.BooksListOneTimeEffect
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import timber.log.Timber
import javax.inject.Inject

/**
 * TODO при разворачивании экрана надо чекать статусы
 * TODO: permission!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * TODO: swipeRefresh
 */
@HiltViewModel
class RootBooksCatalogViewModel @Inject constructor(
    private val getBooksListUseCase: GetBooksListUseCase,
    private val uiMapper: UiBookMapper,

    private val downloadBookUseCase: Lazy<DownloadBookUseCase>,
    private val getBookUriUseCase: Lazy<GetBookUriUseCase>,
    private val bookDownloadedActionHandler: Lazy<BookDownloadedActionHandler>,
) : BaseViewModel(), RootBooksActions {

    /** Контент экрана */
    private val _state = MutableStateFlow<UiRootBooksState>(UiRootBooksState.Loading)
    val state: StateFlow<UiRootBooksState> get() = _state

    /** Одноразовые события */
    private val _oneTimeEffect = Channel<BooksListOneTimeEffect>(capacity = Channel.BUFFERED)
    val oneTimeEffect: Flow<BooksListOneTimeEffect> get() = _oneTimeEffect.receiveAsFlow()

    /** Список downloadId, которые в прогрессе скачивания. Значение - Book.id */
    private val loadingInProgressMap = hashMapOf<Long, String>()

    init {
        loadBooks()
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        viewModelScope.launch {
            bookDownloadedActionHandler.get().sharedStateFlow.shareIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(),
            ).collect {
                handleDownloadedBook(it)
            }
        }
    }

    private fun handleDownloadedBook(state: BookDownloadedActionHandler.BookState) {
        val (downloadId) = state
        val bookId = loadingInProgressMap[downloadId]

        var currentBook: UiBook? = null // FIXME
        // обновляется на статус скачанного (или нет)
        updateBookState { oldBook ->
            if (oldBook.id == bookId) {
                currentBook = getBookWithActualDownloadState(oldBook)
                currentBook
            } else {
                oldBook
            }
        }

        //TODO : refactor
        val uri = getBookUriUseCase.get().execute(currentBook?.fileNameWithExt.orEmpty())
        val isSuccess = currentBook != null && uri != null
        if (!isSuccess) {
            showErrorSnack(R.string.books_download_snack_error)
        }

        loadingInProgressMap.remove(downloadId)
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
                Timber.tag("voronin").d("RootBookActions.UpdateStates")
                updateDownloadingStates()
            }
            is RootBookActions.OnBookClick -> {
                // TODO: открытие карточки книги
            }
            is RootBookActions.OnDownloadBookClick -> {
                // TODO: permission!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
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

    private fun getBookWithActualDownloadState(book: UiBook): UiBook {
        val uri = getBookUriUseCase.get().execute(book.fileNameWithExt)

        return book.copy(
            downloadState = when {
                uri != null -> {
                    UIBookState.DOWNLOADED
                }
                // скачивание ещё идёт
                loadingInProgressMap.values.contains(book.id) -> UIBookState.DOWNLOADING
                else -> UIBookState.AVAILABLE_TO_DOWNLOAD
            },
            fileUri = uri,
        )
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
                loadingInProgressMap[downloadId] = book.id
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

    private fun updateBookState(action: (UiBook) -> UiBook) {
        updateState { state ->
            state.copy(
                books = state.books.map { action(it) },
            )
        }
    }

    private fun showErrorSnack(textResId: Int) {
        _oneTimeEffect.trySend(BooksListOneTimeEffect.ShowErrorSnackBar(textResId))
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