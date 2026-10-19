package su.sv.wiki.presentation.root.model

import androidx.compose.runtime.Immutable

/**
 * Состояние экрана Wiki
 */
sealed class UiWikiState {

    /** Начальное состояние - показываем историю */
    object Initial : UiWikiState()

    /** Загрузка */
    object Loading : UiWikiState()

    /**
     * Контент - статья найдена
     * @Immutable - оптимизация Compose recomposition
     */
    @Immutable
    data class Content(
        val article: UiWikiArticle,
        val isFavorite: Boolean = false,
    ) : UiWikiState()

    /** Ничего не найдено */
    object NotFound : UiWikiState()

    /** Ошибка */
    data class Error(val message: String) : UiWikiState()
}
