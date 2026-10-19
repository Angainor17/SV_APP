package su.sv.wiki.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import su.sv.wiki.data.local.entity.ArticleCacheEntity

/**
 * DAO для работы с кэшем статей
 */
@Dao
interface ArticleCacheDao {

    /**
     * Получить статью из кэша по названию
     */
    @Query("SELECT * FROM article_cache WHERE title = :title LIMIT 1")
    suspend fun getArticleByTitle(title: String): ArticleCacheEntity?

    /**
     * Сохранить статью в кэш
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleCacheEntity)

    /**
     * Удалить статью из кэша
     */
    @Query("DELETE FROM article_cache WHERE title = :title")
    suspend fun deleteArticleByTitle(title: String)

    /**
     * Очистить весь кэш
     */
    @Query("DELETE FROM article_cache")
    suspend fun clearCache()

    /**
     * Удалить старые записи (оставить последние N)
     */
    @Query("DELETE FROM article_cache WHERE title NOT IN (SELECT title FROM article_cache ORDER BY cachedAt DESC LIMIT :keepCount)")
    suspend fun deleteOldCache(keepCount: Int = 50)
}
