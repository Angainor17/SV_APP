package su.sv.wiki.domain.usecase

import kotlinx.coroutines.flow.Flow
import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase для проверки наличия избранных статей
 */
@Singleton
class HasFavoritesUseCase @Inject constructor(
    private val repository: WikiRepository,
) {
    /**
     * Проверить, есть ли хоть одно избранное
     * @return Flow<Boolean> - true если есть хоть одно избранное
     */
    fun execute(): Flow<Boolean> {
        return repository.hasFavorites()
    }
}
