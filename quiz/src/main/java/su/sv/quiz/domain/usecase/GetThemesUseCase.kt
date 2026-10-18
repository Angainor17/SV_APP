package su.sv.quiz.domain.usecase

import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.quiz.domain.model.QuizTheme
import su.sv.quiz.domain.repository.QuizRepository
import javax.inject.Inject

/**
 * Получить список тем квизов.
 */
class GetThemesUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: QuizRepository,
) {
    suspend operator fun invoke(): Result<List<QuizTheme>> =
        withContext(dispatcherProvider.io) { repository.getThemes() }
}
