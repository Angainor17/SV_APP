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
import su.sv.books.catalog.domain.GetBookFiltersUseCase
import su.sv.books.catalog.domain.GetBookUriUseCase
import su.sv.books.catalog.domain.GetBooksListUseCase
import su.sv.books.catalog.domain.model.BookFilter
import su.sv.books.catalog.presentation.CommonDownloadBookStates
import su.sv.books.catalog.presentation.base.BaseBookViewModel
import su.sv.books.catalog.presentation.root.mapper.UiBookFilterMapper
import su.sv.books.catalog.presentation.root.mapper.UiBookMapper
import su.sv.books.catalog.presentation.root.model.UiBookFilter
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.books.catalog.presentation.root.viewmodel.effects.BooksListOneTimeEffect
import su.sv.commonui.managers.ResourcesRepository
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook
import javax.inject.Inject

@HiltViewModel
class RootBooksCatalogViewModel @Inject constructor(
    private val getBooksListUseCase: GetBooksListUseCase,
    private val getBookFiltersUseCase: GetBookFiltersUseCase,
    private val uiMapper: UiBookMapper,
    private val uiFilterMapper: UiBookFilterMapper,
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

    private fun refreshList(preserveFilters: Boolean = false) {
        viewModelScope.launch {
            // Сохраняем текущее состояние фильтров если нужно
            val currentState = _state.value as? UiRootBooksState.Content
            val preservedFilters = if (preserveFilters && currentState != null) {
                currentState.filters to currentState.selectedFilters
            } else {
                null
            }

            getBooksListUseCase.execute().fold(
                onSuccess = { list ->
                    val uiBooks = uiMapper.fromDomainToUi(list)

                    if (preservedFilters != null) {
                        // Сохраняем фильтры и выбранные, обновляем только книги
                        val (currentFilters, selectedFilters) = preservedFilters
                        _state.value = UiRootBooksState.Content.create(
                            books = uiBooks,
                            filters = currentFilters,
                            selectedFilters = selectedFilters,
                            hasDownloadedBooks = uiBooks.any { it.fileUri != null },
                            filterScrollResetKey = currentState?.filterScrollResetKey ?: 0,
                        )
                    } else {
                        // Создаём новые фильтры
                        val filters = getBookFiltersUseCase.execute(list)
                        val selectedFilters = setOf(BookFilter.All)
                        val uiFilters = uiFilterMapper.mapToUi(filters, selectedFilters)

                        _state.value = if (list.isEmpty()) {
                            UiRootBooksState.EmptyState
                        } else {
                            UiRootBooksState.Content.create(
                                books = uiBooks,
                                filters = uiFilters,
                                selectedFilters = selectedFilters,
                                hasDownloadedBooks = uiBooks.any { it.fileUri != null },
                            )
                        }
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
                refreshList(preserveFilters = true)
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

            is RootBookActions.OnOpenDownloadedBook -> {
                openDownloadedBook(action.book)
            }

            is RootBookActions.OnFilterSelect -> {
                handleFilterSelect(action.filter)
            }

            is RootBookActions.OnFilterRemove -> {
                handleFilterRemove(action.filter)
            }
        }
    }

    private fun handleFilterSelect(filter: BookFilter) {
        updateState { state ->
            val newSelectedFilters = when (filter) {
                is BookFilter.All -> setOf(BookFilter.All)
                else -> {
                    // При выборе другого фильтра убираем "Все" из выбранных
                    val currentFilters = state.selectedFilters.filter { it !is BookFilter.All }.toSet()
                    if (state.selectedFilters.contains(filter)) {
                        currentFilters - filter
                    } else {
                        currentFilters + filter
                    }
                }
            }

            // Если сбросили все фильтры, возвращаем "Все"
            val finalSelectedFilters = if (newSelectedFilters.isEmpty()) {
                setOf(BookFilter.All)
            } else {
                newSelectedFilters
            }

            val updatedUiFilters = updateFiltersAvailability(state.filters, finalSelectedFilters, state.books)

            // Отправляем эффект для скролла к началу
            _oneTimeEffect.trySend(BooksListOneTimeEffect.ScrollToTop)

            UiRootBooksState.Content.create(
                books = state.books,
                filters = updatedUiFilters,
                selectedFilters = finalSelectedFilters,
                hasDownloadedBooks = state.hasDownloadedBooks,
                filterScrollResetKey = state.filterScrollResetKey + 1,
            )
        }
    }

    private fun handleFilterRemove(filter: BookFilter) {
        updateState { state ->
            val newSelectedFilters = state.selectedFilters - filter

            // Если сбросили все фильтры, возвращаем "Все"
            val finalSelectedFilters = if (newSelectedFilters.isEmpty() || newSelectedFilters.all { it is BookFilter.All }) {
                setOf(BookFilter.All)
            } else {
                newSelectedFilters.filter { it !is BookFilter.All }.toSet()
            }

            val updatedUiFilters = updateFiltersAvailability(state.filters, finalSelectedFilters, state.books)

            // Отправляем эффект для скролла к началу
            _oneTimeEffect.trySend(BooksListOneTimeEffect.ScrollToTop)

            UiRootBooksState.Content.create(
                books = state.books,
                filters = updatedUiFilters,
                selectedFilters = finalSelectedFilters,
                hasDownloadedBooks = state.hasDownloadedBooks,
                filterScrollResetKey = state.filterScrollResetKey + 1,
            )
        }
    }

    private fun updateFiltersAvailability(
        filters: List<UiBookFilter>,
        selectedFilters: Set<BookFilter>,
        books: List<UiBook>
    ): List<UiBookFilter> {
        // Если нет выбранных фильтров или выбрано "Все" - все фильтры доступны
        if (selectedFilters.isEmpty() || selectedFilters.contains(BookFilter.All)) {
            return filters.map { it.copy(isAvailable = true, isSelected = selectedFilters.contains(it.filter)) }
        }

        // Фильтруем книги по выбранным фильтрам
        val filteredBooks = books.filter { book ->
            selectedFilters.all { filter ->
                when (filter) {
                    is BookFilter.All -> true
                    is BookFilter.Category -> book.category == filter.name
                    is BookFilter.Author -> book.author.contains(filter.name)
                    is BookFilter.Series -> book.title.contains(filter.name)
                }
            }
        }

        // Определяем доступность фильтров
        return filters.map { uiFilter ->
            val isSelected = selectedFilters.contains(uiFilter.filter)
            val isAvailable = when (uiFilter.filter) {
                is BookFilter.All -> true
                is BookFilter.Category -> filteredBooks.any { it.category == uiFilter.filter.name } || isSelected
                is BookFilter.Author -> filteredBooks.any { it.author.contains(uiFilter.filter.name) } || isSelected
                is BookFilter.Series -> filteredBooks.any { it.title.contains(uiFilter.filter.name) } || isSelected
            }
            uiFilter.copy(isSelected = isSelected, isAvailable = isAvailable)
        }
    }

    private fun updateDownloadingStates() {
        updateState { state ->
            val updatedBooks = state.books.map {
                getBookWithActualDownloadState(it)
            }
            UiRootBooksState.Content.create(
                books = updatedBooks,
                filters = state.filters,
                selectedFilters = state.selectedFilters,
                hasDownloadedBooks = updatedBooks.any { it.fileUri != null },
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

    private fun openDownloadedBook(book: UiBook) {
        if (book.fileUri != null) {
            _oneTimeEffect.trySend(BooksListOneTimeEffect.OpenReader(book))
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
            val updatedBooks = state.books.map { action(it) }
            UiRootBooksState.Content.create(
                books = updatedBooks,
                filters = state.filters,
                selectedFilters = state.selectedFilters,
                hasDownloadedBooks = updatedBooks.any { it.fileUri != null },
            )
        }
    }

    override fun showErrorSnack(resId: Int) {
        _oneTimeEffect.trySend(
            BooksListOneTimeEffect.ShowErrorSnackBar(
                text = resourcesRepository.get().getString(resId)
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
