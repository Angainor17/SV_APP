package su.sv.api.data.response

import com.google.gson.annotations.SerializedName

class VkAttachmentPhoto(

    // "id": 457239017,
    @SerializedName("id") val id: Long?,
    @SerializedName("sizes") val sizes: List<VkPhotoSize>?,
    @SerializedName("orig_photo") val origPhoto: VkPhotoSize?,
)
