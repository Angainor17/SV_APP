package su.sv.wiki.domain.repository

import kotlinx.coroutines.flow.Flow
import su.sv.wiki.domain.model.WikiArticle
import su.sv.wiki.domain.model.WikiSearchResult
import su.sv.wiki.domain.model.WikiSearchSuggestion

/**
 * Результат операции
 */
sealed class WikiResult<out T> {
    data class Success<T>(val data: T) : WikiResult<T>()
    data class Error(val message: String, val code: String? = null) : WikiResult<Nothing>()
    data object NotFound : WikiResult<Nothing>()
}

/**
 * Репозиторий для работы с Wiki
 */
interface WikiRepository {

    // ========== Удалённые операции ==========

    /**
     * Поиск статьи по запросу
     * @param query поисковый запрос
     * @return результат поиска или ошибка
     */
    suspend fun searchArticle(query: String): WikiResult<WikiSearchResult>

    /**
     * Получение статьи по заголовку
     * @param title заголовок статьи
     * @return статья или ошибка
     */
    suspend fun getArticle(title: String): WikiResult<WikiArticle>

    /**
     * Получение подсказок для поиска (автодополнение)
     * @param query поисковый запрос
     * @param limit максимальное количество подсказок
     * @return список подсказок
     */
    suspend fun getSearchSuggestions(query: String, limit: Int = 5): List<WikiSearchSuggestion>

    // ========== Локальные операции (избранное) ==========

    /**
     * Получить все избранные статьи
     */
    fun getFavorites(): Flow<List<WikiArticle>>

    /**
     * Получить список названий избранных статей
     */
    fun getFavoriteTitles(): Flow<List<String>>

    /**
     * Проверить, есть ли хоть одно избранное
     */
    fun hasFavorites(): Flow<Boolean>

    /**
     * Проверить, есть ли статья в избранном
     */
    suspend fun isFavorite(title: String): Boolean

    /**
     * Добавить статью в избранное
     */
    suspend fun addToFavorites(article: WikiArticle)

    /**
     * Удалить статью из избранного
     */
    suspend fun removeFromFavorites(title: String)

    /**
     * Очистить всё избранное
     */
    suspend fun clearFavorites()

    // ========== Локальные операции (история) ==========

    /**
     * Получить историю поиска
     */
    fun getHistory(): Flow<List<String>>

    /**
     * Добавить статью в историю
     */
    suspend fun addToHistory(title: String)

    /**
     * Очистить историю
     */
    suspend fun clearHistory()
}
