package su.sv.news.data.response

import com.google.gson.annotations.SerializedName

class VkResponseNewsAttachment(

    // "type": "photo",
    @SerializedName("type") val type: String?,

    @SerializedName("photo") val photo: VkAttachmentPhoto?,
)
