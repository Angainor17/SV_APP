package su.sv.wiki.domain.usecase

import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject

/**
 * Use Case для добавления статьи в историю
 * Main-safe: выполняет DB операцию на IO dispatcher
 */
class AddHistoryUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: WikiRepository,
) {

    suspend fun execute(title: String) {
        // DB операция - IO
        withContext(dispatcherProvider.io) {
            repository.addToHistory(title)
        }
    }
}
