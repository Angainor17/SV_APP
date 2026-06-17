package su.sv.wiki.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import su.sv.wiki.data.local.dao.FavoriteDao
import su.sv.wiki.data.local.dao.HistoryDao
import su.sv.wiki.data.local.entity.FavoriteEntity
import su.sv.wiki.data.local.entity.HistoryEntity

/**
 * Room Database для Wiki модуля
 */
@Database(
    entities = [
        FavoriteEntity::class,
        HistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class WikiDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
}
