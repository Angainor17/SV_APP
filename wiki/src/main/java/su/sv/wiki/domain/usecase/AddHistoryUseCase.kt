package su.sv.wiki.domain.usecase

import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject

/**
 * Use Case для добавления статьи в историю
 */
class AddHistoryUseCase @Inject constructor(
    private val repository: WikiRepository,
) {

    suspend fun execute(title: String) {
        repository.addToHistory(title)
    }
}
