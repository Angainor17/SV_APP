package su.sv.api.data.response

import com.google.gson.annotations.SerializedName

class VkResponse<T>(
    @SerializedName("response") val response: T,
)
