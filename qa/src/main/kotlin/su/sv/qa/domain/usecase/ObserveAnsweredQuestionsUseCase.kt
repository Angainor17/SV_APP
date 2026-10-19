package su.sv.qa.domain.usecase

import kotlinx.coroutines.flow.Flow
import su.sv.qa.domain.model.AnsweredQuestion
import su.sv.qa.domain.repository.QaRepository
import javax.inject.Inject

/**
 * Use Case для получения всех отвеченных вопросов (локальный кэш)
 */
class ObserveAnsweredQuestionsUseCase @Inject constructor(
    private val repository: QaRepository,
) {

    operator fun invoke(): Flow<List<AnsweredQuestion>> {
        return repository.observeAnsweredQuestions()
    }
}
