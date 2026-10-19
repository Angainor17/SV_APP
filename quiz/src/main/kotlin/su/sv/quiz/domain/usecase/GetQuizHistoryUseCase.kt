package su.sv.quiz.domain.usecase

import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.quiz.domain.model.QuizResult
import su.sv.quiz.domain.repository.QuizRepository
import javax.inject.Inject

/**
 * Получить историю прохождений квизов.
 */
class GetQuizHistoryUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: QuizRepository,
) {
    suspend operator fun invoke(): Result<List<QuizResult>> =
        withContext(dispatcherProvider.io) { repository.getHistory() }
}
