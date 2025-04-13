package su.sv.news.presentation.root.viewmodel

import su.sv.news.domain.GetNewsListUseCase
import su.sv.news.presentation.root.mapper.UiNewsMapper
import su.sv.news.presentation.root.model.UiNewsItem
import su.sv.news.presentation.utils.BasePagingSource

class NewsPagingSource(
    private val useCase: GetNewsListUseCase,
    private val uiMapper: UiNewsMapper,
) : BasePagingSource<UiNewsItem>() {

    override suspend fun loadPage(
        offset: Int,
        limit: Int
    ): Result<List<UiNewsItem>> {
        return useCase.execute(offset = offset).map { list ->
            list.map { newsItem ->
                uiMapper.fromDomainToUi(newsItem)
            }
        }
    }
}
