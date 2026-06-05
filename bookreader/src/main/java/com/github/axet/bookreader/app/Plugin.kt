package com.github.axet.bookreader.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.core.graphics.ColorUtils
import com.github.axet.bookreader.widgets.FBReaderView
import com.github.axet.bookreader.widgets.ScrollWidget
import com.github.axet.bookreader.widgets.SelectionView
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow
import org.geometerplus.fbreader.bookmodel.TOCTree
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import org.geometerplus.zlibrary.text.view.ZLTextView
import java.io.IOException

/**
 * Интерфейс плагина для чтения различных форматов книг.
 */
interface Plugin {

    /**
     * Создаёт View для отображения книги.
     */
    fun create(book: Storage.FBook): View

    /**
     * Прямоугольник с координатами.
     */
    open class Box {
        @JvmField var x: Int = 0 // нижний левый x
        @JvmField var y: Int = 0 // нижний левый y
        @JvmField var w: Int = 0 // x + w = верхний правый x
        @JvmField var h: Int = 0 // y + h = верхний правый y

        constructor()

        constructor(r: Box) {
            this.x = r.x
            this.y = r.y
            this.w = r.w
            this.h = r.h
        }

        constructor(x: Int, y: Int, w: Int, h: Int) {
            this.x = x
            this.y = y
            this.w = w
            this.h = h
        }

        /**
         * Преобразует в Rect.
         */
        fun toRect(w: Int, h: Int): Rect {
            return Rect(x, h - this.h - y, x + this.w, h - y)
        }
    }

    /**
     * Прямоугольник для рендеринга.
     */
    class RenderRect : Box() {
        @JvmField var src: Rect? = null
        @JvmField var dst: Rect? = null
    }

    /**
     * Абстрактный класс страницы.
     */
    abstract class Page {
        @JvmField var pageNumber: Int = 0
        @JvmField var pageOffset: Int = 0 // размеры pageBox
        @JvmField var pageBox: Box? = null // размеры pageBox
        @JvmField var w: Int = 0 // ширина отображения
        @JvmField var h: Int = 0 // высота отображения
        @JvmField var hh: Double = 0.0 // размеры pageBox, видимая высота
        @JvmField var ratio: Double = 0.0
        @JvmField var pageStep: Int = 0 // размеры pageBox, размер шага страницы
        @JvmField var pageOverlap: Int = 0 // размеры pageBox, размер перекрытия страницы
        @JvmField var dpi: Int = 0 // dpi pageBox, задаётся вручную

        constructor()

        constructor(r: Page) {
            w = r.w
            h = r.h
            hh = r.hh
            ratio = r.ratio
            pageNumber = r.pageNumber
            pageOffset = r.pageOffset
            if (r.pageBox != null)
                pageBox = Box(r.pageBox!!)
            pageStep = r.pageStep
            pageOverlap = r.pageOverlap
        }

        constructor(r: Page, index: ZLViewEnums.PageIndex) : this(r) {
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
            this.hh *= ratio
            this.ratio *= ratio
            pageBox!!.w = w
            pageBox!!.h = (pageBox!!.h * ratio).toInt()
            pageOffset = (pageOffset * ratio).toInt()
            dpi = (dpi * ratio).toInt()
        }

        /**
         * Возвращает прямоугольник рендеринга.
         */
        fun renderRect(): RenderRect {
            val render = RenderRect() // область рендеринга

            render.x = 0
            render.w = pageBox!!.w

            if (pageOffset < 0) { // показываем пустое пространство в начале
                val tail = (pageBox!!.h - pageOffset - hh).toInt() // хвост для обрезки снизу
                if (tail < 0) {
                    render.h = pageBox!!.h
                    render.y = 0
                } else {
                    render.h = pageBox!!.h - tail
                    render.y = tail
                }
                render.dst = Rect(0, (-pageOffset / ratio).toInt(), w, h)
            } else if (pageOffset == 0 && hh > pageBox!!.h) {  // показываем по центру по вертикали
                val t = ((hh - pageBox!!.h) / ratio / 2).toInt()
                render.h = pageBox!!.h
                render.dst = Rect(0, t, w, h - t)
            } else {
                render.h = hh.toInt()
                render.y = pageBox!!.h - render.h - pageOffset - 1
                if (render.y < 0) {
                    render.h += render.y
                    h += (render.y / ratio).toInt() // конвертация в размеры отображения
                    render.y = 0
                }
                render.dst = Rect(0, 0, w, h)
            }

            render.src = Rect(0, 0, render.w, render.h)

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
                load(p.getParagraphIndex(), p.elementIndex)
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
        fun updatePage(r: Page) {
            w = r.w
            h = r.h
            ratio = r.ratio
            hh = r.hh
            pageStep = r.pageStep
            pageOverlap = r.pageOverlap
        }
    }

    /**
     * Базовый класс View для отображения книги.
     */
    open class View {

        companion object {
            val TAG: String = View::class.java.simpleName

            const val RENDER_MIN = 512 // минимальная ширина экрана

            @JvmField val NEGATIVE = floatArrayOf(
                -1.0f, 0f, 0f, 0f, 255f, // красный
                0f, -1.0f, 0f, 0f, 255f, // зелёный
                0f, 0f, -1.0f, 0f, 255f, // синий
                0f, 0f, 0f, 1.0f, 0f     // альфа
            )

            /**
             * Вычисляет нечётный индекс.
             */
            @JvmStatic
            fun odd(i: Int): Int {
                return ((i + 1) / 2) * ((i + 1) % 2 - i % 2)
            }

            /**
             * Вычисляет нечётную страницу.
             */
            @JvmStatic
            fun odd(page: Int, i: Int, max: Int): Int {
                var p = page + odd(i)
                if (page <= i / 2)
                    p = i
                if (page + i / 2 >= max)
                    p = max - i - 1
                return p
            }
        }

        @JvmField var wallpaper: Bitmap? = null
        @JvmField var wallpaperColor: Int = 0
        @JvmField var paint: Paint = Paint() // цвет переднего плана / содержимого
        @JvmField var current: Page? = null
        @JvmField var reflow: Boolean = false
        @JvmField var reflowDebug: Boolean = false
        @JvmField var reflower: Reflow? = null

        init {
            updateTheme()
        }

        /**
         * Обновляет тему.
         */
        open fun updateTheme() {
            try {
                val app = FBReaderApp(null, BookCollectionShadow())
                val wallpaper = app.BookTextView.getWallpaperFile()
                if (wallpaper != null)
                    this.wallpaper = BitmapFactory.decodeStream(wallpaper.inputStream)
                else
                    this.wallpaper = null
                wallpaperColor = (0xff shl 24) or app.BookTextView.getBackgroundColor().intValue()
                if (ColorUtils.calculateLuminance(wallpaperColor) < 0.5f && this !is ComicsPlugin.ComicsView)
                    paint.colorFilter = ColorMatrixColorFilter(NEGATIVE)
                else
                    paint.colorFilter = null
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        /**
         * Рисует обои на canvas.
         */
        open fun drawWallpaper(canvas: Canvas) {
            if (wallpaper != null) {
                val dx = wallpaper!!.width.toFloat()
                val dy = wallpaper!!.height.toFloat()
                var cw = 0
                while (cw < canvas.width + dx) {
                    var ch = 0
                    while (ch < canvas.height + dy) {
                        canvas.drawBitmap(wallpaper!!, cw - dx, ch - dy, paint)
                        ch += dy.toInt()
                    }
                    cw += dx.toInt()
                }
            } else {
                canvas.drawColor(wallpaperColor)
            }
        }

        /**
         * Переходит к указанной позиции.
         */
        open fun gotoPosition(p: ZLTextPosition?) {
            if (p == null)
                return
            if (current!!.pageNumber != p.getParagraphIndex() || current!!.pageOffset != p.elementIndex)
                current!!.load(p)
            if (reflower != null) {
                if (reflower!!.page != p.getParagraphIndex()) {
                    reflower!!.reset()
                    reflower!!.page = current!!.pageNumber
                }
                reflower!!.index = p.elementIndex
            }
        }

        /**
         * Обрабатывает завершение прокрутки.
         */
        open fun onScrollingFinished(index: ZLViewEnums.PageIndex): Boolean {
            if (reflow && reflowDebug) {
                when (index) {
                    ZLViewEnums.PageIndex.previous -> {
                        current!!.pageNumber--
                        current!!.pageOffset = 0
                        current!!.load()
                    }
                    ZLViewEnums.PageIndex.next -> {
                        current!!.pageNumber++
                        current!!.pageOffset = 0
                        current!!.load()
                    }
                    else -> {}
                }
                return false
            }
            if (reflower != null) {
                reflower!!.onScrollingFinished(index)
                Log.d(TAG, "Reflow position: ${reflower!!.page}.${reflower!!.index}")
                if (index == ZLViewEnums.PageIndex.current)
                    return false
                if (reflower!!.page != current!!.pageNumber) {
                    current!!.pageNumber = reflower!!.page
                    current!!.pageOffset = 0
                    current!!.load()
                    return false
                }
                if (reflower!!.index == -1) {
                    current!!.pageNumber = reflower!!.page - 1
                    current!!.pageOffset = 0
                    current!!.load()
                    return false
                }
                if (reflower!!.index >= reflower!!.emptyCount()) { // текущая указывает на следующую страницу +1
                    current!!.pageNumber = reflower!!.page + 1
                    current!!.pageOffset = 0
                    current!!.load()
                    return false
                }
                return false
            }
            val old = object : Page(current!!) {
                override fun load() {}
                override fun getPagesCount(): Int = current!!.getPagesCount()
            }
            current!!.load(index)
            val r: Page = when (index) {
                ZLViewEnums.PageIndex.previous -> object : Page(current!!, ZLViewEnums.PageIndex.next) {
                    override fun load() {}
                    override fun getPagesCount(): Int = current!!.getPagesCount()
                }
                ZLViewEnums.PageIndex.next -> object : Page(current!!, ZLViewEnums.PageIndex.previous) {
                    override fun load() {}
                    override fun getPagesCount(): Int = current!!.getPagesCount()
                }
                else -> return false
            }
            return !old.equals(r.pageNumber, r.pageOffset) // нужен сброс кэша?
        }

        /**
         * Возвращает текущую позицию.
         */
        open fun getPosition(): ZLTextPosition {
            return ZLTextFixedPosition(current!!.pageNumber, current!!.pageOffset, 0)
        }

        /**
         * Возвращает следующую позицию.
         */
        open fun getNextPosition(): ZLTextPosition? {
            if (current!!.w == 0 || current!!.h == 0)
                return null // после reset() мы не знаем размер экрана
            val next = object : Page(current!!, ZLViewEnums.PageIndex.next) {
                override fun load() {}
                override fun getPagesCount(): Int = current!!.getPagesCount()
            }
            if (current!!.equals(next.pageNumber, next.pageOffset))
                return null // !canScroll()
            val e = ZLTextFixedPosition(next.pageNumber, next.pageOffset, 0)
            if (e.getParagraphIndex() >= next.getPagesCount())
                return null
            return e
        }

        /**
         * Проверяет возможность прокрутки.
         */
        open fun canScroll(index: ZLViewEnums.PageIndex): Boolean {
            if (reflower != null) {
                if (reflower!!.canScroll(index))
                    return true
                when (index) {
                    ZLViewEnums.PageIndex.previous -> {
                        if (current!!.pageNumber > 0)
                            return true
                        if (current!!.pageNumber != reflower!!.page) { // происходит только на 0-й странице документа
                            val render = reflower!!.index
                            val bm = render(reflower!!.rw, reflower!!.h, current!!.pageNumber)!! // 0-я страница
                            reflower!!.load(bm, current!!.pageNumber, 0)
                            bm.recycle()
                            var count = reflower!!.count()
                            count += render
                            reflower!!.index = count
                            return count > 0
                        }
                        return false
                    }
                    ZLViewEnums.PageIndex.next -> {
                        if (current!!.pageNumber + 1 < current!!.getPagesCount())
                            return true
                        if (current!!.pageNumber != reflower!!.page) { // происходит только на последней странице документа
                            val render = reflower!!.index - reflower!!.count()
                            val bm = render(reflower!!.rw, reflower!!.h, current!!.pageNumber)!! // последняя страница
                            reflower!!.load(bm, current!!.pageNumber, 0)
                            bm.recycle()
                            reflower!!.index = render
                            return render + 1 < reflower!!.count()
                        }
                        return false
                    }
                    else -> return true // current???
                }
            }
            val r = object : Page(current!!, index) {
                override fun load() {}
                override fun getPagesCount(): Int = current!!.getPagesCount()
            }
            return !r.equals(current!!.pageNumber, current!!.pageOffset)
        }

        /**
         * Возвращает позицию страницы.
         */
        open fun pagePosition(): ZLTextView.PagePosition {
            return ZLTextView.PagePosition(current!!.pageNumber + 1, current!!.getPagesCount())
        }

        /**
         * Рендерит страницу в bitmap.
         */
        open fun render(w: Int, h: Int, page: Int, c: Bitmap.Config): Bitmap? {
            return null
        }

        /**
         * Рендерит страницу в bitmap.
         */
        open fun render(w: Int, h: Int, page: Int): Bitmap? {
            var w = w
            var h = h
            if (w < RENDER_MIN) {
                val ratio = RENDER_MIN / w.toFloat()
                w = (w * ratio).toInt()
                h = (h * ratio).toInt()
            }
            return render(w, h, page, Bitmap.Config.RGB_565) // активен reflower, всегда 565
        }

        /**
         * Рисует на bitmap.
         */
        open fun drawOnBitmap(context: Context, bitmap: Bitmap, w: Int, h: Int, index: ZLViewEnums.PageIndex, custom: FBReaderView.CustomView, info: Storage.RecentInfo) {
            val canvas = Canvas(bitmap)
            drawOnCanvas(context, canvas, w, h, index, custom, info)
        }

        /**
         * Возвращает информацию о странице.
         */
        open fun getPageInfo(w: Int, h: Int, c: ScrollWidget.ScrollAdapter.PageCursor): Page? {
            return null
        }

        /**
         * Возвращает высоту страницы.
         */
        open fun getPageHeight(w: Int, c: ScrollWidget.ScrollAdapter.PageCursor): Double {
            val r = getPageInfo(w, 0, c)!!
            return r.pageBox!!.h / r.ratio
        }

        /**
         * Рисует на canvas.
         */
        open fun drawOnCanvas(context: Context, canvas: Canvas, w: Int, h: Int, index: ZLViewEnums.PageIndex, custom: FBReaderView.CustomView, info: Storage.RecentInfo) {
            var index = index
            if (reflow) {
                if (reflower == null)
                    reflower = Reflow(context, w, h, current!!.pageNumber, custom, info)
                var bm: Bitmap? = null
                reflower!!.reset(w, h)
                var render = reflower!!.index // индекс страницы reflow для рендеринга
                var page = reflower!!.page // номер страницы для рендеринга
                if (reflowDebug) {
                    when (index) {
                        ZLViewEnums.PageIndex.previous -> page = current!!.pageNumber - 1
                        ZLViewEnums.PageIndex.next -> page = current!!.pageNumber + 1
                        else -> {}
                    }
                    index = ZLViewEnums.PageIndex.current
                    render = 0
                }
                when (index) {
                    ZLViewEnums.PageIndex.previous -> { // предыдущая может указывать на несколько страниц назад
                        if (reflower!!.count() == -1 && render > 0) { // прогулка по сброшенному reflower, перезагрузка
                            bm = render(reflower!!.rw, reflower!!.h, page)
                            reflower!!.load(bm!!)
                        }
                        render -= 1
                        while (render < 0) {
                            page--
                            bm?.recycle()
                            bm = render(reflower!!.rw, reflower!!.h, page)
                            reflower!!.load(bm!!)
                            render = render + reflower!!.emptyCount()
                            reflower!!.page = page
                            reflower!!.index = render + 1 // onScrollingFinished - 1
                        }
                        if (reflower!!.count() > render) {
                            bm?.recycle()
                            bm = reflower!!.render(render)
                        }
                        reflower!!.pending = -1
                    }
                    ZLViewEnums.PageIndex.current -> {
                        if (reflower!!.count() > 0) {
                            bm = reflower!!.render(render)
                        } else {
                            bm = render(reflower!!.rw, reflower!!.h, page)
                            if (reflowDebug) {
                                reflower!!.k2!!.verbose = true
                                reflower!!.k2!!.showMarkedSource = true
                            }
                            reflower!!.load(bm!!, page, render)
                            if (reflowDebug) {
                                reflower!!.bm = null // не утилизировать
                                reflower!!.close()
                                reflower = null
                            } else {
                                if (reflower!!.count() > render) { // пустая исходная страница
                                    bm.recycle()
                                    bm = reflower!!.render(render)
                                }
                            }
                        }
                    }
                    ZLViewEnums.PageIndex.next -> { // следующая может указывать на несколько страниц вперёд
                        if (reflower!!.count() == -1) { // прогулка по сброшенному reflower, перезагрузка
                            bm = render(reflower!!.rw, reflower!!.h, page)
                            reflower!!.load(bm!!)
                        }
                        render += 1
                        while (reflower!!.emptyCount() - render <= 0) {
                            page++
                            render -= reflower!!.emptyCount()
                            bm?.recycle()
                            bm = render(reflower!!.rw, reflower!!.h, page)
                            reflower!!.load(bm!!, page, render - 1) // onScrollingFinished + 1
                        }
                        if (reflower!!.count() > render) {
                            bm?.recycle()
                            bm = reflower!!.render(render)
                        }
                        reflower!!.pending = 1
                    }
                    else -> {}
                }
                if (bm != null) {
                    if (reflower == null || reflower!!.bm === bm)
                        drawWallpaper(canvas) // собираемся рисовать исходную страницу, подготавливаем фон
                    else
                        canvas.drawColor(wallpaperColor) // подготавливаем белый
                    drawPage(canvas, w, h, bm)
                    bm.recycle()
                    return
                }
            }
            if (reflower != null) {
                reflower!!.close()
                reflower = null
            }
            drawWallpaper(canvas)
            draw(canvas, w, h, index)
        }

        /**
         * Рисует на bitmap.
         */
        open fun draw(bitmap: Canvas, w: Int, h: Int, index: ZLViewEnums.PageIndex, c: Bitmap.Config) {}

        /**
         * Рисует на bitmap.
         */
        open fun draw(bitmap: Canvas, w: Int, h: Int, index: ZLViewEnums.PageIndex) {
            draw(bitmap, w, h, index, Bitmap.Config.RGB_565)
        }

        /**
         * Рисует страницу на canvas.
         */
        open fun drawPage(canvas: Canvas, w: Int, h: Int, bm: Bitmap) {
            val src = Rect(0, 0, bm.width, bm.height)
            val wr = w / bm.width.toFloat()
            val hr = h / bm.height.toFloat()
            val dh = (bm.height * wr).toInt()
            val dw = (bm.width * hr).toInt()
            val dst: Rect
            if (dh > h) { // масштабирование по ширине делает слишком высоким
                val mid = (w - dw) / 2
                dst = Rect(mid, 0, dw + mid, h) // масштабируем по высоте и берём вычисленную ширину
            } else { // берём ширину
                val mid = (h - dh) / 2
                dst = Rect(0, mid, w, dh + mid) // масштабируем по ширине и берём вычисленную высоту
            }
            canvas.drawBitmap(bm, src, dst, paint)
        }

        /**
         * Закрывает View.
         */
        open fun close() {}

        /**
         * Возвращает текущий элемент оглавления.
         */
        open fun getCurrentTOCElement(tocTree: TOCTree): TOCTree? {
            var treeToSelect: TOCTree? = null
            @Suppress("UNCHECKED_CAST")
            val iterator = (tocTree as java.lang.Iterable<TOCTree>).iterator()
            while (iterator.hasNext()) {
                val tree = iterator.next()
                val reference = tree.reference
                if (reference == null)
                    continue
                if (reference.ParagraphIndex > current!!.pageNumber)
                    break
                treeToSelect = tree
            }
            return treeToSelect
        }

        /**
         * Создаёт выделение на странице.
         */
        open fun select(page: Int): Selection? {
            return null
        }

        /**
         * Создаёт выделение на странице.
         */
        open fun select(p: Selection.Page, point: Selection.Point): Selection? {
            return null
        }

        /**
         * Создаёт выделение между позициями.
         */
        open fun select(start: ZLTextPosition, end: ZLTextPosition): Selection? {
            return null
        }

        /**
         * Возвращает страницу выделения.
         */
        open fun selectPage(start: ZLTextPosition, info: Reflow.Info?, w: Int, h: Int): Selection.Page {
            return if (reflow && info != null)
                Selection.Page(start.getParagraphIndex(), info.bm.width(), info.bm.height())
            else
                Selection.Page(start.getParagraphIndex(), w, h)
        }

        /**
         * Возвращает прямоугольник выделения.
         */
        open fun selectRect(info: Reflow.Info, x: Int, y: Int): Rect? {
            var x = x - info.margin.left
            val dst = info.dst
            for (d in dst.keys) {
                if (d.contains(x, y))
                    return dst[d]
            }
            return null
        }

        /**
         * Возвращает точку выделения.
         */
        open fun selectPoint(info: Reflow.Info?, x: Int, y: Int): Selection.Point? {
            return if (reflow) {
                var x = x - info!!.margin.left
                for (d in info.dst.keys) {
                    if (d.contains(x, y))
                        return Selection.Point(info.fromDst(d, x, y))
                }
                null
            } else {
                Selection.Point(x, y)
            }
        }

        /**
         * Создаёт выделение.
         */
        open fun select(start: ZLTextPosition, info: Reflow.Info?, w: Int, h: Int, x: Int, y: Int): Selection? {
            val p = selectPoint(info, x, y)
            if (p != null)
                return select(selectPage(start, info, w, h), p)
            return null
        }

        /**
         * Обновляет границы выделения.
         */
        open fun boundsUpdate(rr: Array<Rect>, info: Reflow.Info): Array<Rect> {
            val list = ArrayList<Rect>()
            for (r in rr) {
                for (s in info.src.keys) {
                    val i = Rect(r)
                    if (i.intersect(s) && (i.height() * 100 / s.height() > SelectionView.ARTIFACT_PERCENTS || r.height() > 0 && i.height() * 100 / r.height() > SelectionView.ARTIFACT_PERCENTS)) { // игнорируем артефакты высотой менее 10%
                        val d = info.fromSrc(s, i)
                        list.add(d)
                    }
                }
            }
            for (i in list.size - 1 downTo 0) {
                for (j in i - 1 downTo 0) {
                    val r = list[i]
                    val k = list[j]
                    if (r.intersects(k.left, k.top, k.right, k.bottom)) {
                        k.union(k)
                        list.removeAt(i)
                        break
                    }
                }
            }
            return list.toTypedArray()
        }

        /**
         * Возвращает ссылки на странице.
         */
        open fun getLinks(page: Selection.Page): Array<Link>? {
            return null
        }

        /**
         * Выполняет поиск текста.
         */
        open fun search(text: String): Search? {
            return null
        }

        /**
         * Класс выделения текста.
         */
        open class Selection {

            companion object {
                /**
                 * Вычисляет нечётный индекс.
                 */
                @JvmStatic
                fun odd(i: Int): Int {
                    return ((i + 1) / 2) * ((i + 1) % 2 - i % 2)
                }

                /**
                 * Вычисляет нечётную страницу.
                 */
                @JvmStatic
                fun odd(page: Int, i: Int, max: Int): Int {
                    var p = page + odd(i)
                    if (page <= i / 2)
                        p = i
                    if (page + i / 2 >= max)
                        p = max - i - 1
                    return p
                }
            }

            /**
             * Проверяет, является ли символ частью слова.
             */
            open fun isWord(c: Char): Boolean {
                if (Character.isSpaceChar(c))
                    return false
                return Character.isDigit(c) || Character.isLetter(c) || Character.isLetterOrDigit(c) || c == '[' || c == ']' || c == '(' || c == ')'
            }

            /**
             * Устанавливает начало выделения.
             */
            open fun setStart(page: Page, point: Point) {}

            /**
             * Устанавливает конец выделения.
             */
            open fun setEnd(page: Page, point: Point) {}

            /**
             * Возвращает выделенный текст.
             */
            open fun getText(): String? = null

            /**
             * Возвращает начало выделения.
             */
            open fun getStart(): ZLTextPosition? = null

            /**
             * Возвращает конец выделения.
             */
            open fun getEnd(): ZLTextPosition? = null

            /**
             * Возвращает все границы выделения.
             */
            open fun getBoundsAll(page: Page): Array<Rect>? = null

            /**
             * Возвращает границы выделения.
             */
            open fun getBounds(page: Page): Bounds? = null

            /**
             * Проверяет, находится ли точка выше выделения.
             */
            open fun isAbove(page: Page, point: Point): Boolean? = null

            /**
             * Проверяет, находится ли точка ниже выделения.
             */
            open fun isBelow(page: Page, point: Point): Boolean? = null

            /**
             * Проверяет, находится ли точка между началом и концом.
             */
            open fun inBetween(page: Page, start: Point, end: Point): Boolean? = null

            /**
             * Проверяет валидность точки.
             */
            open fun isValid(page: Page, point: Point): Boolean = false

            /**
             * Проверяет, выделена ли страница.
             */
            open fun isSelected(page: Int): Boolean = false

            /**
             * Закрывает выделение.
             */
            open fun close() {}

            /**
             * Интерфейс для установки границ выделения.
             */
            interface Setter {
                fun setStart(x: Int, y: Int)
                fun setEnd(x: Int, y: Int)
                fun getBounds(): Bounds?
            }

            /**
             * Границы выделения.
             */
            class Bounds {
                @JvmField var rr: Array<Rect>? = null
                @JvmField var reverse: Boolean = false
                @JvmField var start: Boolean = false
                @JvmField var end: Boolean = false
            }

            /**
             * Страница выделения.
             */
            class Page(
                @JvmField val page: Int,
                @JvmField val w: Int,
                @JvmField val h: Int
            )

            /**
             * Точка выделения.
             */
            class Point(
                @JvmField var x: Int,
                @JvmField var y: Int
            ) {
                constructor(p: android.graphics.Point) : this(p.x, p.y)
            }
        }

        /**
         * Ссылка в документе.
         */
        class Link {
            @JvmField var url: String? = null
            @JvmField var index: Int = 0
            @JvmField var rect: Rect? = null

            constructor()

            constructor(url: String?, index: Int, rect: Rect?) {
                this.url = url
                this.index = index
                this.rect = rect
            }
        }

        /**
         * Класс поиска текста.
         */
        open class Search {
            /**
             * Возвращает количество найденных результатов.
             */
            open fun getCount(): Int = 0

            /**
             * Переходит к следующему результату.
             */
            open fun next(): Int = -1

            /**
             * Переходит к предыдущему результату.
             */
            open fun prev(): Int = -1

            /**
             * Устанавливает страницу для поиска.
             */
            open fun setPage(page: Int) {}

            /**
             * Возвращает границы найденного текста.
             */
            open fun getBounds(page: Selection.Page): Bounds? = null

            /**
             * Закрывает поиск.
             */
            open fun close() {}

            /**
             * Границы найденного текста.
             */
            class Bounds {
                @JvmField var rr: Array<Rect>? = null
                @JvmField var highlight: Array<Rect>? = null
            }
        }
    }
}
