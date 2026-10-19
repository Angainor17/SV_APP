package su.sv.qa.domain.repository

import kotlinx.coroutines.flow.Flow
import su.sv.qa.domain.model.AnsweredQuestion
import su.sv.qa.domain.model.QuestionReport
import su.sv.qa.domain.model.TypoReport

/**
 * Репозиторий для работы с вопросами/ответами и репортами об опечатках
 */
interface QaRepository {

    /**
     * Отправить репорт об опечатке
     */
    suspend fun submitTypoReport(report: TypoReport): Result<Unit>

    /**
     * Отправить вопрос по выделенному тексту
     */
    suspend fun submitQuestionReport(report: QuestionReport): Result<Unit>

    /**
     * Синхронизировать отвеченные вопросы с бэкендом (инкрементально, по answerUpdatedAt)
     */
    suspend fun syncAnsweredQuestions(): Result<Unit>

    /**
     * Все отвеченные вопросы (локальный кэш)
     */
    fun observeAnsweredQuestions(): Flow<List<AnsweredQuestion>>

    /**
     * Отвеченные вопросы для конкретной книги (локальный кэш)
     */
    fun observeAnsweredQuestionsForBook(catalogBookId: String): Flow<List<AnsweredQuestion>>
}
