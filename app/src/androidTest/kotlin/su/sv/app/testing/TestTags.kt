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
        const val SEARCH_BUTTON = "books_search_button"
        const val SEARCH_CLEAR_BUTTON = "books_search_clear_button"
        const val SEARCH_CLOSE_BUTTON = "books_search_close_button"
        const val CATEGORY_FILTER = "category_filter"
        const val CATEGORY_CHIP = "category_chip"
        const val LOADING = "books_loading"
        const val ERROR = "books_error"
        const val BOOKMARKS_BUTTON = "books_bookmarks_button"
        const val DOWNLOADED_BUTTON = "books_downloaded_button"
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
    // Значения тегов должны совпадать с ReaderTestTags в модуле bookreader
    // (com.github.axet.bookreader.screens.testing.ReaderTestTags).
    object Reader {
        const val ROOT = "reader_root"
        const val CONTENT = "reader_content"
        const val TOP_BAR = "reader_top_bar"
        const val LOADING = "reader_loading"
        const val ERROR = "reader_error"

        // TopBar
        const val BACK_BUTTON = "reader_back_button"
        const val SEARCH_BUTTON = "reader_search_button"
        const val TOC_BUTTON = "reader_toc_button"
        const val BOOKMARKS_BUTTON = "reader_bookmarks_button"
        const val FONT_BUTTON = "reader_font_button"
        const val PAGE_INDICATOR = "reader_page_indicator"
        const val MENU_BUTTON = "reader_menu_button"
        const val MENU_DROPDOWN = "reader_menu_dropdown"

        // Search
        const val SEARCH_FIELD = "reader_search_field"
        const val SEARCH_CLOSE = "reader_search_close"

        // TOC
        const val TOC_DIALOG = "toc_dialog"
        const val TOC_LIST = "toc_list"
        const val TOC_ITEM = "toc_item"

        // Bookmarks
        const val BOOKMARK_DIALOG = "bookmarks_dialog"
        const val BOOKMARKS_LIST = "bookmarks_list"

        // Font settings
        const val FONT_SETTINGS_SHEET = "font_settings_sheet"

        // Selection
        const val SELECTION_PANEL = "selection_panel"
        const val SELECTION_COPY = "selection_copy_button"
        const val SELECTION_BOOKMARK = "selection_bookmark_button"
    }

    // ==================== Common ====================
    object Common {
        const val PROGRESS_INDICATOR = "progress_indicator"
        const val ERROR_MESSAGE = "error_message"
        const val RETRY_BUTTON = "retry_button"
        const val BACK_BUTTON = "back_button"
    }

    // ==================== Continue Reading Snackbar ====================
    object ContinueReading {
        const val ROOT = "continue_reading_root"
        const val COVER = "continue_reading_cover"
        const val TITLE = "continue_reading_title"
        const val AUTHOR = "continue_reading_author"
        const val CONTINUE_BUTTON = "continue_reading_button"
        const val DISMISS_BUTTON = "continue_reading_dismiss"
    }
}
