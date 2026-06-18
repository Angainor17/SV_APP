package su.sv.wiki.domain.usecase

import su.sv.wiki.domain.model.WikiSearchSuggestion
import su.sv.wiki.domain.repository.WikiRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase для получения подсказок поиска (автодополнение)
 */
@Singleton
class GetSearchSuggestionsUseCase @Inject constructor(
    private val repository: WikiRepository,
) {
    /**
     * Получить подсказки для поискового запроса
     * @param query поисковый запрос (минимум 2 символа)
     * @param limit максимальное количество подсказок (по умолчанию 5)
     * @return список подсказок
     */
    suspend fun execute(query: String, limit: Int = 5): List<WikiSearchSuggestion> {
        return repository.getSearchSuggestions(query, limit)
    }
}
