package su.sv.wiki.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения избранных статей
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val title: String,
    val content: String,           // HTML контент
    val links: String,             // JSON строка со списком внутренних ссылок
    val externalLinks: String,     // JSON строка со списком внешних ссылок
    val articleUrl: String,        // URL статьи на сайте (например, https://svremya.su/Марксизм)
    val savedAt: Long,             // Timestamp сохранения
)
