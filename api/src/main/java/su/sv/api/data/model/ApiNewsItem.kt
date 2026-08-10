package su.sv.api.data.model

import com.google.gson.annotations.SerializedName
import su.sv.api.data.response.VkResponseNewsAttachment

/**
 * Модель новости в списке полученная от ВК
 */
class ApiNewsItem(

    /** Идентификатор поста */
    @SerializedName("id") val id: Int?,

    /** Идентификатор владельца стены */
    @SerializedName("owner_id") val ownerId: Int?,

    @SerializedName("date") val dateSeconds: Long?,

    @SerializedName("text") val text: String?,

    @SerializedName("attachments") val attachments: List<VkResponseNewsAttachment>?,

    @SerializedName("copy_history") val copyHistory: List<ApiNewsItem>?,
)
