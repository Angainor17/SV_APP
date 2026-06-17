package su.sv.wiki.domain.usecase

import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject

/**
 * Use Case для проверки, есть ли статья в избранном
 */
class IsFavoriteUseCase @Inject constructor(
    private val repository: WikiRepository,
) {

    suspend fun execute(title: String): Boolean {
        return repository.isFavorite(title)
    }
}
