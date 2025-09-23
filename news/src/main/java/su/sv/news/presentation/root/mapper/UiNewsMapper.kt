package su.sv.news.presentation.root.mapper

import android.annotation.SuppressLint
import su.sv.commonui.managers.DateFormatter
import su.sv.news.domain.model.NewsItem
import su.sv.news.presentation.root.model.UiNewsItem
import javax.inject.Inject

class UiNewsMapper @Inject constructor(
    private val videoMapper: UiNewsVideoMapper,
    private val dateFormatter: DateFormatter,
) {

    @SuppressLint("NewApi")
    fun fromDomainToUi(domain: NewsItem): UiNewsItem {
        return UiNewsItem(
            id = domain.id.orEmpty(),
            dateFormatted = domain.date?.toLocalDate()?.let {
                dateFormatter.formatShortDateOnly(it)
            }.orEmpty(),
            description = domain.description.orEmpty(),
            images = domain.images.filter { it.isNotEmpty() },
            videos = domain.videos.map {
                videoMapper.fromDomainToUi(it)
            },
        )
    }
}
