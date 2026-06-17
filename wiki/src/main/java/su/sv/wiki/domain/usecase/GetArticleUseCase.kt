package su.sv.wiki.domain.usecase

import su.sv.wiki.domain.model.WikiArticle
import su.sv.wiki.domain.repository.WikiRepository
import su.sv.wiki.domain.repository.WikiResult
import javax.inject.Inject

/**
 * Use Case для получения статьи по заголовку
 */
class GetArticleUseCase @Inject constructor(
    private val repository: WikiRepository,
) {

    suspend fun execute(title: String): WikiResult<WikiArticle> {
        return repository.getArticle(title)
    }
}
