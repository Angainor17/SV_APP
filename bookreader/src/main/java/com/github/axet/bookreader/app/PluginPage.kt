package com.github.axet.bookreader.app

import com.github.axet.bookreader.widgets.FBReaderView
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import timber.log.Timber

/**
 * Абстрактный класс страницы.
 *
 * Вынесен из [Plugin.Page] для уменьшения размера Plugin.kt.
 * [Plugin.Page] оставлен как backward-compatible враппер.
 */
abstract class PluginPage {
    @JvmField var pageNumber: Int = 0
    @JvmField var pageOffset: Int = 0 // размеры pageBox
    @JvmField var pageBox: PluginBox? = null // размеры pageBox
    @JvmField var w: Int = 0 // ширина отображения
    @JvmField var h: Int = 0 // высота отображения
    @JvmField var hh: Double = 0.0 // размеры pageBox, видимая высота
    @JvmField var ratio: Double = 0.0
    @JvmField var pageStep: Int = 0 // размеры pageBox, размер шага страницы
    @JvmField var pageOverlap: Int = 0 // размеры pageBox, размер перекрытия страницы
    @JvmField var dpi: Int = 0 // dpi pageBox, задаётся вручную

    constructor()

    constructor(r: PluginPage) {
        w = r.w
        h = r.h
        hh = r.hh
        ratio = r.ratio
        pageNumber = r.pageNumber
        pageOffset = r.pageOffset
        if (r.pageBox != null)
            pageBox = PluginBox(r.pageBox!!)
        pageStep = r.pageStep
        pageOverlap = r.pageOverlap
    }

    constructor(r: PluginPage, index: ZLViewEnums.PageIndex) : this(r) {
        load(index)
    }

    /**
     * Рендерит страницу.
     */
    fun renderPage() {
        ratio = pageBox!!.w / w.toDouble()
        hh = h * ratio

        pageOverlap = (hh * FBReaderView.PAGE_OVERLAP_PERCENTS / 100).toInt()
        pageStep = (hh - pageOverlap).toInt() // -5% или нижняя базовая линия
    }

    /**
     * Загружает страницу по индексу.
     */
    fun load(index: ZLViewEnums.PageIndex) {
        when (index) {
            ZLViewEnums.PageIndex.next -> next()
            ZLViewEnums.PageIndex.previous -> prev()
            else -> {}
        }
    }

    /**
     * Загружает страницу.
     */
    abstract fun load()

    /**
     * Возвращает количество страниц.
     */
    abstract fun getPagesCount(): Int

    /**
     * Переходит к следующей странице.
     */
    fun next(): Boolean {
        var pageOffset = this.pageOffset + pageStep
        val tail = pageBox!!.h - pageOffset
        if (pageOffset >= pageBox!!.h || tail <= pageOverlap) {
            var pageNumber = this.pageNumber + 1
            if (pageNumber >= getPagesCount())
                return false
            this.pageOffset = 0
            this.pageNumber = pageNumber
            load()
            renderPage()
            return true
        }
        this.pageOffset = pageOffset
        return true
    }

    /**
     * Переходит к предыдущей странице.
     */
    fun prev(): Boolean {
        var pageOffset = this.pageOffset - pageStep
        if (this.pageOffset > 0 && pageOffset < 0) { // происходит только при повороте экрана
            this.pageOffset = pageOffset // синхронизация с верхом = 0 или сохранение отрицательного смещения
            return true
        } else if (pageOffset < 0) {
            var pageNumber = this.pageNumber - 1
            if (pageNumber < 0)
                return false
            this.pageNumber = pageNumber
            load() // загрузка pageBox
            renderPage() // вычисление pageStep
            val tail = pageBox!!.h % pageStep
            pageOffset = pageBox!!.h - tail
            if (tail <= pageOverlap)
                pageOffset = pageOffset - pageStep // пропуск хвоста
            this.pageOffset = pageOffset
            return true
        }
        this.pageOffset = pageOffset
        return true
    }

    /**
     * Масштабирует страницу.
     */
    fun scale(w: Int, h: Int) {
        val ratio = w / pageBox!!.w.toDouble()
        Timber.tag("voronin").d("PluginPage.scale() w=$w h=$h oldRatio=${this.ratio} newRatio=$ratio pageBox=${pageBox!!.w}x${pageBox!!.h} hh=$hh")
        this.hh *= ratio
        this.ratio *= ratio
        pageBox!!.w = w
        pageBox!!.h = (pageBox!!.h * ratio).toInt()
        pageOffset = (pageOffset * ratio).toInt()
        dpi = (dpi * ratio).toInt()
        Timber.tag("voronin").d("PluginPage.scale() result: pageBox=${pageBox!!.w}x${pageBox!!.h} hh=$hh ratio=${this.ratio}")
    }

    /**
     * Возвращает прямоугольник рендеринга.
     */
    fun renderRect(): PluginRenderRect {
        val render = PluginRenderRect() // область рендеринга

        render.x = 0
        render.w = pageBox!!.w

        Timber.tag("voronin").d("PluginPage.renderRect() pageOffset=$pageOffset hh=$hh pageBox=${pageBox!!.w}x${pageBox!!.h} ratio=$ratio w=$w h=$h")

        if (pageOffset < 0) { // показываем пустое пространство в начале
            val tail = (pageBox!!.h - pageOffset - hh).toInt() // хвост для обрезки снизу
            if (tail < 0) {
                render.h = pageBox!!.h
                render.y = 0
            } else {
                render.h = pageBox!!.h - tail
                render.y = tail
            }
            render.dst = android.graphics.Rect(0, (-pageOffset / ratio).toInt(), w, h)
            Timber.tag("voronin").d("PluginPage.renderRect() case1: pageOffset<0 dst=${render.dst}")
        } else if (pageOffset == 0 && hh > pageBox!!.h) {  // показываем по центру по вертикали
            val t = ((hh - pageBox!!.h) / ratio / 2).toInt()
            render.h = pageBox!!.h
            render.dst = android.graphics.Rect(0, t, w, h - t)
            Timber.tag("voronin").d("PluginPage.renderRect() case2: centered t=$t dst=${render.dst}")
        } else {
            render.h = hh.toInt()
            render.y = pageBox!!.h - render.h - pageOffset - 1
            if (render.y < 0) {
                render.h += render.y
                h += (render.y / ratio).toInt() // конвертация в размеры отображения
                render.y = 0
            }
            render.dst = android.graphics.Rect(0, 0, w, h)
            Timber.tag("voronin").d("PluginPage.renderRect() case3: normal dst=${render.dst}")
        }

        render.src = android.graphics.Rect(0, 0, render.w, render.h)

        return render
    }

    /**
     * Сравнивает страницу с указанными параметрами.
     */
    fun equals(n: Int, o: Int): Boolean {
        return pageNumber == n && pageOffset == o
    }

    /**
     * Загружает страницу по позиции.
     */
    fun load(p: ZLTextPosition?) {
        if (p == null) {
            load(0, 0)
        } else {
            load(p.paragraphIndex, p.elementIndex)
        }
    }

    /**
     * Загружает страницу по номеру и смещению.
     */
    fun load(n: Int, o: Int) {
        pageNumber = n
        pageOffset = o
        load()
    }

    /**
     * Обновляет параметры страницы.
     */
    fun updatePage(r: PluginPage) {
        w = r.w
        h = r.h
        ratio = r.ratio
        hh = r.hh
        pageStep = r.pageStep
        pageOverlap = r.pageOverlap
        if (r.pageBox != null) {
            pageBox = PluginBox(r.pageBox!!)
        }
    }
}
