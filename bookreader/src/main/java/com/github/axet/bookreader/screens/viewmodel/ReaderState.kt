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
        val viewMode: ViewMode = ViewMode.PAGING,
        val isReflow: Boolean = false,
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
