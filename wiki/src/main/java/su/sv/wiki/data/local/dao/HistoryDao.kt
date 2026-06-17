package su.sv.wiki.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import su.sv.wiki.data.local.entity.HistoryEntity

/**
 * DAO для работы с историей поиска
 */
@Dao
interface HistoryDao {

    /**
     * Получить всю историю (поток для реактивности)
     */
    @Query("SELECT * FROM history ORDER BY searchedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    /**
     * Получить последние N записей истории
     */
    @Query("SELECT * FROM history ORDER BY searchedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 20): Flow<List<HistoryEntity>>

    /**
     * Добавить запись в историю
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    /**
     * Удалить запись из истории
     */
    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    /**
     * Очистить всю историю
     */
    @Query("DELETE FROM history")
    suspend fun clearHistory()

    /**
     * Удалить старые записи (оставить последние N)
     */
    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY searchedAt DESC LIMIT :keepCount)")
    suspend fun deleteOldHistory(keepCount: Int = 50)
}
