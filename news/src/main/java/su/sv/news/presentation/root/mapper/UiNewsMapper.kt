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
            dateFormatted = if (date.isToday()) {
                dateFormatter.formatShortTimeOnly(date)
            } else {
                dateFormatter.formatShortDateOnly(date)
            },
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
        )
    }

    private fun LocalDateTime?.isToday(): Boolean {
        if (this == null) return false
        return this.toLocalDate() == LocalDate.now()
    }
}
