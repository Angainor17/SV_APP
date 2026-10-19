package su.sv.qa.domain.usecase

import kotlinx.coroutines.flow.Flow
import su.sv.qa.domain.model.AnsweredQuestion
import su.sv.qa.domain.repository.QaRepository
import javax.inject.Inject

/**
 * Use Case для получения отвеченных вопросов конкретной книги (локальный кэш)
 */
class ObserveAnsweredQuestionsForBookUseCase @Inject constructor(
    private val repository: QaRepository,
) {

    operator fun invoke(catalogBookId: String): Flow<List<AnsweredQuestion>> {
        return repository.observeAnsweredQuestionsForBook(catalogBookId)
    }
}
