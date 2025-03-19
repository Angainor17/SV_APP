package su.sv.books.catalog.presentation.root.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import su.sv.books.catalog.domain.GetBooksListUseCase
import su.sv.books.catalog.presentation.root.mapper.UiBookMapper
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class RootBooksCatalogViewModel @Inject constructor(
    private val getBooksListUseCase: GetBooksListUseCase,
    private val uiMapper: UiBookMapper,
) : BaseViewModel(), RootBooksActions {

    private val _state = MutableStateFlow<UiRootBooksState>(UiRootBooksState.Loading)
    val state: StateFlow<UiRootBooksState> get() = _state

    init {
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

    override fun onRetryClick() {
        loadBooks()
    }
}