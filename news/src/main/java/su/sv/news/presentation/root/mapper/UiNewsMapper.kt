package su.sv.news.presentation.root.mapper

import android.annotation.SuppressLint
import su.sv.commonui.managers.DateFormatter
import su.sv.news.domain.model.NewsItem
import su.sv.news.presentation.root.model.UiNewsItem
import java.time.LocalDateTime
import javax.inject.Inject


class UiNewsMapper @Inject constructor(
    private val mediaMapper: UiNewsMediaMapper,
    private val dateFormatter: DateFormatter,
) {

    @SuppressLint("NewApi")
    fun fromDomainToUi(domain: NewsItem): UiNewsItem {
        val date = domain.date ?: LocalDateTime.now()

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

    @SuppressLint("NewApi")
    private fun LocalDateTime.isToday(): Boolean {
        return LocalDateTime.now().toLocalDate() == toLocalDate()
    }
}
