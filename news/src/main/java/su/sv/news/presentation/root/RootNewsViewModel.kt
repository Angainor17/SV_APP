package su.sv.news.presentation.root

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.news.domain.GetNewsListUseCase
import su.sv.news.presentation.root.mapper.UiNewsMapper
import su.sv.news.presentation.root.model.UiNewsItem
import su.sv.news.presentation.root.model.UiRootNewsState
import su.sv.news.presentation.root.viewmodel.NewsPagingSource
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActions
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActionsHandler
import su.sv.news.presentation.root.viewmodel.effects.NewsListOneTimeEffect
import javax.inject.Inject

@HiltViewModel
class RootNewsViewModel @Inject constructor(
    private val getNewsListUseCase: GetNewsListUseCase,
    private val uiMapper: UiNewsMapper,
) : BaseViewModel(), RootNewsActionsHandler {

    /** Контент экрана */
    private val _state = MutableStateFlow<UiRootNewsState>(UiRootNewsState.Loading)
    val state: StateFlow<UiRootNewsState> get() = _state

    /** Одноразовые события */
    private val _oneTimeEffect = Channel<NewsListOneTimeEffect>(capacity = Channel.BUFFERED)
    val oneTimeEffect: Flow<NewsListOneTimeEffect> get() = _oneTimeEffect.receiveAsFlow()

    val pagingConfig = PagingConfig(
        pageSize = GetNewsListUseCase.NEWS_PAGE_SIZE,
        prefetchDistance = GetNewsListUseCase.NEWS_PREFETCH_DISTANCE,  // Pre-fetch the next page when 5 items away from the end
        initialLoadSize = GetNewsListUseCase.NEWS_PAGE_SIZE   // Initial load size
    )

    val pagingDataFlow: Flow<PagingData<UiNewsItem>> = Pager(pagingConfig) {
        NewsPagingSource(
            useCase = getNewsListUseCase,
            uiMapper = uiMapper,
        )
    }.flow.cachedIn(viewModelScope)

    override fun onAction(action: RootNewsActions) {
        when (action) {
            RootNewsActions.OnSwipeRefresh -> {
                updateState { contentState ->
                    contentState.copy(isRefreshing = true)
                }
                refreshList()
            }

            RootNewsActions.OnRetryClick -> {
                loadBooks()
            }

            is RootNewsActions.OnNewsClick -> {
                _oneTimeEffect.trySend(
                    NewsListOneTimeEffect.OpenNewsItem(action.item)
                )
            }
        }
    }

    private fun loadBooks() {
        _state.value = UiRootNewsState.Loading
        refreshList()
    }

    private fun refreshList() {
        viewModelScope.launch {
            getNewsListUseCase.execute().fold(
                onSuccess = { list ->
                    _state.value = if (list.isEmpty()) {
                        UiRootNewsState.EmptyState
                    } else {
                        UiRootNewsState.Content(
                            newsPagingData = pagingDataFlow,
//                            news = uiMapper.fromDomainToUi(list),
                        )
                    }
                },
                onFailure = {
                    _state.value = UiRootNewsState.Failure(it)
                },
            )
        }
    }

    private fun updateState(action: (UiRootNewsState.Content) -> UiRootNewsState.Content) {
        _state.update { state ->
            if (state is UiRootNewsState.Content) {
                action.invoke(state)
            } else {
                state
            }
        }
    }
}
