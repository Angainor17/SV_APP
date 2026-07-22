package su.sv.wiki.domain.usecase

import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.wiki.domain.model.WikiArticle
import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject

/**
 * Use Case для добавления статьи в избранное
 * Main-safe: выполняет DB операцию на IO dispatcher
 */
class AddFavoriteUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: WikiRepository,
) {

    suspend fun execute(article: WikiArticle) {
        // DB операция - IO
        withContext(dispatcherProvider.io) {
            repository.addToFavorites(article)
        }
    }
}
