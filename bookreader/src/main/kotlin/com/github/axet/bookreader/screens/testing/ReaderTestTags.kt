package com.github.axet.bookreader.screens.testing

/**
 * TestTags для экрана чтения книги (ReaderScreen).
 *
 * Используются в Modifier.testTag() для UI тестирования.
 */
object ReaderTestTags {

    // ==================== Root ====================
    const val ROOT = "reader_root"
    const val CONTENT = "reader_content"
    const val LOADING = "reader_loading"
    const val ERROR = "reader_error"

    // ==================== TopBar ====================
    object TopBar {
        const val ROOT = "reader_top_bar"
        const val BACK_BUTTON = "reader_back_button"
        const val SEARCH_BUTTON = "reader_search_button"
        const val TOC_BUTTON = "reader_toc_button"
        const val BOOKMARKS_BUTTON = "reader_bookmarks_button"
        const val FONT_BUTTON = "reader_font_button"
        const val MENU_BUTTON = "reader_menu_button"

        // Меню
        const val MENU_DROPDOWN = "reader_menu_dropdown"
        const val MENU_VIEW_MODE = "menu_view_mode"
        const val MENU_REFLOW = "menu_reflow"
        const val MENU_SETTINGS = "menu_settings"

        // Поиск
        const val SEARCH_PANEL = "reader_search_panel"
        const val SEARCH_FIELD = "reader_search_field"
        const val SEARCH_CLOSE = "reader_search_close"
        const val SEARCH_PREV = "reader_search_prev"
        const val SEARCH_NEXT = "reader_search_next"
        const val SEARCH_COUNTER = "reader_search_counter"
        const val SEARCH_LOADING = "reader_search_loading"
    }

    // ==================== Selection Panel ====================
    object Selection {
        const val PANEL = "selection_panel"
        const val BOOKMARK_BUTTON = "selection_bookmark_button"
        const val SHARE_BUTTON = "selection_share_button"
        const val COPY_BUTTON = "selection_copy_button"
        const val QUESTION_BUTTON = "selection_question_button"
        const val ALERT_BUTTON = "selection_alert_button"
        const val CLOSE_BUTTON = "selection_close_button"
        const val TEXT_PREVIEW = "selection_text_preview"
    }

    // ==================== Bookmarks ====================
    object Bookmarks {
        const val DIALOG = "bookmarks_dialog"
        const val LIST = "bookmarks_list"
        const val ITEM = "bookmark_item"
        const val ITEM_PAGE = "bookmark_item_page"
        const val ITEM_TEXT = "bookmark_item_text"
        const val ITEM_NAME = "bookmark_item_name"
        const val ITEM_DELETE = "bookmark_item_delete"
        const val EMPTY_STATE = "bookmarks_empty_state"

        // Редактирование
        const val EDIT_SHEET = "bookmark_edit_sheet"
        const val EDIT_TEXT = "bookmark_edit_text"
        const val EDIT_NAME_FIELD = "bookmark_edit_name"
        const val EDIT_COLOR_PICKER = "bookmark_edit_colors"
        const val EDIT_COLOR_ITEM = "bookmark_edit_color"
        const val EDIT_SAVE = "bookmark_edit_save"
        const val EDIT_CANCEL = "bookmark_edit_cancel"
    }

    // ==================== TOC (Оглавление) ====================
    object Toc {
        const val DIALOG = "toc_dialog"
        const val LIST = "toc_list"
        const val ITEM = "toc_item"
        const val ITEM_TITLE = "toc_item_title"
        const val ITEM_EXPAND = "toc_item_expand"
        const val EMPTY_STATE = "toc_empty_state"
    }

    // ==================== Navigation ====================
    object Navigation {
        const val DIALOG = "navigation_dialog"
        const val SLIDER = "navigation_slider"
        const val PAGE_INFO = "navigation_page_info"
        const val CHAPTER_INFO = "navigation_chapter_info"
        const val CANCEL_BUTTON = "navigation_cancel"
        const val OK_BUTTON = "navigation_ok"
    }

    // ==================== Font Settings ====================
    object FontSettings {
        const val SHEET = "font_settings_sheet"
        const val SIZE_SLIDER = "font_size_slider"
        const val SIZE_VALUE = "font_size_value"
        const val FONT_LIST = "font_family_list"
        const val FONT_ITEM = "font_family_item"
        const val IGNORE_EMBEDDED = "font_ignore_embedded"
    }

    // ==================== TTS ====================
    object TTS {
        const val PANEL = "tts_panel"
        const val PLAY_BUTTON = "tts_play"
        const val PAUSE_BUTTON = "tts_pause"
        const val PREV_BUTTON = "tts_prev"
        const val NEXT_BUTTON = "tts_next"
        const val CLOSE_BUTTON = "tts_close"
    }

    // ==================== Fullscreen ====================
    object Fullscreen {
        const val OVERLAY = "fullscreen_overlay"
        const val SHOW_BARS = "fullscreen_show_bars"
    }

    // ==================== Image Popup ====================
    object ImagePopup {
        const val DIALOG = "image_popup_dialog"
        const val IMAGE = "image_popup_image"
        const val SHARE_BUTTON = "image_popup_share"
        const val ZOOM_BUTTON = "image_popup_zoom"
        const val CLOSE_BUTTON = "image_popup_close"
    }

    // ==================== Errors ====================
    object Error {
        const val DIALOG = "error_dialog"
        const val MESSAGE = "error_message"
        const val RETRY_BUTTON = "error_retry"
        const val CLOSE_BUTTON = "error_close"
    }
}