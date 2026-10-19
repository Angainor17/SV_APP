package su.sv.wiki.domain.usecase

import kotlinx.coroutines.flow.Flow
import su.sv.wiki.domain.model.WikiArticle
import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject

/**
 * Use Case для получения списка избранных статей
 */
class GetFavoritesUseCase @Inject constructor(
    private val repository: WikiRepository,
) {

    operator fun invoke(): Flow<List<WikiArticle>> {
        return repository.getFavorites()
    }
}
