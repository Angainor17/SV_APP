package su.sv.news.domain

import su.sv.commonui.ext.toLocalDateTime
import su.sv.news.data.NewsRepo
import su.sv.news.data.model.ApiNewsItem
import su.sv.news.domain.model.NewsItem
import java.time.LocalDateTime
import javax.inject.Inject

class GetNewsListUseCase @Inject constructor(
    private val newsRepo: NewsRepo,
) {

    suspend fun execute(offset: Int): Result<List<NewsItem>> {
        return newsRepo.getNews(
            count = NEWS_PAGE_SIZE,
            offset = offset,
        ).map { list ->
            list.map { it.toDomain() }
        }
    }

    private fun ApiNewsItem.toDomain(): NewsItem {
        return NewsItem(
            id = (id ?: 0).toString(),
            date = dateMillis?.toLocalDateTime() ?: LocalDateTime.now(),
            description = text.orEmpty(),
            images = attachments.orEmpty()
                .filter { it.type == "photo" }
                .mapNotNull { it.photo?.origPhoto?.url }, // FIXME: select size
        )
    }

    companion object {
        const val NEWS_PAGE_SIZE = 20
        const val NEWS_PREFETCH_DISTANCE = 5
    }
}
