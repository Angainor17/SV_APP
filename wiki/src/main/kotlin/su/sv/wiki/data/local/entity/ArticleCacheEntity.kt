package su.sv.wiki.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для кэширования статей
 * @Index on cachedAt - оптимизация для ORDER BY cachedAt
 */
@Entity(
    tableName = "article_cache",
    indices = [
        Index(value = ["cachedAt"]),
    ]
)
data class ArticleCacheEntity(
    @PrimaryKey
    val title: String,
    val content: String,           // HTML контент
    val links: String,             // JSON строка со списком внутренних ссылок
    val externalLinks: String,     // JSON строка со списком внешних ссылок
    val articleUrl: String,        // URL статьи на сайте
    val imageUrl: String?,         // URL картинки из статьи (если есть)
    val cachedAt: Long,            // Timestamp кэширования
)
