package su.sv.news.domain

import org.threeten.bp.LocalDateTime
import su.sv.api.data.NewsRepo
import su.sv.api.data.model.ApiNewsItem
import su.sv.commonui.ext.toLocalDateTime
import su.sv.news.domain.mapper.fromApiToDomain
import su.sv.news.domain.model.NewsItem
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
        if (copyHistory.orEmpty().isNotEmpty()) {
            return copyHistory.orEmpty().first().toDomain().copy(
                date = getDate(),
            )
        }

        val attachments = attachments.orEmpty()
        return NewsItem(
            id = (id ?: 0).toString(),
            date = getDate(),
            description = text.orEmpty(),
            images = attachments
                .filter { it.type == "photo" }
                .mapNotNull {
                    it.photo?.let { api -> fromApiToDomain(api) }
                },
            videos = attachments
                .filter { it.type == "video" }
                .mapNotNull {
                    it.video?.let { api -> fromApiToDomain(api) }
                },
            mediaItems = attachments
                .mapNotNull { fromApiToDomain(it) }
        )
    }

    private fun ApiNewsItem.getDate(): LocalDateTime {
        return ((dateSeconds ?: 0) * 1_000).toLocalDateTime()
    }

    companion object {
        const val NEWS_PAGE_SIZE = 20
        const val NEWS_PREFETCH_DISTANCE = 5
    }
}
