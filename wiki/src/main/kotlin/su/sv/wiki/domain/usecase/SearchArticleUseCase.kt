package su.sv.wiki.domain.usecase

import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.wiki.domain.model.WikiSearchResult
import su.sv.wiki.domain.repository.WikiRepository
import su.sv.wiki.domain.repository.WikiResult
import javax.inject.Inject

/**
 * Use Case для поиска статьи
 * Main-safe: выполняет сетевой запрос на IO dispatcher
 */
class SearchArticleUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: WikiRepository,
) {

    suspend fun execute(query: String): WikiResult<WikiSearchResult> {
        // Сетевой запрос - IO операция
        return withContext(dispatcherProvider.io) {
            repository.searchArticle(query)
        }
    }
}
