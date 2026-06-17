package su.sv.wiki.domain.usecase

import su.sv.wiki.domain.model.WikiSearchResult
import su.sv.wiki.domain.repository.WikiRepository
import su.sv.wiki.domain.repository.WikiResult
import javax.inject.Inject

/**
 * Use Case для поиска статьи
 */
class SearchArticleUseCase @Inject constructor(
    private val repository: WikiRepository,
) {

    suspend fun execute(query: String): WikiResult<WikiSearchResult> {
        return repository.searchArticle(query)
    }
}
