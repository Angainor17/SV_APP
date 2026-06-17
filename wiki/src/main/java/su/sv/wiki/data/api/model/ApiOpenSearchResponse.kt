package su.sv.wiki.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Ответ OpenSearch (автодополнение)
 * API: action=opensearch&search=QUERY&limit=N
 *
 * Формат ответа - массив из 4 элементов:
 * [0] - поисковый запрос
 * [1] - массив заголовков
 * [2] - массив описаний (обычно пустые)
 * [3] - массив URL
 */
data class ApiOpenSearchResponse(
    @SerializedName("")
    val searchQuery: String? = null,

    val titles: List<String>? = null,

    val descriptions: List<String>? = null,

    val urls: List<String>? = null,
)

/**
 * Маппинг из JSON-массива в типизированный объект
 */
class OpenSearchResponseParser {
    fun parse(jsonArray: List<Any?>): ApiOpenSearchResponse {
        return ApiOpenSearchResponse(
            searchQuery = jsonArray.getOrNull(0) as? String,
            titles = jsonArray.getOrNull(1) as? List<String>,
            descriptions = jsonArray.getOrNull(2) as? List<String>,
            urls = jsonArray.getOrNull(3) as? List<String>,
        )
    }
}
