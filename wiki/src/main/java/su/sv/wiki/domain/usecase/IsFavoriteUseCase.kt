package su.sv.wiki.domain.usecase

import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject

/**
 * Use Case для проверки, есть ли статья в избранном
 * Main-safe: выполняет DB запрос на IO dispatcher
 */
class IsFavoriteUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: WikiRepository,
) {

    suspend fun execute(title: String): Boolean {
        // DB запрос - IO операция
        return kotlinx.coroutines.withContext(dispatcherProvider.io) {
            repository.isFavorite(title)
        }
    }
}
