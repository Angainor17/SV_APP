package su.sv.quiz.domain.repository

import su.sv.quiz.domain.model.Quiz
import su.sv.quiz.domain.model.QuizResult
import su.sv.quiz.domain.model.QuizTheme

/**
 * Репозиторий квизов.
 *
 * Единственная точка доступа презентационного слоя к данным квизов.
 * Сейчас данные отдаёт мок-источник (см. data/source), позже — реальный API.
 */
interface QuizRepository {

    /** Получить список тем с количеством квизов в каждой. */
    suspend fun getThemes(): Result<List<QuizTheme>>

    /** Получить новые квизы (созданные за последние 14 дней), не более [limit]. */
    suspend fun getNewQuizzes(limit: Int): Result<List<Quiz>>

    /** Получить квизы указанной темы. */
    suspend fun getQuizzesByTheme(themeId: String): Result<List<Quiz>>

    /** Получить квиз по идентификатору. */
    suspend fun getQuiz(quizId: String): Result<Quiz>

    /** Получить историю прохождений (лучшие результаты), новые — сверху. */
    suspend fun getHistory(): Result<List<QuizResult>>

    /** Сохранить результат прохождения (хранится лучший результат по квизу). */
    suspend fun saveResult(result: QuizResult): Result<Unit>
}
