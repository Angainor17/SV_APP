package su.sv.quiz.domain.usecase

import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.quiz.domain.model.Quiz
import su.sv.quiz.domain.repository.QuizRepository
import javax.inject.Inject

/**
 * Получить квизы указанной темы.
 */
class GetQuizzesByThemeUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: QuizRepository,
) {
    suspend operator fun invoke(themeId: String): Result<List<Quiz>> =
        withContext(dispatcherProvider.io) { repository.getQuizzesByTheme(themeId) }
}
