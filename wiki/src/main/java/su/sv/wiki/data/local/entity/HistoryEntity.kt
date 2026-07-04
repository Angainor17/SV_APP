package su.sv.wiki.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для хранения истории поиска
 * @Index on title - оптимизация для DELETE WHERE title = :title
 */
@Entity(
    tableName = "history",
    indices = [
        Index(value = ["title"], unique = true),
        Index(value = ["searchedAt"]),
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,             // Название статьи
    val searchedAt: Long,          // Timestamp поиска
)
