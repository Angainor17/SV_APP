package su.sv.qa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import su.sv.qa.data.local.entity.AnsweredQuestionEntity

/**
 * DAO для работы с локальным кэшем отвеченных вопросов
 */
@Dao
interface AnsweredQuestionDao {

    /**
     * Все отвеченные вопросы (поток для реактивности)
     */
    @Query("SELECT * FROM answered_questions ORDER BY answerUpdatedAt DESC")
    fun getAll(): Flow<List<AnsweredQuestionEntity>>

    /**
     * Отвеченные вопросы для конкретной книги
     */
    @Query("SELECT * FROM answered_questions WHERE bookId = :bookId ORDER BY answerUpdatedAt DESC")
    fun getForBook(bookId: String): Flow<List<AnsweredQuestionEntity>>

    /**
     * Вставить/обновить список вопросов, полученных с сервера
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(questions: List<AnsweredQuestionEntity>)

    /**
     * Максимальный answerUpdatedAt среди уже сохранённых вопросов — база для параметра `since` при синхронизации
     */
    @Query("SELECT MAX(answerUpdatedAt) FROM answered_questions")
    suspend fun getMaxAnswerUpdatedAt(): Long?
}
