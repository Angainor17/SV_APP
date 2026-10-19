package su.sv.quiz.domain.usecase

import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.quiz.domain.model.Quiz
import su.sv.quiz.domain.repository.QuizRepository
import javax.inject.Inject

/**
 * Получить новые квизы (созданные за последние 14 дней).
 */
class GetNewQuizzesUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: QuizRepository,
) {
    suspend operator fun invoke(limit: Int): Result<List<Quiz>> =
        withContext(dispatcherProvider.io) { repository.getNewQuizzes(limit) }
}
