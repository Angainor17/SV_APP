package su.sv.quiz.data.repository

import su.sv.quiz.data.source.QuizDataSource
import su.sv.quiz.domain.model.Quiz
import su.sv.quiz.domain.model.QuizResult
import su.sv.quiz.domain.model.QuizTheme
import su.sv.quiz.domain.repository.QuizRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [QuizRepository] на основе [QuizDataSource].
 *
 * История прохождений хранится в памяти (лучший результат по каждому квизу).
 * Позже будет заменена на Room-базу (см. ТЗ, раздел «Кеширование»).
 */
@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val dataSource: QuizDataSource,
) : QuizRepository {

    /** Лучшие результаты по квизам (in-memory, до появления Room). */
    private val bestResults = mutableMapOf<String, QuizResult>()

    override suspend fun getThemes(): Result<List<QuizTheme>> = runCatching {
        val themes = dataSource.getThemes()
        val quizzes = dataSource.getQuizzes()
        themes.map { theme ->
            theme.copy(quizCount = quizzes.count { it.themeId == theme.id })
        }
    }

    override suspend fun getNewQuizzes(limit: Int): Result<List<Quiz>> = runCatching {
        val cutoff = System.currentTimeMillis() - NEW_QUIZ_WINDOW_MS
        dataSource.getQuizzes()
            .filter { it.createdAtEpochMillis >= cutoff }
            .take(limit)
    }

    override suspend fun getQuizzesByTheme(themeId: String): Result<List<Quiz>> = runCatching {
        dataSource.getQuizzes().filter { it.themeId == themeId }
    }

    override suspend fun getQuiz(quizId: String): Result<Quiz> = runCatching {
        dataSource.getQuiz(quizId) ?: throw NoSuchElementException("Квиз не найден: $quizId")
    }

    override suspend fun getHistory(): Result<List<QuizResult>> = runCatching {
        bestResults.values.sortedByDescending { it.completedAtEpochMillis }
    }

    override suspend fun saveResult(result: QuizResult): Result<Unit> = runCatching {
        val existing = bestResults[result.quizId]
        if (existing == null || result.percentage > existing.percentage) {
            bestResults[result.quizId] = result
        }
    }

    private companion object {
        const val NEW_QUIZ_WINDOW_MS = 14L * 24 * 60 * 60 * 1000
    }
}
