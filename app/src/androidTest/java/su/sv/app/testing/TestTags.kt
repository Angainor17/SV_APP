package su.sv.app.testing

/**
 * TestTags для UI элементов экранов.
 * Используются в модификаторах Modifier.testTag() и в тестах.
 */
object TestTags {

    // ==================== Bottom Navigation ====================
    object BottomNav {
        const val ROOT = "bottom_nav_root"
        const val TAB_NEWS = "tab_news"
        const val TAB_BOOKS = "tab_books"
        const val TAB_WIKI = "tab_wiki"
        const val TAB_INFO = "tab_info"
    }

    // ==================== News Module ====================
    object News {
        const val ROOT = "news_root"
        const val LIST = "news_list"
        const val ITEM = "news_item"
        const val ITEM_TITLE = "news_item_title"
        const val ITEM_IMAGE = "news_item_image"
        const val ITEM_VIDEO = "news_item_video"
        const val THEME_TOGGLE = "theme_toggle"
        const val LOADING = "news_loading"
        const val ERROR = "news_error"
    }

    // ==================== Books Module ====================
    object BooksCatalog {
        const val ROOT = "books_catalog_root"
        const val LIST = "books_list"
        const val ITEM = "book_item"
        const val ITEM_TITLE = "book_title"
        const val ITEM_AUTHOR = "book_author"
        const val ITEM_COVER = "book_cover"
        const val SEARCH_FIELD = "books_search_field"
        const val CATEGORY_FILTER = "category_filter"
        const val CATEGORY_CHIP = "category_chip"
        const val LOADING = "books_loading"
        const val ERROR = "books_error"
    }

    object BookDetail {
        const val ROOT = "book_detail_root"
        const val COVER = "book_detail_cover"
        const val TITLE = "book_detail_title"
        const val AUTHOR = "book_detail_author"
        const val DESCRIPTION = "book_detail_description"
        const val READ_BUTTON = "book_read_button"
        const val DOWNLOAD_PROGRESS = "book_download_progress"
        const val LOADING = "book_detail_loading"
    }

    object DownloadedBooks {
        const val ROOT = "downloaded_books_root"
        const val LIST = "downloaded_books_list"
        const val ITEM = "downloaded_book_item"
        const val ITEM_TITLE = "downloaded_book_title"
        const val DELETE_BUTTON = "delete_book_button"
        const val EMPTY_STATE = "downloaded_empty_state"
    }

    object Bookmarks {
        const val ROOT = "bookmarks_root"
        const val LIST = "bookmarks_list"
        const val ITEM = "bookmark_item"
        const val ITEM_TITLE = "bookmark_title"
        const val ITEM_PREVIEW = "bookmark_preview"
        const val MODE_TOGGLE = "bookmarks_mode_toggle"
        const val MODE_LIST = "mode_list"
        const val MODE_BY_BOOK = "mode_by_book"
        const val EMPTY_STATE = "bookmarks_empty_state"
    }

    // ==================== Wiki Module ====================
    object WikiRoot {
        const val ROOT = "wiki_root"
        const val SEARCH_FIELD = "wiki_search_field"
        const val SUGGESTIONS_LIST = "wiki_suggestions_list"
        const val SUGGESTION_ITEM = "wiki_suggestion_item"
        const val HISTORY_LIST = "wiki_history_list"
        const val HISTORY_ITEM = "wiki_history_item"
        const val FAVORITES_BUTTON = "wiki_favorites_button"
    }

    object WikiArticle {
        const val ROOT = "wiki_article_root"
        const val TITLE = "article_title"
        const val CONTENT = "article_content"
        const val FAVORITE_BUTTON = "article_favorite_button"
        const val LINK = "article_link"
        const val LOADING = "article_loading"
    }

    object WikiFavorites {
        const val ROOT = "wiki_favorites_root"
        const val LIST = "wiki_favorites_list"
        const val ITEM = "favorite_item"
        const val ITEM_TITLE = "favorite_title"
        const val ITEM_PREVIEW = "favorite_preview"
        const val EMPTY_STATE = "favorites_empty_state"
    }

    // ==================== Info Module ====================
    object Info {
        const val ROOT = "info_root"
        const val LINKS_LIST = "info_links_list"
        const val LINK_ITEM = "info_link_item"
        const val LINK_ICON = "link_icon"
        const val LINK_TITLE = "link_title"
        const val VERSION = "app_version"
    }

    // ==================== BookReader Module ====================
    object Reader {
        const val ROOT = "reader_root"
        const val CONTENT = "reader_content"
        const val TOP_BAR = "reader_top_bar"
        const val BOTTOM_BAR = "reader_bottom_bar"
        const val TITLE = "reader_book_title"
        const val CHAPTER_TITLE = "reader_chapter_title"

        // Settings
        const val SETTINGS_BUTTON = "reader_settings_button"
        const val FONT_SIZE_UP = "font_size_up"
        const val FONT_SIZE_DOWN = "font_size_down"
        const val THEME_LIGHT = "theme_light"
        const val THEME_DARK = "theme_dark"
        const val THEME_SEPIA = "theme_sepia"

        // Navigation
        const val TOC_BUTTON = "toc_button"
        const val TOC_LIST = "toc_list"
        const val TOC_ITEM = "toc_item"
        const val PAGE_PREV = "page_prev"
        const val PAGE_NEXT = "page_next"

        // Search
        const val SEARCH_BUTTON = "search_button"
        const val SEARCH_FIELD = "reader_search_field"
        const val SEARCH_RESULT = "search_result"
        const val SEARCH_CLOSE = "search_close"

        // Bookmarks
        const val BOOKMARK_BUTTON = "bookmark_button"
        const val BOOKMARK_DIALOG = "bookmark_dialog"
        const val BOOKMARK_TITLE_FIELD = "bookmark_title_field"
        const val BOOKMARK_SAVE_BUTTON = "bookmark_save_button"
        const val BOOKMARKS_LIST_BUTTON = "bookmarks_list_button"

        // Selection
        const val SELECTION_PANEL = "selection_panel"
        const val SELECTION_COPY = "selection_copy"
        const val SELECTION_BOOKMARK = "selection_bookmark"

        // Loading
        const val LOADING = "reader_loading"
        const val ERROR = "reader_error"
    }

    // ==================== Common ====================
    object Common {
        const val PROGRESS_INDICATOR = "progress_indicator"
        const val ERROR_MESSAGE = "error_message"
        const val RETRY_BUTTON = "retry_button"
        const val BACK_BUTTON = "back_button"
    }
}