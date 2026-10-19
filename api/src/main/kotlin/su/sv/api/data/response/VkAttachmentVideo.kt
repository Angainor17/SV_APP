package su.sv.api.data.response

import com.google.gson.annotations.SerializedName

class VkAttachmentVideo(

    @SerializedName("id") val id: Long?,
    @SerializedName("text") val text: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("image") val image: List<VkPhotoSize>?,
    @SerializedName("access_key") val accessKey: String?,
    @SerializedName("owner_id") val ownerId: String?,
)
