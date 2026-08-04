package su.sv.qa.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import su.sv.qa.data.local.dao.AnsweredQuestionDao
import su.sv.qa.data.local.entity.AnsweredQuestionEntity

@Database(
    entities = [AnsweredQuestionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class QaDatabase : RoomDatabase() {
    abstract fun answeredQuestionDao(): AnsweredQuestionDao
}
