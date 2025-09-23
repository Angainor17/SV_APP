package su.sv.news.domain.mapper

import su.sv.api.data.response.VkAttachmentVideo
import su.sv.news.domain.model.NewsVideoItem

fun fromApiToDomain(api: VkAttachmentVideo): NewsVideoItem {
    val id = api.id.toString()
    val ownerId = api.ownerId.orEmpty()

    return NewsVideoItem(
        id = id,
        image = api.image?.lastOrNull()?.url.orEmpty(), // TODO find needed
        link = "https://vk.com/video${ownerId}_$id",
    )
}