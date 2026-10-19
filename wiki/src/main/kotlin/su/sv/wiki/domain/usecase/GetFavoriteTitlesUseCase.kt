package su.sv.wiki.domain.usecase

import kotlinx.coroutines.flow.Flow
import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase для получения списка названий избранных статей
 */
@Singleton
class GetFavoriteTitlesUseCase @Inject constructor(
    private val repository: WikiRepository,
) {
    /**
     * Получить список названий избранных статей
     * @return Flow<List<String>> - поток со списком названий
     */
    fun execute(): Flow<List<String>> {
        return repository.getFavoriteTitles()
    }
}
