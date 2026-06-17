package su.sv.wiki.domain.usecase

import su.sv.wiki.domain.model.WikiArticle
import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject

/**
 * Use Case для добавления статьи в избранное
 */
class AddFavoriteUseCase @Inject constructor(
    private val repository: WikiRepository,
) {

    suspend fun execute(article: WikiArticle) {
        repository.addToFavorites(article)
    }
}
