package su.sv.wiki.domain.usecase

import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase для очистки всех избранных статей
 */
@Singleton
class ClearFavoritesUseCase @Inject constructor(
    private val repository: WikiRepository,
) {
    /**
     * Очистить всё избранное
     */
    suspend fun execute() {
        repository.clearFavorites()
    }
}
