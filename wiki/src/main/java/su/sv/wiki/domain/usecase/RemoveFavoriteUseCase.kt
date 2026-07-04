package su.sv.wiki.domain.usecase

import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject

/**
 * Use Case для удаления статьи из избранного
 * Main-safe: выполняет DB операцию на IO dispatcher
 */
class RemoveFavoriteUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: WikiRepository,
) {

    suspend fun execute(title: String) {
        // DB операция - IO
        kotlinx.coroutines.withContext(dispatcherProvider.io) {
            repository.removeFromFavorites(title)
        }
    }
}
