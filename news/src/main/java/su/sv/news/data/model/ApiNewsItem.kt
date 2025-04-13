package su.sv.news.data.model

import com.google.gson.annotations.SerializedName
import su.sv.news.data.response.VkResponseNewsAttachment

/**
 * Модель новости в списке полученная от ВК
 */
class ApiNewsItem(

    /** Идентификатор для хранения */
    @SerializedName("id") val id: Int?,

    @SerializedName("date") val dateMillis: Long?,

    @SerializedName("text") val text: String?,

    @SerializedName("attachments") val attachments: List<VkResponseNewsAttachment>?,
)
