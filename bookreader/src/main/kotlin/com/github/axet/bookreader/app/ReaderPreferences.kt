package com.github.axet.bookreader.app

/**
 * Константы для SharedPreferences настроек читалки
 */
object ReaderPreferences {
    const val PREFERENCE_FONTFAMILY_FBREADER: String = "fontfamily_fb"
    const val PREFERENCE_FONTSIZE_FBREADER: String = "fontsize_fb"
    const val PREFERENCE_FONTSIZE_REFLOW: String = "fontsize_reflow"
    const val PREFERENCE_FONTSIZE_REFLOW_DEFAULT: Float = 0.8f
    const val PREFERENCE_SCREENLOCK: String = "screen_lock"
    const val PREFERENCE_VOLUME_KEYS: String = "volume_keys"
    const val PREFERENCE_ROTATE: String = "rotate"
    const val PREFERENCE_VIEW_MODE: String = "view_mode"
    const val PREFERENCE_STORAGE: String = "storage_path"
    const val PREFERENCE_IGNORE_EMBEDDED_FONTS: String = "ignore_embedded_fonts"
    const val PREFERENCE_FONTS_FOLDER: String = "fonts_folder"

    /**
     * Двухстраничный режим (Two Column View)
     * null = авто (по размеру экрана), true = включить, false = выключить
     */
    const val PREFERENCE_TWO_COLUMN_VIEW: String = "two_column_view"

    // === Видимость кнопок в тулбаре читалки ===

    const val PREFERENCE_TOOLBAR_SEARCH: String = "toolbar_search"
    const val PREFERENCE_TOOLBAR_TOC: String = "toolbar_toc"
    const val PREFERENCE_TOOLBAR_BOOKMARKS: String = "toolbar_bookmarks"
    const val PREFERENCE_TOOLBAR_FONT: String = "toolbar_font"
    const val PREFERENCE_TOOLBAR_PAGE_INDICATOR: String = "toolbar_page_indicator"
}