package su.sv.api.data.response

import com.google.gson.annotations.SerializedName

class VkPhotoSize(

    @SerializedName("height") val height: Int?,
    @SerializedName("width") val width: Int?,
    @SerializedName("url") val url: String?,

    /**
     * "height": 75,
     * "type": "s",
     * "width": 56,
     *
     * "height": 130,
     * "type": "m",
     * "width": 98,
     *
     * "height": 604,
     * "type": "x",
     * "width": 453,
     *
     * "height": 807,
     * "type": "y",
     * "width": 606,
     *
     * "height": 1066,
     * "type": "z",
     * "width": 800,
     *
     * "height": 173,
     * "type": "o",
     * "width": 130,
     *
     * "height": 266,
     * "type": "p",
     * "width": 200,
     *
     * "height": 426,
     * "type": "q",
     * "width": 320,
     *
     * "height": 680,
     * "type": "r",
     * "width": 510,
     */
    @SerializedName("type") val type: String?,
)
