package com.github.axet.bookreader.screens.viewmodel

import com.github.axet.bookreader.app.Storage

/**
 * Состояние экрана чтения книги
 */
sealed class ReaderState {
    /**
     * Загрузка книги
     */
    object Loading : ReaderState()

    /**
     * Контент книги загружен
     */
    data class Content(
        val book: Storage.Book,
        val positionText: String = "",
        val isFullscreen: Boolean = false,
        val showToc: Boolean = false,
        val showBookmarks: Boolean = false,
        val showFontSettings: Boolean = false,
        val showBookmarkEdit: Boolean = false,
        val editingBookmark: Storage.Bookmark? = null,
        val showNavigation: Boolean = false,
        val showSelection: Boolean = false,
        val selectionStartY: Int = 0,
        val selectionEndY: Int = 0,
        val viewMode: ViewMode = ViewMode.PAGING,
        val isReflow: Boolean = false,
        /** Можно ли менять шрифт (FB2/EPUB/MOBI или PDF в режиме reflow) */
        val canChangeFont: Boolean = true,
        /** Флаг: были ли показаны подсказки зон касания при открытии книги */
        val hasShownControlsHint: Boolean = false,
        /** Zoom scale (1.0 = normal, >1.0 = zoomed in) */
        val zoomScale: Float = 1.0f,
        /** Zoom pivot X (focus point for zoom) */
        val zoomPivotX: Float = 0f,
        /** Zoom pivot Y (focus point for zoom) */
        val zoomPivotY: Float = 0f,
        /** Is currently in zoom mode */
        val isInZoom: Boolean = false,
        /** Search state */
        val searchState: SearchState = SearchState(),
    ) : ReaderState()

    /**
     * Ошибка загрузки
     */
    data class Error(val message: String) : ReaderState()
}

/**
 * Режим просмотра книги
 */
enum class ViewMode {
    /** Постраничный просмотр */
    PAGING,
    /** Непрерывная прокрутка */
    CONTINUOUS
}
