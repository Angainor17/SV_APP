package su.sv.api.data.response

import com.google.gson.annotations.SerializedName
import su.sv.api.data.model.ApiNewsItem

class VkNewsResponse(
    @SerializedName("count") val count: Int?,
    @SerializedName("items") val items: List<ApiNewsItem>?,
)
