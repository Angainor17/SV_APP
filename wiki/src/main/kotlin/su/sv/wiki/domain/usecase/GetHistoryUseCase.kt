package su.sv.wiki.domain.usecase

import kotlinx.coroutines.flow.Flow
import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject

/**
 * Use Case для получения истории поиска
 */
class GetHistoryUseCase @Inject constructor(
    private val repository: WikiRepository,
) {

    operator fun invoke(): Flow<List<String>> {
        return repository.getHistory()
    }
}
