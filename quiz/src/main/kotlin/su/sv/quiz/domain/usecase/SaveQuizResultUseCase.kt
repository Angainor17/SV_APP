package su.sv.quiz.domain.usecase

import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.quiz.domain.model.QuizResult
import su.sv.quiz.domain.repository.QuizRepository
import javax.inject.Inject

/**
 * Сохранить результат прохождения квиза.
 */
class SaveQuizResultUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: QuizRepository,
) {
    suspend operator fun invoke(result: QuizResult): Result<Unit> =
        withContext(dispatcherProvider.io) { repository.saveResult(result) }
}
