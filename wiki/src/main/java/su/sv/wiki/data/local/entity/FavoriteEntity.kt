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
    val links: String,             // JSON строка со списком ссылок
    val savedAt: Long,             // Timestamp сохранения
)
