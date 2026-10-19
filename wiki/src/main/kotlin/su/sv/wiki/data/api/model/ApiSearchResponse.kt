package su.sv.wiki.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Ответ на поиск статей
 * API: action=query&list=search
 */
data class ApiSearchResponse(
    @SerializedName("query")
    val query: ApiSearchQuery? = null,

    @SerializedName("batchcomplete")
    val batchComplete: String? = null,
)

data class ApiSearchQuery(
    @SerializedName("searchinfo")
    val searchInfo: ApiSearchInfo? = null,

    @SerializedName("search")
    val search: List<ApiSearchItem>? = null,
)

data class ApiSearchInfo(
    @SerializedName("totalhits")
    val totalHits: Int? = null,
)

data class ApiSearchItem(
    @SerializedName("ns")
    val namespace: Int? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("pageid")
    val pageId: Int? = null,

    @SerializedName("size")
    val size: Int? = null,

    @SerializedName("wordcount")
    val wordCount: Int? = null,

    @SerializedName("snippet")
    val snippet: String? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null,
)
