package su.sv.wiki.testing

/**
 * TestTags для модуля wiki.
 */
object WikiTestTags {
    const val ROOT = "wiki_root"
    const val SEARCH_FIELD = "wiki_search_field"
    const val SUGGESTIONS_LIST = "wiki_suggestions_list"
    const val SUGGESTION_ITEM = "wiki_suggestion_item"
    const val HISTORY_LIST = "wiki_history_list"
    const val HISTORY_ITEM = "wiki_history_item"
    const val FAVORITES_BUTTON = "wiki_favorites_button"

    object Article {
        const val ROOT = "wiki_article_root"
        const val TITLE = "article_title"
        const val CONTENT = "article_content"
        const val FAVORITE_BUTTON = "article_favorite_button"
        const val LINK = "article_link"
        const val LOADING = "article_loading"
    }

    object Favorites {
        const val ROOT = "wiki_favorites_root"
        const val LIST = "wiki_favorites_list"
        const val ITEM = "favorite_item"
        const val ITEM_TITLE = "favorite_title"
        const val ITEM_PREVIEW = "favorite_preview"
        const val EMPTY_STATE = "favorites_empty_state"
    }
}
