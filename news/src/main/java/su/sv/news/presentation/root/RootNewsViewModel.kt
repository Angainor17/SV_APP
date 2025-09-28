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
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.news.domain.GetNewsListUseCase
import su.sv.news.presentation.root.mapper.UiNewsMapper
import su.sv.news.presentation.root.model.UiNewsItem
import su.sv.news.presentation.root.model.UiNewsMedia
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

    /** Контент экрана (вне списка) */
    private val _state = MutableStateFlow(UiRootNewsState())
    val state: StateFlow<UiRootNewsState> get() = _state

    /** Одноразовые события */
    private val _oneTimeEffect = Channel<NewsListOneTimeEffect>(capacity = Channel.BUFFERED)
    val oneTimeEffect: Flow<NewsListOneTimeEffect> get() = _oneTimeEffect.receiveAsFlow()

    private val pagingConfig = PagingConfig(
        pageSize = GetNewsListUseCase.NEWS_PAGE_SIZE,
        prefetchDistance = GetNewsListUseCase.NEWS_PREFETCH_DISTANCE,  // Pre-fetch the next page when 5 items away from the end
        initialLoadSize = GetNewsListUseCase.NEWS_PAGE_SIZE,   // Initial load size
    )

    val pagingDataFlow: Flow<PagingData<UiNewsItem>> = Pager(pagingConfig) {
        NewsPagingSource(
            useCase = getNewsListUseCase,
            uiMapper = uiMapper,
        )
    }.flow.cachedIn(viewModelScope)

    override fun onAction(action: RootNewsActions) {
        when (action) {
            RootNewsActions.OnSwipeRefreshFinished -> {
                updateState { contentState ->
                    contentState.copy(isRefreshing = false)
                }
            }

            RootNewsActions.OnSwipeRefresh -> {
                updateState { contentState ->
                    contentState.copy(isRefreshing = true)
                }
            }

            is RootNewsActions.OnNewsClick -> {
                _oneTimeEffect.trySend(
                    NewsListOneTimeEffect.OpenNewsItem(action.item)
                )
            }

            is RootNewsActions.OnNewsMediaClick -> {
                val item = action.item
                when (item) {
                    is UiNewsMedia.ItemVideo -> {
                        _oneTimeEffect.trySend(
                            NewsListOneTimeEffect.OpenNewsVideo(item)
                        )
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun updateState(action: (UiRootNewsState) -> UiRootNewsState) {
        _state.update { state ->
            action.invoke(state)
        }
    }
}
