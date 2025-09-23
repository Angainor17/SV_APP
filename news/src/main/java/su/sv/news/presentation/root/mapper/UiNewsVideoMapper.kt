package su.sv.news.presentation.root.mapper

import su.sv.news.domain.model.NewsVideoItem
import su.sv.news.presentation.root.model.UiItemVideo
import javax.inject.Inject

class UiNewsVideoMapper @Inject constructor() {

    fun fromDomainToUi(domain: NewsVideoItem): UiItemVideo {
        return UiItemVideo(
            id = domain.id,
            image = domain.image,
            link = domain.link,
        )
    }
}
