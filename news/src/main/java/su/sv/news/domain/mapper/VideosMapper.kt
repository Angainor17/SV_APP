package su.sv.news.domain.mapper

import su.sv.api.data.response.VkAttachmentPhoto
import su.sv.api.data.response.VkAttachmentVideo
import su.sv.api.data.response.VkResponseNewsAttachment
import su.sv.news.domain.model.NewsMediaItem

fun fromApiToDomain(api: VkResponseNewsAttachment): NewsMediaItem? {
    return when (api.type) {
        "photo" -> fromApiToDomain(api.photo ?: return null)
        "video" -> fromApiToDomain(api.video ?: return null)
        else -> null
    }
}

fun fromApiToDomain(api: VkAttachmentPhoto): NewsMediaItem.ImageItem {
    return NewsMediaItem.ImageItem(
        image = api.getImageUrl(),
    )
}

fun fromApiToDomain(api: VkAttachmentVideo): NewsMediaItem.VideoItem {
    val id = api.id.toString()
    val ownerId = api.ownerId.orEmpty()

    return NewsMediaItem.VideoItem(
        id = id,
        image = api.image?.lastOrNull()?.url.orEmpty(), // TODO find needed
        link = "https://vk.com/video${ownerId}_$id",
    )
}

fun VkResponseNewsAttachment.getImageUrl(): String? = photo?.getImageUrl()  // FIXME: select size

fun VkAttachmentPhoto.getImageUrl(): String = origPhoto?.url.orEmpty()  // FIXME: select size
