package su.sv.wiki.domain.usecase

import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject

/**
 * Use Case для очистки истории поиска
 */
class ClearHistoryUseCase @Inject constructor(
    private val repository: WikiRepository,
) {

    suspend fun execute() {
        repository.clearHistory()
    }
}
