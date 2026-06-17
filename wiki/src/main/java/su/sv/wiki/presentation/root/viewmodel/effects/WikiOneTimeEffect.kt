package su.sv.wiki.presentation.root.viewmodel.effects

/**
 * Одноразовые события на экране Wiki
 */
sealed class WikiOneTimeEffect {

    /** Показать сообщение об ошибке */
    data class ShowErrorSnackBar(val text: String) : WikiOneTimeEffect()

    /** Показать сообщение об успешном добавлении в избранное */
    data class ShowAddedToFavorites(val title: String) : WikiOneTimeEffect()

    /** Показать сообщение об удалении из избранного */
    data class ShowRemovedFromFavorites(val title: String) : WikiOneTimeEffect()
}
