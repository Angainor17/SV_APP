package su.sv.wiki.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import su.sv.wiki.data.local.entity.FavoriteEntity

/**
 * DAO для работы с избранными статьями
 */
@Dao
interface FavoriteDao {

    /**
     * Получить все избранные статьи (поток для реактивности)
     */
    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    /**
     * Получить избранную статью по заголовку
     */
    @Query("SELECT * FROM favorites WHERE title = :title")
    suspend fun getFavoriteByTitle(title: String): FavoriteEntity?

    /**
     * Проверить, есть ли статья в избранном
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE title = :title)")
    suspend fun isFavorite(title: String): Boolean

    /**
     * Добавить статью в избранное
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    /**
     * Удалить статью из избранного
     */
    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    /**
     * Удалить статью из избранного по заголовку
     */
    @Query("DELETE FROM favorites WHERE title = :title")
    suspend fun deleteFavoriteByTitle(title: String)
}
