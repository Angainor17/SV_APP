package com.github.axet.bookreader.screens.viewmodel

import android.net.Uri
import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.widgets.FBReaderView
import org.geometerplus.zlibrary.text.view.ZLTextPosition

/**
 * Действия на экране чтения книги
 */
sealed class ReaderActions {

    // === Загрузка и навигация ===

    /**
     * Загрузить книгу
     */
    data class LoadBook(
        val uri: Uri,
        val position: FBReaderView.ZLTextIndexPosition? = null
    ) : ReaderActions()

    /**
     * Сохранить текущую позицию
     */
    object SavePosition : ReaderActions()

    /**
     * Перейти к позиции
     */
    data class GoToPosition(val position: ZLTextPosition) : ReaderActions()

    /**
     * Вернуться назад
     */
    object NavigateBack : ReaderActions()

    /**
     * Перейти к настройкам
     */
    object NavigateToSettings : ReaderActions()

    // === Отображение ===

    /**
     * Переключить полноэкранный режим
     */
    object ToggleFullscreen : ReaderActions()

    /**
     * Переключить режим просмотра (постраничный/непрерывный)
     */
    object ToggleViewMode : ReaderActions()

    /**
     * Переключить Reflow режим (для PDF)
     */
    object ToggleReflow : ReaderActions()

    /**
     * Отметить что подсказки зон касания были показаны
     */
    object MarkControlsHintShown : ReaderActions()

    /**
     * Установить состояние fullscreen режима
     */
    data class SetFullscreen(val isFullscreen: Boolean) : ReaderActions()

    // === Диалоги ===

    /**
     * Переключить отображение содержания (TOC)
     */
    object ToggleToc : ReaderActions()

    /**
     * Переключить отображение закладок
     */
    object ToggleBookmarks : ReaderActions()

    /**
     * Переключить отображение настроек шрифта
     */
    object ToggleFontSettings : ReaderActions()

    /**
     * Скрыть все диалоги
     */
    object HideDialogs : ReaderActions()

    // === Закладки ===

    /**
     * Открыть редактирование закладки
     */
    data class EditBookmark(val bookmark: Storage.Bookmark) : ReaderActions()

    /**
     * Сохранить изменения закладки
     */
    data class SaveBookmarkEdit(val bookmark: Storage.Bookmark, val name: String, val color: Int) : ReaderActions()

    /**
     * Добавить закладку
     */
    data class AddBookmark(val bookmark: Storage.Bookmark) : ReaderActions()

    /**
     * Удалить закладку
     */
    data class DeleteBookmark(val bookmark: Storage.Bookmark) : ReaderActions()

    /**
     * Перейти к закладке
     */
    data class GoToBookmark(val bookmark: Storage.Bookmark) : ReaderActions()

    // === Шрифты ===

    /**
     * Установить размер шрифта (для FBReader)
     */
    data class SetFontSize(val size: Int) : ReaderActions()

    /**
     * Установить размер шрифта (для Reflow)
     */
    data class SetReflowFontSize(val size: Float) : ReaderActions()

    /**
     * Установить семейство шрифта
     */
    data class SetFontFamily(val family: String) : ReaderActions()

    /**
     * Игнорировать встроенные шрифты
     */
    data class SetIgnoreEmbeddedFonts(val ignore: Boolean) : ReaderActions()

    // === Поиск ===

    /**
     * Поиск по тексту
     */
    data class Search(val query: String) : ReaderActions()

    /**
     * Найти следующее вхождение
     */
    object SearchNext : ReaderActions()

    /**
     * Найти предыдущее вхождение
     */
    object SearchPrevious : ReaderActions()

    /**
     * Закрыть поиск
     */
    object SearchClose : ReaderActions()
}
