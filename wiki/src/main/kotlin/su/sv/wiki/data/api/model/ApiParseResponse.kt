package su.sv.wiki.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Ответ на парсинг страницы
 * API: action=parse&page=TITLE&prop=text|links|displaytitle
 */
data class ApiParseResponse(
    @SerializedName("parse")
    val parse: ApiParseData? = null,

    @SerializedName("error")
    val error: ApiError? = null,
)

data class ApiParseData(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("pageid")
    val pageId: Int? = null,

    @SerializedName("text")
    val text: ApiTextContent? = null,

    @SerializedName("links")
    val links: List<ApiLink>? = null,

    @SerializedName("displaytitle")
    val displayTitle: String? = null,
)

data class ApiTextContent(
    @SerializedName("*")
    val content: String? = null,
)

data class ApiLink(
    @SerializedName("ns")
    val namespace: Int? = null,

    @SerializedName("exists")
    val exists: String? = null,

    @SerializedName("*")
    val title: String? = null,
)

/**
 * Ответ с ошибкой API
 */
data class ApiError(
    @SerializedName("code")
    val code: String? = null,

    @SerializedName("info")
    val info: String? = null,
)
