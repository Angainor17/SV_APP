package su.sv.news.presentation.root.mapper

import su.sv.news.domain.model.NewsItem
import su.sv.news.presentation.root.model.UiNewsItem
import javax.inject.Inject

class UiNewsMapper @Inject constructor() {

    fun fromDomainToUi(domain: NewsItem): UiNewsItem {
        return UiNewsItem(
            id = domain.id.orEmpty(),
            title = domain.title.orEmpty(),
            description = domain.description.orEmpty(),
            image = domain.image.orEmpty(),
        )
    }
}