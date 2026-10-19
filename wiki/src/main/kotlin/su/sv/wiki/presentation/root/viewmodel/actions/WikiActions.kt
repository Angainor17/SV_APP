package su.sv.wiki.presentation.root.viewmodel.actions

import su.sv.wiki.domain.model.WikiArticle

/**
 * Действия на экране Wiki
 */
sealed class WikiActions {

    /** Поиск статьи */
    data class OnSearch(val query: String) : WikiActions()

    /** Изменение текста поиска (для подсказок) */
    data class OnSearchQueryChanged(val query: String) : WikiActions()

    /** Нажатие на подсказку */
    data class OnSuggestionClick(val title: String) : WikiActions()

    /** Сброс выбранной подсказки */
    object OnSuggestionApplied : WikiActions()

    /** Нажатие на ссылку в тексте статьи */
    data class OnLinkClick(val title: String) : WikiActions()

    /** Добавить в избранное */
    data class OnAddFavorite(val title: String) : WikiActions()

    /** Удалить из избранного */
    data class OnRemoveFavorite(val title: String) : WikiActions()

    /** Нажатие на элемент истории */
    data class OnHistoryItemClick(val title: String) : WikiActions()

    /** Очистить историю */
    object OnClearHistory : WikiActions()

    /** Повторить после ошибки */
    object OnRetryClick : WikiActions()

    /** Закрыть статью (вернуться к истории) */
    object OnCloseArticle : WikiActions()

    /** Выбрать статью из избранного (для master-detail на планшетах) */
    data class OnFavoriteClick(val article: WikiArticle) : WikiActions()

    /** Закрыть detail панель (для master-detail на планшетах) */
    object OnCloseDetail : WikiActions()
}
