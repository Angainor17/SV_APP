package su.sv.news.presentation.root.mapper

import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import su.sv.commonui.managers.DateFormatter
import su.sv.news.domain.model.NewsItem
import su.sv.news.presentation.root.model.UiNewsItem
import javax.inject.Inject

class UiNewsMapper @Inject constructor(
    private val mediaMapper: UiNewsMediaMapper,
    private val dateFormatter: DateFormatter,
) {

    fun fromDomainToUi(domain: NewsItem): UiNewsItem {
        val date = domain.date

        return UiNewsItem(
            id = domain.id.orEmpty(),
            dateFormatted = formatDate(date),
            description = domain.description.orEmpty(),
            images = domain.images.map {
                mediaMapper.fromDomainToUi(it)
            },
            videos = domain.videos.map {
                mediaMapper.fromDomainToUi(it)
            },
            allMedia = domain.mediaItems.map {
                mediaMapper.fromDomainToUi(it)
            },
            vkPostUrl = buildVkPostUrl(domain.ownerId, domain.id),
        )
    }

    /**
     * Строит URL поста VK из owner_id и post_id.
     * Формат: https://vk.com/wall{owner_id}_{post_id}
     */
    private fun buildVkPostUrl(ownerId: Int?, postId: String?): String {
        val oid = ownerId?.toString() ?: return ""
        val pid = postId ?: return ""
        if (oid.isEmpty() || pid.isEmpty()) return ""
        return "https://vk.com/wall${oid}_${pid}"
    }

    private fun formatDate(date: LocalDateTime?): String {
        if (date == null) return ""

        return when {
            date.isToday() -> dateFormatter.formatShortTimeOnly(date)
            date.year == LocalDate.now().year -> dateFormatter.formatShortDateOnly(date)
            else -> dateFormatter.formatShortDateWithYear(date)
        }
    }

    private fun LocalDateTime?.isToday(): Boolean {
        if (this == null) return false
        return this.toLocalDate() == LocalDate.now()
    }
}
