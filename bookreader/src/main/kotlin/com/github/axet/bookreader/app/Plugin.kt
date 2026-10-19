package com.github.axet.bookreader.app

import org.geometerplus.zlibrary.core.view.ZLViewEnums

/**
 * Интерфейс плагина для чтения различных форматов книг.
 *
 * Вложенные классы ([Box], [RenderRect], [Page], [View]) оставлены как
 * backward-compatible врапперы над соответствующими top-level классами
 * ([PluginBox], [PluginRenderRect], [PluginPage], [PluginView]).
 * Новый код должен использовать top-level классы напрямую.
 */
interface Plugin {

    /**
     * Создаёт View для отображения книги.
     */
    fun create(book: Storage.FBook): View

    /**
     * Прямоугольник с координатами.
     *
     * Backward-compatible враппер над [PluginBox].
     * Новый код должен использовать [PluginBox] напрямую.
     */
    open class Box : PluginBox {
        constructor() : super()
        constructor(r: Box) : super(r)
        constructor(x: Int, y: Int, w: Int, h: Int) : super(x, y, w, h)
    }

    /**
     * Прямоугольник для рендеринга.
     *
     * Backward-compatible враппер над [PluginRenderRect].
     */
    class RenderRect : PluginRenderRect()

    /**
     * Абстрактный класс страницы.
     *
     * Backward-compatible враппер над [PluginPage].
     * Новый код должен использовать [PluginPage] напрямую.
     */
    abstract class Page : PluginPage {
        constructor() : super()
        constructor(r: Page) : super(r)
        constructor(r: Page, index: ZLViewEnums.PageIndex) : super(r, index)
    }

    /**
     * Базовый класс View для отображения книги.
     *
     * Backward-compatible враппер над [PluginView].
     * Новый код должен использовать [PluginView] напрямую.
     */
    open class View : PluginView()
}
