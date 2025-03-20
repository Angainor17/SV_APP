package su.sv.books.catalog.presentation.root.viewmodel

import android.net.Uri
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
import su.sv.books.catalog.domain.DownloadBookUseCase
import su.sv.books.catalog.domain.GetBooksListUseCase
import su.sv.books.catalog.presentation.root.mapper.UiBookMapper
import su.sv.books.catalog.presentation.root.model.UiBook
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.books.catalog.presentation.root.viewmodel.effects.BooksListOneTimeEffect
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RootBooksCatalogViewModel @Inject constructor(
    private val getBooksListUseCase: GetBooksListUseCase,
    private val uiMapper: UiBookMapper,

    private val downloadBookUseCase: Lazy<DownloadBookUseCase>,
) : BaseViewModel(), RootBooksActions {

    /** Контент экрана */
    private val _state = MutableStateFlow<UiRootBooksState>(UiRootBooksState.Loading)
    val state: StateFlow<UiRootBooksState> get() = _state

    /** Одноразовые события */
    private val _oneTimeEffect = Channel<BooksListOneTimeEffect>(capacity = Channel.BUFFERED)
    val oneTimeEffect: Flow<BooksListOneTimeEffect> get() = _oneTimeEffect.receiveAsFlow()

    init {
        Timber.tag("voronin").d("RootBooksCatalogViewModel init $this")
        loadBooks()
    }

    fun loadBooks() {
        _state.value = UiRootBooksState.Loading

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
            RootBookActions.OnRetryClick -> {
                loadBooks()
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

    private fun loadBook(book: UiBook) {
        Timber.tag("voronin").d("VM loadBook = ${book.fileNameWithExt}")

        viewModelScope.launch {
            updateBookLoadingState(book, isLoading = true)

            downloadBookUseCase.get().execute(
                DownloadBookUseCase.Params(
                    url = book.downloadUrl,
                    bookTitle = book.downloadUrl,
                    fileNameWithExt = book.fileNameWithExt,
                )
            )
                .shareIn(viewModelScope, SharingStarted.Lazily)
                .collect { uri: Uri? ->
                    Timber.tag("voronin").d("uri = $uri")
                    updateBookAfterDownload(uri, book)
                }
        }
    }

    private fun updateBookAfterDownload(uri: Uri?, book: UiBook) {
        if (uri == null) {
            showErrorSnack(R.string.books_download_snack_error)
            updateBookLoadingState(book, isLoading = false)
        } else {
            updateBookState { state ->
                state.copy(
                    isDownloaded = true,
                    isDownloading = false,
                    fileUri = uri,
                )
            }
        }
    }

    private fun updateBookLoadingState(book: UiBook, isLoading: Boolean) {
        updateBookState { oldBook ->
            if (oldBook.id == book.id) {
                book.copy(
                    isDownloading = isLoading,
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