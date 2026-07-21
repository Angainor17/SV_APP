package su.sv.wiki.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import su.sv.wiki.data.api.model.ApiParseResponse
import su.sv.wiki.data.api.model.ApiSearchResponse

/**
 * API интерфейс для работы с MediaWiki
 * Базовый URL: https://svremya.su/
 */
interface WikiApi {

    /**
     * Поиск статей
     * @param query поисковый запрос
     * @param what тип поиска: "nearmatch" - точное совпадение, "text" - по тексту, "title" - по заголовкам
     * @param limit максимальное количество результатов
     */
    @GET("api.php")
    suspend fun search(
        @Query("action") action: String = "query",
        @Query("list") list: String = "search",
        @Query("srsearch") query: String,
        @Query("srwhat") what: String = "nearmatch",
        @Query("srlimit") limit: Int = 10,
        @Query("format") format: String = "json",
        @Query("utf8") utf8: String = "",
    ): Response<ApiSearchResponse>

    /**
     * Получение содержимого страницы
     * @param title заголовок страницы
     * @param disableEditSection отключить ссылки "править" в секциях (default: true)
     * @param disableTOC отключить оглавление (default: false)
     */
    @GET("api.php")
    suspend fun getPage(
        @Query("action") action: String = "parse",
        @Query("page") title: String,
        @Query("prop") prop: String = "text|links|displaytitle",
        @Query("disableeditsection") disableEditSection: Int = 1,
        @Query("disabletoc") disableTOC: Int = 0,
        @Query("format") format: String = "json",
    ): Response<ApiParseResponse>

    /**
     * Автодополнение поиска (OpenSearch)
     * Возвращает JSON-массив: [query, titles[], descriptions[], urls[]]
     * @param query поисковый запрос
     * @param limit максимальное количество результатов (по умолчанию 5)
     */
    @GET("api.php")
    suspend fun openSearch(
        @Query("action") action: String = "opensearch",
        @Query("search") query: String,
        @Query("limit") limit: Int = 5,
        @Query("format") format: String = "json",
        @Query("utf8") utf8: String = "",
    ): Response<List<Any?>>

    companion object {
        const val BASE_URL = "https://svremya.su/"
    }
}
