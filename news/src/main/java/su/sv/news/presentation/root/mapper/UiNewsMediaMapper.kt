package su.sv.news.presentation.root.mapper

import su.sv.news.domain.model.NewsMediaItem
import su.sv.news.presentation.root.model.UiNewsMedia
import javax.inject.Inject

class UiNewsMediaMapper @Inject constructor() {

    fun fromDomainToUi(domain: NewsMediaItem): UiNewsMedia {
        return when (domain) {
            is NewsMediaItem.ImageItem -> fromDomainToUi(domain)
            is NewsMediaItem.VideoItem -> fromDomainToUi(domain)
        }
    }

    fun fromDomainToUi(domain: NewsMediaItem.ImageItem): UiNewsMedia.ItemImage {
        return UiNewsMedia.ItemImage(
            image = domain.image,
        )
    }

    fun fromDomainToUi(domain: NewsMediaItem.VideoItem): UiNewsMedia.ItemVideo {
        return UiNewsMedia.ItemVideo(
            id = domain.id,
            image = domain.image,
            link = domain.link,
        )
    }
}
