package su.sv.wiki.domain.usecase

import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.wiki.domain.model.WikiArticle
import su.sv.wiki.domain.repository.WikiRepository
import su.sv.wiki.domain.repository.WikiResult
import javax.inject.Inject

/**
 * Use Case для получения статьи по заголовку
 * Main-safe: выполняет IO операции на IO dispatcher
 */
class GetArticleUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: WikiRepository,
) {

    suspend fun execute(title: String): WikiResult<WikiArticle> {
        // Сетевой запрос и DB кэш - IO операция
        return dispatcherProvider.io.let { io ->
            withContext(io) {
                repository.getArticle(title)
            }
        }
    }
}
