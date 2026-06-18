package su.sv.wiki.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.sv.wiki.data.api.WikiApi
import su.sv.wiki.data.api.model.ApiLink
import su.sv.wiki.data.local.dao.FavoriteDao
import su.sv.wiki.data.local.dao.HistoryDao
import su.sv.wiki.data.local.entity.FavoriteEntity
import su.sv.wiki.data.local.entity.HistoryEntity
import su.sv.wiki.domain.model.WikiArticle
import su.sv.wiki.domain.model.WikiExternalLink
import su.sv.wiki.domain.model.WikiLink
import su.sv.wiki.domain.model.WikiSearchResult
import su.sv.wiki.domain.model.WikiSearchSuggestion
import su.sv.wiki.domain.repository.WikiRepository
import su.sv.wiki.domain.repository.WikiResult
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория Wiki
 */
@Singleton
class WikiRepositoryImpl @Inject constructor(
    private val api: WikiApi,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val gson: Gson,
) : WikiRepository {

    // ========== Удалённые операции ==========

    override suspend fun searchArticle(query: String): WikiResult<WikiSearchResult> {
        return try {
            val response = api.search(query = query)

            when {
                !response.isSuccessful -> {
                    WikiResult.Error(
                        message = "Ошибка сети: ${response.code()}",
                        code = response.code().toString(),
                    )
                }

                response.body()?.query?.search.isNullOrEmpty() -> {
                    WikiResult.NotFound
                }

                else -> {
                    val searchItem = response.body()!!.query!!.search!!.first()
                    WikiResult.Success(
                        WikiSearchResult(
                            title = searchItem.title.orEmpty(),
                            pageId = searchItem.pageId ?: 0,
                            snippet = searchItem.snippet.orEmpty(),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error searching article: $query")
            WikiResult.Error(
                message = e.message ?: "Неизвестная ошибка",
                code = "NETWORK_ERROR",
            )
        }
    }

    override suspend fun getArticle(title: String): WikiResult<WikiArticle> {
        return try {
            val response = api.getPage(title = title)

            when {
                !response.isSuccessful -> {
                    WikiResult.Error(
                        message = "Ошибка сети: ${response.code()}",
                        code = response.code().toString(),
                    )
                }

                response.body()?.error != null -> {
                    val error = response.body()!!.error!!
                    if (error.code == "missingtitle") {
                        WikiResult.NotFound
                    } else {
                        WikiResult.Error(
                            message = error.info.orEmpty(),
                            code = error.code,
                        )
                    }
                }

                response.body()?.parse == null -> {
                    WikiResult.Error(
                        message = "Пустой ответ от сервера",
                        code = "EMPTY_RESPONSE",
                    )
                }

                else -> {
                    val parseData = response.body()!!.parse!!
                    val htmlContent = parseData.text?.content.orEmpty()
                    val title = parseData.title.orEmpty()
                    WikiResult.Success(
                        WikiArticle(
                            title = title,
                            pageId = parseData.pageId ?: 0,
                            content = htmlContent,
                            links = parseData.links?.map { it.toDomain() }.orEmpty(),
                            externalLinks = parseExternalLinks(htmlContent),
                            articleUrl = "https://svremya.su/${title.replace(" ", "_")}",
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting article: $title")
            WikiResult.Error(
                message = e.message ?: "Неизвестная ошибка",
                code = "NETWORK_ERROR",
            )
        }
    }

    override suspend fun getSearchSuggestions(query: String, limit: Int): List<WikiSearchSuggestion> {
        return try {
            if (query.length < 2) return emptyList()

            val response = api.openSearch(query = query, limit = limit)

            if (!response.isSuccessful || response.body() == null) {
                return emptyList()
            }

            val body = response.body()!!
            val titles = body.getOrNull(1) as? List<*> ?: return emptyList()

            titles.mapNotNull { title ->
                (title as? String)?.let { WikiSearchSuggestion(title = it) }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting search suggestions: $query")
            emptyList()
        }
    }

    // ========== Локальные операции (избранное) ==========

    override fun getFavorites(): Flow<List<WikiArticle>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toDomain(gson) }
        }
    }

    override fun getFavoriteTitles(): Flow<List<String>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.title }
        }
    }

    override fun hasFavorites(): Flow<Boolean> {
        return favoriteDao.getFavoritesCount().map { count -> count > 0 }
    }

    override suspend fun isFavorite(title: String): Boolean {
        return favoriteDao.isFavorite(title)
    }

    override suspend fun addToFavorites(article: WikiArticle) {
        val entity = article.toEntity(gson)
        favoriteDao.insertFavorite(entity)
    }

    override suspend fun removeFromFavorites(title: String) {
        favoriteDao.deleteFavoriteByTitle(title)
    }

    override suspend fun clearFavorites() {
        favoriteDao.clearFavorites()
    }

    // ========== Локальные операции (история) ==========

    override fun getHistory(): Flow<List<String>> {
        return historyDao.getRecentHistory(limit = 20).map { entities ->
            entities.map { it.title }
        }
    }

    override suspend fun addToHistory(title: String) {
        val entity = HistoryEntity(
            title = title,
            searchedAt = System.currentTimeMillis(),
        )
        historyDao.insertHistory(entity)
        // Удаляем старые записи, оставляем только последние 50
        historyDao.deleteOldHistory(keepCount = 50)
    }

    override suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    // ========== Маппинги ==========

    private fun ApiLink.toDomain(): WikiLink {
        return WikiLink(
            title = this.title.orEmpty(),
            exists = this.exists != null,
        )
    }

    private fun FavoriteEntity.toDomain(gson: Gson): WikiArticle {
        val linksType = object : TypeToken<List<WikiLink>>() {}.type
        val externalLinksType = object : TypeToken<List<WikiExternalLink>>() {}.type

        val links: List<WikiLink> = try {
            gson.fromJson(this.links, linksType) ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error parsing links from favorite")
            emptyList()
        }

        val externalLinks: List<WikiExternalLink> = try {
            gson.fromJson(this.externalLinks, externalLinksType) ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error parsing external links from favorite")
            emptyList()
        }

        return WikiArticle(
            title = this.title,
            pageId = 0, // Не храним pageId в избранном
            content = this.content,
            links = links,
            externalLinks = externalLinks,
            articleUrl = this.articleUrl,
        )
    }

    private fun WikiArticle.toEntity(gson: Gson): FavoriteEntity {
        return FavoriteEntity(
            title = this.title,
            content = this.content,
            links = gson.toJson(this.links),
            externalLinks = gson.toJson(this.externalLinks),
            articleUrl = this.articleUrl,
            savedAt = System.currentTimeMillis(),
        )
    }

    // ========== Парсинг HTML ==========

    /**
     * Извлекает внешние ссылки из HTML-контента
     * Ищет теги <a> с class="external"
     */
    private fun parseExternalLinks(html: String): List<WikiExternalLink> {
        val regex = """<a[^>]*class="external[^"]*"[^>]*href="([^"]+)"[^>]*>([^<]*)</a>""".toRegex()
        return regex.findAll(html).map { match ->
            WikiExternalLink(
                url = match.groupValues[1],
                text = match.groupValues[2].trim(),
            )
        }.filter { it.text.isNotEmpty() }.toList()
    }
}