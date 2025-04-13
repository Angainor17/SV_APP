package su.sv.news.domain

import su.sv.news.data.NewsRepo
import su.sv.news.data.model.ApiNewsItem
import su.sv.news.domain.model.NewsItem
import javax.inject.Inject

class GetNewsListUseCase @Inject constructor(
    private val newsRepo: NewsRepo,
) {

    suspend fun execute(): Result<List<NewsItem>> {
        return newsRepo.getNews(
            count = NEWS_PAGE_SIZE,
        ).map { it.toDomain() }
    }

    private fun ApiNewsItem.toDomain() : NewsItem{

    }

    companion object {
        const val NEWS_PAGE_SIZE = 20
        const val NEWS_PREFETCH_DISTANCE = 5
    }
}
