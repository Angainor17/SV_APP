package com.github.axet.bookreader.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.github.axet.androidlibrary.widgets.ThemeUtils
import com.github.axet.bookreader.app.Plugin
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.view.SelectionCursor
import timber.log.Timber

/**
 * View для отображения выделения текста.
 */
open class SelectionView(
    context: Context,
    custom: FBReaderView.CustomView,
    @JvmField val selection: Plugin.View.Selection
) : FrameLayout(context) {

    companion object {
        const val ARTIFACT_PERCENTS = 15
        const val SELECTION_ALPHA = 0x99
        const val SELECTION_PADDING = 1 // dp

        /**
         * Преобразует координаты rect в относительные (относительно родителя).
         */
        @JvmStatic
        fun relativeTo(thiz: Rect, rect: Rect) {
            thiz.offset(-rect.left, -rect.top)
        }

        /**
         * Преобразует координаты rect в абсолютные.
         */
        @JvmStatic
        fun absTo(thiz: Rect, rect: Rect) {
            thiz.offset(rect.left, rect.top)
        }

        /**
         * Создаёт Rect для круга с центром (x, y) и радиусом r.
         */
        @JvmStatic
        fun circleRect(x: Int, y: Int, r: Int): Rect {
            return Rect(x - r, y - r, x + r, y + r)
        }

        /**
         * Создаёт Rect для маркера выделения.
         */
        @JvmStatic
        fun rectHandle(which: SelectionCursor.Which, x: Int, y: Int): HotRect {
            val dpi = ZLibrary.Instance().displayDPI
            val unit = dpi / 120
            val xCenter = if (which == SelectionCursor.Which.Left) x - unit - 1 else x + unit + 1
            val rect = HotRect(xCenter - unit, y - dpi / 8, xCenter + unit, y + dpi / 8, x, y)
            if (which == SelectionCursor.Which.Left)
                rect.rect.union(circleRect(xCenter, y - dpi / 8, unit * 6))
            else
                rect.rect.union(circleRect(xCenter, y + dpi / 8, unit * 6))
            return rect
        }

        /**
         * Рисует маркер выделения.
         */
        @JvmStatic
        fun drawHandle(canvas: Canvas, which: SelectionCursor.Which, x: Int, y: Int, handles: Paint) {
            val dpi = ZLibrary.Instance().displayDPI
            val unit = dpi / 120
            val xCenter = if (which == SelectionCursor.Which.Left) x - unit - 1 else x + unit + 1
            canvas.drawRect(
                (xCenter - unit).toFloat(),
                (y - dpi / 8).toFloat(),
                (xCenter + unit).toFloat(),
                (y + dpi / 8).toFloat(),
                handles
            )
            if (which == SelectionCursor.Which.Left)
                canvas.drawCircle(xCenter.toFloat(), (y - dpi / 8).toFloat(), (unit * 6).toFloat(), handles)
            else
                canvas.drawCircle(xCenter.toFloat(), (y + dpi / 8).toFloat(), (unit * 6).toFloat(), handles)
        }

        /**
         * Объединяет список прямоугольников в один.
         */
        @JvmStatic
        fun union(rr: List<Rect>): Rect {
            var i = 0
            val bounds = Rect(rr[i++])
            while (i < rr.size) {
                val r = rr[i]
                bounds.union(r)
                i++
            }
            return bounds
        }

        /**
         * Проверяет пересечение линий по вертикали.
         */
        @JvmStatic
        fun lineIntersects(r1: Rect, r2: Rect): Boolean {
            return r1.top < r2.bottom && r2.top < r1.bottom
        }

        /**
         * Объединяет прямоугольники в линии.
         */
        @JvmStatic
        fun lines(rr: Array<Rect>): List<Rect> {
            return lines(rr.toList())
        }

        /**
         * Объединяет прямоугольники в линии.
         */
        @JvmStatic
        fun lines(rr: List<Rect>): List<Rect> {
            val lines = ArrayList<Rect>()
            for (r in rr) {
                var merged = false
                for (l in lines) {
                    if (lineIntersects(l, r)) {
                        l.union(r)
                        merged = true
                        break
                    }
                }
                if (!merged)
                    lines.add(Rect(r))
                // Слияние линий
                val linesCopy = ArrayList(lines)
                for (l in linesCopy) {
                    for (ll in lines) {
                        if (l != ll && lineIntersects(ll, l)) {
                            ll.union(l)
                            lines.remove(l)
                            break
                        }
                    }
                }
            }
            lines.sortWith(UL())
            return lines
        }
    }

    // === State Management ===

    /**
     * Текущее состояние drag операции.
     */
    private var _dragState: DragState = DragState.Idle

    /**
     * Публичное состояние drag (immutable).
     */
    val dragState: DragState get() = _dragState

    // === Legacy fields (будут удалены после полного refactor) ===

    var touch: PageView? = null
    var startRect = HandleRect()
    var endRect = HandleRect()
    val handles: Paint = Paint()
    @JvmField var margin: Rect? = null // абсолютные координаты
    var clip: Int = 0

    init {
        handles.style = Paint.Style.FILL
        handles.color = 0xff shl 24 or custom.selectionBackgroundColor.intValue()

        layoutParams = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        background = ColorDrawable(Color.TRANSPARENT)
    }

    /**
     * Устанавливает высоту отсечения.
     */
    fun setClipHeight(h: Int) {
        clip = h
    }

    /**
     * Находит PageView по координатам.
     */
    fun findView(x: Int, y: Int): PageView? {
        for (i in 0 until childCount) {
            val view = getChildAt(i) as PageView
            if (view.left < view.right && view.top < view.bottom &&
                x >= view.left && x < view.right && y >= view.top && y < view.bottom
            ) {
                return view
            }
        }
        return null
    }

    /**
     * Добавляет PageView.
     */
    fun add(page: PageView) {
        addView(page)
    }

    /**
     * Удаляет PageView.
     */
    fun remove(page: PageView) {
        if (touch == page)
            touch = null
        removeView(page)
        update()
    }

    /**
     * Обновляет PageView с новыми координатами.
     */
    fun update(page: PageView, x: Int, y: Int) {
        page.update(x, y)
        update()
    }

    /**
     * Обновляет всё выделение.
     */
    fun update() {
        var margin: Rect? = null

        var reverse = false
        var first: Rect? = null
        var firstPage: PageView? = null
        var last: Rect? = null
        var lastPage: PageView? = null

        for (i in 0 until childCount) {
            val v = getChildAt(i) as PageView

            if (margin == null)
                margin = Rect(v.margin)
            else
                margin.union(v.margin)

            reverse = v.selection!!.reverse

            if (v.selection!!.start) {
                first = Rect(v.lines!![0])
                absTo(first, v.margin)
                firstPage = v
            }
            if (v.selection!!.end) {
                last = Rect(v.lines!![v.lines!!.size - 1])
                absTo(last, v.margin)
                lastPage = v
            }
        }

        if (margin == null || first == null && last == null)
            return // повреждено

        if (first == null) { // reflow выделение может иметь артефакты между страницами
            first = Rect(lastPage!!.lines!![0])
            absTo(first, lastPage.margin)
            firstPage = lastPage
        }
        if (last == null) { // reflow выделение может иметь артефакты между страницами
            last = Rect(firstPage!!.lines!![firstPage.lines!!.size - 1])
            absTo(last, firstPage.margin)
            lastPage = firstPage
        }

        val left = rectHandle(SelectionCursor.Which.Left, first!!.left, first.top + first.height() / 2)
        val right = rectHandle(SelectionCursor.Which.Right, last!!.right, last.top + last.height() / 2)

        if (reverse) {
            startRect.rect = right
            startRect.which = SelectionCursor.Which.Right
            startRect.page = lastPage
            endRect.rect = left
            endRect.which = SelectionCursor.Which.Left
            endRect.page = firstPage
        } else {
            startRect.rect = left
            startRect.which = SelectionCursor.Which.Left
            startRect.page = firstPage
            endRect.rect = right
            endRect.which = SelectionCursor.Which.Right
            endRect.page = lastPage
        }

        startRect.makeUnion(margin)
        endRect.makeUnion(margin)

        startRect.drawRect(margin)
        endRect.drawRect(margin)

        this.margin = margin

        for (i in 0 until childCount) {
            val v = getChildAt(i) as PageView
            val vlp = v.layoutParams as MarginLayoutParams
            vlp.leftMargin = v.margin.left - margin.left
            vlp.topMargin = v.margin.top - margin.top
            vlp.width = v.margin.width()
            vlp.height = v.margin.height()
            v.requestLayout()
        }

        val lp = layoutParams as MarginLayoutParams
        lp.leftMargin = margin.left
        lp.topMargin = margin.top
        lp.width = margin.width()
        lp.height = margin.height()
        requestLayout()
    }

    override fun draw(canvas: Canvas) {
        val c = canvas.clipBounds
        c.bottom = clip - top
        canvas.clipRect(c)
        super.draw(canvas)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (margin != null) { // не повреждено / не пустое окно
            drawHandle(canvas, startRect.which!!, startRect)
            drawHandle(canvas, endRect.which!!, endRect)
        }
    }

    /**
     * Рисует маркер выделения.
     */
    fun drawHandle(canvas: Canvas, which: SelectionCursor.Which, rect: HandleRect) {
        drawHandle(canvas, which, rect.draw!!.x, rect.draw!!.y, handles)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Timber.tag("voronin").d("SelectionView onTouchEvent: action=${event.action}, dragState=$_dragState")

        // Если margin null, выделение повреждено - не обрабатываем
        if (margin == null) {
            Timber.d("SelectionView: margin is null, skipping touch")
            return super.onTouchEvent(event)
        }

        val x = event.x.toInt() + left
        val y = event.y.toInt() + top

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> handleDragStart(x, y)
            MotionEvent.ACTION_MOVE -> handleDragMove(x, y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handleDragEnd(x, y, event.action == MotionEvent.ACTION_CANCEL)
            else -> super.onTouchEvent(event)
        }
    }

    /**
     * Обрабатывает начало drag - ACTION_DOWN.
     * Проверяет попадание в маркеры и начинает drag если попал.
     */
    private fun handleDragStart(x: Int, y: Int): Boolean {
        // Проверяем попадание в start маркер (Left)
        val startHit = checkHandleHit(startRect, x, y, HandleType.LEFT)
        if (startHit.hit) {
            startDrag(startHit.handleType!!, startHit.offsetX, startHit.offsetY, x, y)
            return true
        }

        // Проверяем попадание в end маркер (Right)
        val endHit = checkHandleHit(endRect, x, y, HandleType.RIGHT)
        if (endHit.hit) {
            startDrag(endHit.handleType!!, endHit.offsetX, endHit.offsetY, x, y)
            return true
        }

        return false
    }

    /**
     * Обрабатывает движение drag - ACTION_MOVE.
     * Обновляет позицию маркера если drag активен.
     */
    private fun handleDragMove(x: Int, y: Int): Boolean {
        val state = _dragState as? DragState.Dragging ?: return false

        // Корректируем координаты с offset
        val adjustedX = x + state.offsetX
        val adjustedY = y + state.offsetY

        // Обновляем bounds через setter
        updateHandlePosition(state.handle, adjustedX, adjustedY)

        return true
    }

    /**
     * Обрабатывает завершение drag - ACTION_UP или ACTION_CANCEL.
     */
    private fun handleDragEnd(x: Int, y: Int, cancelled: Boolean): Boolean {
        val state = _dragState as? DragState.Dragging ?: return false

        if (!cancelled) {
            // Корректируем координаты с offset для финальной позиции
            val adjustedX = x + state.offsetX
            val adjustedY = y + state.offsetY
            updateHandlePosition(state.handle, adjustedX, adjustedY)
        }

        // Завершаем drag
        endDrag()

        return true
    }

    /**
     * Проверяет попадание touch в маркер.
     */
    private fun checkHandleHit(handleRect: HandleRect, x: Int, y: Int, handleType: HandleType): HandleTouchResult {
        val hotRect = handleRect.rect?.rect ?: return HandleTouchResult.NO_HIT
        val hotRectRef = handleRect.rect ?: return HandleTouchResult.NO_HIT

        // Проверяем попадание в rect маркера
        if (hotRect.contains(x, y)) {
            // Вычисляем offset от touch point до hot point
            val offsetX = hotRectRef.hotx - x
            val offsetY = hotRectRef.hoty - y
            Timber.d("Handle hit: $handleType at ($x, $y), offset ($offsetX, $offsetY)")
            return HandleTouchResult.hit(handleType, offsetX, offsetY)
        }

        return HandleTouchResult.NO_HIT
    }

    /**
     * Начинает drag операцию.
     */
    private fun startDrag(handle: HandleType, offsetX: Int, offsetY: Int, startX: Int, startY: Int) {
        Timber.d("Starting drag: handle=$handle, offset=($offsetX, $offsetY)")
        _dragState = DragState.Dragging(handle, offsetX, offsetY, startX, startY)
        onTouchLock()
    }

    /**
     * Обновляет позицию маркера через setter.
     */
    private fun updateHandlePosition(handle: HandleType, x: Int, y: Int) {
        val handleRect = when (handle) {
            HandleType.LEFT -> startRect
            HandleType.RIGHT -> endRect
        }

        val page = handleRect.page
        val setter = page?.setter

        if (setter == null) {
            Timber.w("Cannot update handle position: setter is null for handle $handle")
            return
        }

        // Вызываем соответствующий setter
        when (handle) {
            HandleType.LEFT -> setter.setStart(x, y)
            HandleType.RIGHT -> setter.setEnd(x, y)
        }

        Timber.d("Updated handle $handle position to ($x, $y)")
    }

    /**
     * Завершает drag операцию.
     */
    private fun endDrag() {
        val previousState = _dragState
        Timber.d("Ending drag: previousState=$previousState")
        _dragState = DragState.Idle
        onTouchUnlock()
    }

    /**
     * Закрывает выделение.
     */
    fun close() {
        if (selection != null) {
            selection.close()
        }
    }

    /**
     * Вызывается при начале касания.
     */
    open fun onTouchLock() {}

    /**
     * Вызывается при окончании касания.
     */
    open fun onTouchUnlock() {}

    /**
     * Возвращает Y-координату начала выделения.
     */
    fun getSelectionStartY(): Int {
        if (margin == null) // повреждено / пустое окно
            return 0
        return startRect.rect!!.rect.top
    }

    /**
     * Возвращает Y-координату конца выделения.
     */
    fun getSelectionEndY(): Int {
        if (margin == null) // повреждено / пустое окно
            return 0
        return endRect.rect!!.rect.bottom
    }

    /**
     * Компаратор для сортировки линий.
     */
    class LinesUL(rr: Array<Rect>) : Comparator<Rect> {
        val lines: List<Rect> = lines(rr)

        constructor(rr: List<Rect>) : this(rr.toTypedArray()) {
            // Конструктор для списка
        }

        fun getLine(r: Rect): Int {
            for (i in lines.indices) {
                val l = lines[i]
                if (r.intersect(l))
                    return i
            }
            return -1
        }

        override fun compare(o1: Rect, o2: Rect): Int {
            val r = getLine(o1).compareTo(getLine(o2))
            if (r != 0)
                return r
            return o1.left.compareTo(o2.left)
        }
    }

    /**
     * Компаратор для сортировки по верхнему краю.
     */
    class UL : Comparator<Rect> {
        override fun compare(o1: Rect, o2: Rect): Int {
            val r = o1.top.compareTo(o2.top)
            if (r != 0)
                return r
            return o1.left.compareTo(o2.left)
        }
    }

    /**
     * Прямоугольник с горячей точкой.
     */
    class HotRect {
        var hotx: Int
        var hoty: Int
        var rect: Rect

        constructor(r: HotRect) {
            this.rect = Rect(r.rect)
            hotx = r.hotx
            hoty = r.hoty
        }

        constructor(left: Int, top: Int, right: Int, bottom: Int, x: Int, y: Int) {
            this.rect = Rect(left, top, right, bottom)
            hotx = x
            hoty = y
        }

        /**
         * Преобразует координаты в относительные.
         */
        fun relativeTo(rect: Rect) {
            relativeTo(this.rect, rect)
            hotx = hotx - rect.left
            hoty = hoty - rect.top
        }

        /**
         * Создаёт точку с горячей точкой.
         */
        fun makePoint(x: Int, y: Int): HotPoint {
            return HotPoint(x, y, hotx - x, hoty - y)
        }
    }

    /**
     * Прямоугольник с обработкой касаний.
     */
    open class TouchRect {
        var rect: HotRect? = null
        var touch: HotPoint? = null

        constructor()

        constructor(r: TouchRect) {
            rect = HotRect(r.rect!!)
            if (r.touch != null)
                touch = HotPoint(r.touch!!)
        }

        /**
         * Преобразует координаты в относительные.
         */
        open fun relativeTo(rect: Rect) {
            this.rect!!.relativeTo(rect)
            if (touch != null)
                touch!!.relativeTo(rect)
        }

        /**
         * Обрабатывает событие касания.
         */
        fun onTouchEvent(a: Int, x: Int, y: Int): Boolean {
            if (a == MotionEvent.ACTION_DOWN && rect!!.rect.contains(x, y) || touch != null) {
                if (touch == null)
                    touch = rect!!.makePoint(x, y)
                else
                    touch = HotPoint(x, y, touch!!)
                return true
            }
            return false
        }

        /**
         * Обрабатывает отпускание касания.
         */
        fun onTouchRelease(event: MotionEvent) {
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL)
                touch = null
        }
    }

    /**
     * Точка с смещением.
     */
    class HotPoint : Point {
        var offx: Int
        var offy: Int

        constructor(r: HotPoint) : super(r) {
            offx = r.offx
            offy = r.offy
        }

        constructor(x: Int, y: Int, hx: Int, hy: Int) : super(x + hx, y + hy) {
            offx = hx
            offy = hy
        }

        constructor(x: Int, y: Int, h: HotPoint) : super(x + h.offx, y + h.offy) {
            offx = h.offx
            offy = h.offy
        }

        /**
         * Преобразует координаты в относительные.
         */
        fun relativeTo(r: Rect) {
            x = x - r.left
            y = y - r.top
        }

        /**
         * Преобразует координаты в абсолютные.
         */
        fun absTo(r: Rect) {
            x = x + r.left
            y = y + r.top
        }
    }

    /**
     * Прямоугольник маркера выделения.
     */
    class HandleRect : TouchRect {
        var which: SelectionCursor.Which? = null
        var page: PageView? = null
        var draw: Point? = null

        constructor()

        constructor(r: HandleRect, re: Rect) : super(r) {
            which = r.which
            page = r.page
            relativeTo(re)
        }

        /**
         * Объединяет с прямоугольником.
         */
        fun makeUnion(rect: Rect) {
            rect.union(this.rect!!.rect)
            if (touch != null)
                rect.union(rectHandle(which!!, touch!!.x, touch!!.y).rect)
        }

        /**
         * Обновляет координаты для рисования.
         */
        fun drawRect(rect: Rect) {
            if (touch != null)
                draw = Point(touch!!)
            else
                draw = Point(this.rect!!.hotx, this.rect!!.hoty)
            draw!!.x -= rect.left
            draw!!.y -= rect.top
        }
    }

    /**
     * View для страницы с выделением.
     */
    class PageView(
        context: Context,
        custom: FBReaderView.CustomView,
        val setter: Plugin.View.Selection.Setter?
    ) : View(context) {

        var viewBounds = Rect() // размер view
        var margin = Rect() // абсолютные координаты
        var selection: Plugin.View.Selection.Bounds? = null

        var lines: List<Rect>? = null

        val paint: Paint = Paint()
        val padding: Int

        init {
            paint.style = Paint.Style.FILL
            paint.color = SELECTION_ALPHA shl 24 or custom.selectionBackgroundColor.intValue()

            padding = ThemeUtils.dp2px(context, SELECTION_PADDING.toFloat()).toInt()

            layoutParams = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        /**
         * Обновляет с новыми координатами.
         */
        fun update(x: Int, y: Int) {
            update()
            margin.left = viewBounds.left + x
            margin.right = viewBounds.right + x
            margin.top = viewBounds.top + y
            margin.bottom = viewBounds.bottom + y
        }

        /**
         * Обновляет выделение.
         */
        fun update() {
            selection = setter!!.getBounds()

            if (selection!!.rr == null || selection!!.rr!!.isEmpty())
                return

            lines = lines(selection!!.rr!!.toList())

            viewBounds = union(lines!!)

            viewBounds.inset(-padding, -padding)

            for (r in lines!!) {
                r.inset(-padding, -padding)
                relativeTo(r, viewBounds)
            }
        }

        override fun onDraw(canvas: Canvas) {
            for (r in lines!!)
                canvas.drawRect(r, paint)
        }
    }
}
