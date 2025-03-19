package su.sv.books.catalog.presentation.root

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import su.sv.books.catalog.domain.GetBooksListUseCase
import su.sv.books.catalog.presentation.root.mapper.UiBookMapper
import su.sv.books.catalog.presentation.root.model.UiRootNewsState
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class RootBooksCatalogViewModel @Inject constructor(
    private val getBooksListUseCase: GetBooksListUseCase,
    private val uiMapper: UiBookMapper,
) : BaseViewModel() {

    private val _state = MutableStateFlow<UiRootNewsState>(UiRootNewsState.Loading)
    val state: Flow<UiRootNewsState> get() = _state

    init {
        loadBooks()
    }

    fun loadBooks() {
        _state.value = UiRootNewsState.Loading

        viewModelScope.launch {
            getBooksListUseCase.execute().fold(
                onSuccess = { list ->
                    _state.value = if (list.isEmpty()) {
                        UiRootNewsState.EmptyState
                    } else {
                        UiRootNewsState.Success(
                            books = uiMapper.fromDomainToUi(list)
                        )
                    }
                },
                onFailure = {
                    _state.value = UiRootNewsState.Failure(it)
                },
            )
        }
    }
}
