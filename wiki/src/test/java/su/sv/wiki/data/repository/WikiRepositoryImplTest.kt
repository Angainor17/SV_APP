package su.sv.wiki.data.repository

import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import su.sv.wiki.data.api.WikiApi
import su.sv.wiki.data.api.model.ApiError
import su.sv.wiki.data.api.model.ApiLink
import su.sv.wiki.data.api.model.ApiParseData
import su.sv.wiki.data.api.model.ApiParseResponse
import su.sv.wiki.data.api.model.ApiSearchItem
import su.sv.wiki.data.api.model.ApiSearchQuery
import su.sv.wiki.data.api.model.ApiSearchResponse
import su.sv.wiki.data.api.model.ApiTextContent
import su.sv.wiki.data.local.dao.ArticleCacheDao
import su.sv.wiki.data.local.dao.FavoriteDao
import su.sv.wiki.data.local.dao.HistoryDao
import su.sv.wiki.data.local.entity.ArticleCacheEntity
import su.sv.wiki.data.local.entity.FavoriteEntity
import su.sv.wiki.data.local.entity.HistoryEntity
import su.sv.wiki.domain.model.WikiArticle
import su.sv.wiki.domain.model.WikiSearchResult
import su.sv.wiki.domain.repository.WikiResult

/**
 * Unit-тесты для WikiRepositoryImpl.
 *
 * Тестируют:
 * - Поиск статей (успех, не найдено, ошибка сети, исключение)
 * - Получение статьи (из сети, из кэша, не найдено, ошибка)
 * - Подсказки поиска
 * - Избранное (добавление, удаление, проверка)
 * - История поиска
 */
class WikiRepositoryImplTest {

    // ========== Mocks ==========

    private val api: WikiApi = mockk()
    private val favoriteDao: FavoriteDao = mockk()
    private val historyDao: HistoryDao = mockk()
    private val articleCacheDao: ArticleCacheDao = mockk()
    private val gson: Gson = Gson()

    private val repository = WikiRepositoryImpl(
        api = api,
        favoriteDao = favoriteDao,
        historyDao = historyDao,
        articleCacheDao = articleCacheDao,
        gson = gson,
    )

    // ========== searchArticle ==========

    @Test
    fun `searchArticle returns Success when API returns results`() = runTest {
        val query = "Тест"
        val searchItem = ApiSearchItem(
            title = "Тестовая статья",
            pageId = 123,
            snippet = "Описание статьи",
        )
        val response = ApiSearchResponse(
            query = ApiSearchQuery(search = listOf(searchItem))
        )

        coEvery { api.search(query = query, what = "title") } returns Response.success(response)

        val result = repository.searchArticle(query)

        assertTrue(result is WikiResult.Success)
        val data = (result as WikiResult.Success).data
        assertEquals("Тестовая статья", data.title)
        assertEquals(123, data.pageId)
        assertEquals("Описание статьи", data.snippet)
    }

    @Test
    fun `searchArticle returns NotFound when no results`() = runTest {
        val query = "НеСуществующаяСтатья"
        val response = ApiSearchResponse(
            query = ApiSearchQuery(search = emptyList())
        )

        coEvery { api.search(query = query, what = "title") } returns Response.success(response)

        val result = repository.searchArticle(query)

        assertTrue(result is WikiResult.NotFound)
    }

    @Test
    fun `searchArticle returns Error when API returns error code`() = runTest {
        val query = "Тест"

        coEvery {
            api.search(query = query, what = "title")
        } returns Response.error(404, "Not Found".toResponseBody())

        val result = repository.searchArticle(query)

        assertTrue(result is WikiResult.Error)
        assertEquals("404", (result as WikiResult.Error).code)
    }

    @Test
    fun `searchArticle returns Error when exception occurs`() = runTest {
        val query = "Тест"

        coEvery { api.search(query = query, what = "title") } throws RuntimeException("Network failure")

        val result = repository.searchArticle(query)

        assertTrue(result is WikiResult.Error)
        assertEquals("NETWORK_ERROR", (result as WikiResult.Error).code)
    }

    // ========== getArticle ==========

    @Test
    fun `getArticle returns Success from network when not cached`() = runTest {
        val title = "Тестовая статья"

        coEvery { articleCacheDao.getArticleByTitle(title) } returns null
        coEvery { api.getPage(title = title) } returns Response.success(
            ApiParseResponse(
                parse = ApiParseData(
                    title = title,
                    pageId = 123,
                    text = ApiTextContent(content = "<p>Контент статьи</p>"),
                    links = listOf(ApiLink(title = "Ссылка 1", exists = "")),
                )
            )
        )
        coEvery { articleCacheDao.insertArticle(any()) } returns Unit

        val result = repository.getArticle(title)

        assertTrue(result is WikiResult.Success)
        val article = (result as WikiResult.Success).data
        assertEquals(title, article.title)
        assertEquals(123, article.pageId)
        assertEquals("<p>Контент статьи</p>", article.content)
        assertEquals(1, article.links.size)

        coVerify { articleCacheDao.insertArticle(any()) }
    }

    @Test
    fun `getArticle returns Success from cache when available`() = runTest {
        val title = "Кэшированная статья"

        val cachedEntity = ArticleCacheEntity(
            title = title,
            content = "<p>Кэшированный контент</p>",
            links = "[]",
            externalLinks = "[]",
            articleUrl = "https://svremya.su/$title",
            imageUrl = null,
            cachedAt = System.currentTimeMillis(),
        )
        coEvery { articleCacheDao.getArticleByTitle(title) } returns cachedEntity

        val result = repository.getArticle(title)

        assertTrue(result is WikiResult.Success)
        val article = (result as WikiResult.Success).data
        assertEquals(title, article.title)
        assertEquals("<p>Кэшированный контент</p>", article.content)

        coVerify(exactly = 0) { api.getPage(title = any()) }
    }

    @Test
    fun `getArticle returns NotFound when article missing`() = runTest {
        val title = "Несуществующая статья"

        coEvery { articleCacheDao.getArticleByTitle(title) } returns null
        coEvery { api.getPage(title = title) } returns Response.success(
            ApiParseResponse(error = ApiError(code = "missingtitle", info = "Not found"))
        )

        val result = repository.getArticle(title)

        assertTrue(result is WikiResult.NotFound)
    }

    @Test
    fun `getArticle returns Error when API fails`() = runTest {
        val title = "Статья"

        coEvery { articleCacheDao.getArticleByTitle(title) } returns null
        coEvery { api.getPage(title = title) } returns Response.error(500, "Server Error".toResponseBody())

        val result = repository.getArticle(title)

        assertTrue(result is WikiResult.Error)
        assertEquals("500", (result as WikiResult.Error).code)
    }

    // ========== getSearchSuggestions ==========

    @Test
    fun `getSearchSuggestions returns empty for short queries`() = runTest {
        val result = repository.getSearchSuggestions("a", limit = 5)

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { api.search(query = any(), what = any(), limit = any()) }
    }

    @Test
    fun `getSearchSuggestions returns suggestions from API`() = runTest {
        val query = "Маркс"
        val items = listOf(
            ApiSearchItem(title = "Маркс, Карл", pageId = 1),
            ApiSearchItem(title = "Марксизм", pageId = 2),
        )
        val response = ApiSearchResponse(
            query = ApiSearchQuery(search = items)
        )

        coEvery {
            api.search(query = query, what = "title", limit = any())
        } returns Response.success(response)

        val result = repository.getSearchSuggestions(query, limit = 5)

        assertEquals(2, result.size)
        assertEquals("Маркс, Карл", result[0].title)
        assertEquals("Марксизм", result[1].title)
    }

    @Test
    fun `getSearchSuggestions returns empty list on error`() = runTest {
        val query = "Тест"

        coEvery {
            api.search(query = query, what = "title", limit = any())
        } throws RuntimeException("API error")

        val result = repository.getSearchSuggestions(query)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getSearchSuggestions returns empty when response has null body`() = runTest {
        val query = "Тест"

        coEvery {
            api.search(query = query, what = "title", limit = any())
        } returns Response.success(ApiSearchResponse(query = null))

        val result = repository.getSearchSuggestions(query)

        assertTrue(result.isEmpty())
    }

    // ========== Favorites ==========

    @Test
    fun `getFavorites returns list from DAO`() = runTest {
        val entities = listOf(
            FavoriteEntity(
                title = "Статья 1",
                content = "<p>Контент 1</p>",
                links = "[]",
                externalLinks = "[]",
                articleUrl = "https://svremya.su/Статья_1",
                imageUrl = null,
                savedAt = System.currentTimeMillis(),
            )
        )

        every { favoriteDao.getAllFavorites() } returns flowOf(entities)

        val result = repository.getFavorites().first()

        assertEquals(1, result.size)
        assertEquals("Статья 1", result[0].title)
    }

    @Test
    fun `addToFavorites inserts into DAO`() = runTest {
        val article = WikiArticle(
            title = "Новая статья",
            pageId = 1,
            content = "<p>Контент</p>",
            links = emptyList(),
            externalLinks = emptyList(),
            articleUrl = "https://svremya.su/Новая_статья",
            imageUrl = null,
        )

        coEvery { favoriteDao.insertFavorite(any()) } returns Unit

        repository.addToFavorites(article)

        coVerify { favoriteDao.insertFavorite(any()) }
    }

    @Test
    fun `removeFromFavorites deletes from DAO`() = runTest {
        coEvery { favoriteDao.deleteFavoriteByTitle("Статья") } returns Unit

        repository.removeFromFavorites("Статья")

        coVerify { favoriteDao.deleteFavoriteByTitle("Статья") }
    }

    @Test
    fun `isFavorite delegates to DAO`() = runTest {
        coEvery { favoriteDao.isFavorite("Статья") } returns true

        val result = repository.isFavorite("Статья")

        assertTrue(result)
        coVerify { favoriteDao.isFavorite("Статья") }
    }

    @Test
    fun `clearFavorites clears DAO`() = runTest {
        coEvery { favoriteDao.clearFavorites() } returns Unit

        repository.clearFavorites()

        coVerify { favoriteDao.clearFavorites() }
    }

    // ========== History ==========

    @Test
    fun `getHistory returns list from DAO`() = runTest {
        val entities = listOf(
            HistoryEntity(title = "Статья 1", searchedAt = 1000),
            HistoryEntity(title = "Статья 2", searchedAt = 2000),
        )

        every { historyDao.getRecentHistory(20) } returns flowOf(entities)

        val result = repository.getHistory().first()

        assertEquals(2, result.size)
        assertEquals("Статья 1", result[0])
        assertEquals("Статья 2", result[1])
    }

    @Test
    fun `addToHistory removes duplicate and inserts`() = runTest {
        coEvery { historyDao.deleteHistoryByTitle("Статья") } returns Unit
        coEvery { historyDao.insertHistory(any()) } returns Unit
        coEvery { historyDao.deleteOldHistory(50) } returns Unit

        repository.addToHistory("Статья")

        coVerify { historyDao.deleteHistoryByTitle("Статья") }
        coVerify { historyDao.insertHistory(any()) }
        coVerify { historyDao.deleteOldHistory(50) }
    }

    @Test
    fun `clearHistory clears DAO`() = runTest {
        coEvery { historyDao.clearHistory() } returns Unit

        repository.clearHistory()

        coVerify { historyDao.clearHistory() }
    }
}
