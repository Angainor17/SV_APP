package su.sv.wiki.domain.usecase

import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject

/**
 * Use Case для удаления статьи из избранного
 */
class RemoveFavoriteUseCase @Inject constructor(
    private val repository: WikiRepository,
) {

    suspend fun execute(title: String) {
        repository.removeFromFavorites(title)
    }
}
