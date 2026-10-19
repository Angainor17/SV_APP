package su.sv.quiz.data.source

import su.sv.quiz.domain.model.Quiz
import su.sv.quiz.domain.model.QuizTheme

/**
 * Источник данных квизов.
 *
 * Абстракция, позволяющая переключаться между моком и реальным API без
 * изменения вышележащих слоёв (repository/usecase/presentation).
 */
interface QuizDataSource {

    /** Получить все темы. */
    suspend fun getThemes(): List<QuizTheme>

    /** Получить все квизы. */
    suspend fun getQuizzes(): List<Quiz>

    /** Получить квиз по идентификатору (null, если не найден). */
    suspend fun getQuiz(quizId: String): Quiz?
}
