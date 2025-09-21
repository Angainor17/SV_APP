package su.sv.api.data.response

import com.google.gson.annotations.SerializedName

class VkAttachmentVideo(

    // "id": 457239017,
    @SerializedName("id") val id: Long?,
    @SerializedName("date") val date: Long?,
    @SerializedName("title") val title: String?,
    @SerializedName("image") val image: VkPhotoSize?,
)
