package su.sv.news.presentation.root.model

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * Все состояния экрана со списка новостей
 */
sealed class UiRootNewsState {

    data class Content(
        val newsPagingData: Flow<PagingData<UiNewsItem>>,
        val isRefreshing: Boolean = false,
    ) : UiRootNewsState()

    object EmptyState : UiRootNewsState()

    object Loading : UiRootNewsState()

    class Failure(throwable: Throwable) : UiRootNewsState()
}
