package su.sv.wiki.presentation.root.model

/**
 * Состояние экрана Wiki
 */
sealed class UiWikiState {

    /** Начальное состояние - показываем историю */
    object Initial : UiWikiState()

    /** Загрузка */
    object Loading : UiWikiState()

    /** Контент - статья найдена */
    data class Content(
        val article: UiWikiArticle,
        val isFavorite: Boolean = false,
    ) : UiWikiState()

    /** Ничего не найдено */
    object NotFound : UiWikiState()

    /** Ошибка */
    data class Error(val message: String) : UiWikiState()
}
