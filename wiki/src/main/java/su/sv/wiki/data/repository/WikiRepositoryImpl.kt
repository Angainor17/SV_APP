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
import su.sv.wiki.domain.model.WikiLink
import su.sv.wiki.domain.model.WikiSearchResult
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
                    WikiResult.Success(
                        WikiArticle(
                            title = parseData.title.orEmpty(),
                            pageId = parseData.pageId ?: 0,
                            content = parseData.text?.content.orEmpty(),
                            links = parseData.links?.map { it.toDomain() }.orEmpty(),
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

    // ========== Локальные операции (избранное) ==========

    override fun getFavorites(): Flow<List<WikiArticle>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toDomain(gson) }
        }
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
        val links: List<WikiLink> = try {
            gson.fromJson(this.links, linksType) ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error parsing links from favorite")
            emptyList()
        }

        return WikiArticle(
            title = this.title,
            pageId = 0, // Не храним pageId в избранном
            content = this.content,
            links = links,
        )
    }

    private fun WikiArticle.toEntity(gson: Gson): FavoriteEntity {
        return FavoriteEntity(
            title = this.title,
            content = this.content,
            links = gson.toJson(this.links),
            savedAt = System.currentTimeMillis(),
        )
    }
}
