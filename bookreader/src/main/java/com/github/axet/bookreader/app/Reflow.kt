package com.github.axet.bookreader.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.DisplayMetrics
import androidx.preference.PreferenceManager
import com.github.axet.androidlibrary.app.Natives
import com.github.axet.bookreader.widgets.FBReaderView
import com.github.axet.k2pdfopt.Config
import com.github.axet.k2pdfopt.K2PdfOpt
import org.geometerplus.zlibrary.core.view.ZLViewEnums

/**
 * Класс для переформатирования (reflow) PDF/DJVU документов.
 * Позволяет адаптировать текст под ширину экрана.
 */
class Reflow(
    context: Context,
    w: Int,
    h: Int,
    page: Int,
    private val custom: FBReaderView.CustomView,
    private val info: Storage.RecentInfo
) {
    @JvmField var k2: K2PdfOpt? = null
    @JvmField var page: Int = 0 // страница документа
    @JvmField var index: Int = 0 // текущая позиция просмотра
    @JvmField var pending: Int = 0 // ожидаемое действие, можно отменить
    @JvmField var w: Int = 0
    @JvmField var h: Int = 0
    @JvmField var rw: Int = 0
    @JvmField var bm: Bitmap? = null // исходный bitmap, утилизируется при ошибках
    private val context: Context

    init {
        K2PdfOptInit(context)
        this.context = context
        this.page = page
        reset(w, h)
    }

    companion object {
        /**
         * Инициализирует нативные библиотеки k2pdfopt.
         */
        @JvmStatic
        fun K2PdfOptInit(context: Context) {
            if (Config.natives) {
                Natives.loadLibraries(context, "willus", "k2pdfopt", "k2pdfoptjni")
                Config.natives = false
            }
        }

        /**
         * Рисует прямоугольник на canvas.
         */
        @JvmStatic
        fun drawRect(canvas: Canvas, rect: Rect, paint: Paint) {
            canvas.drawLine(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.top.toFloat(), paint)
            canvas.drawLine(rect.left.toFloat(), rect.bottom.toFloat(), rect.right.toFloat(), rect.bottom.toFloat(), paint)
            canvas.drawLine(rect.left.toFloat(), rect.top.toFloat(), rect.left.toFloat(), rect.bottom.toFloat(), paint)
            canvas.drawLine(rect.right.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat(), paint)
        }
    }

    /**
     * Возвращает левый отступ.
     */
    fun getLeftMargin(): Int = custom.leftMargin

    /**
     * Возвращает правый отступ.
     */
    fun getRightMargin(): Int = custom.rightMargin

    /**
     * Сбрасывает состояние reflow.
     */
    fun reset() {
        w = 0
        h = 0
        k2?.let {
            it.close()
            k2 = null
        }
    }

    /**
     * Сбрасывает состояние с новыми размерами.
     */
    fun reset(w: Int, h: Int) {
        if (this.w != w || this.h != h) {
            val rw = w - getLeftMargin() - getRightMargin()
            this.w = w
            this.h = h
            this.rw = rw
            this.index = 0 // размер изменился, reflow страница может переполнить общее количество страниц
            create()
        }
        if (k2 == null)
            create()
    }

    private fun create() {
        val shared: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        var old: Float = shared.getFloat(BookApplication.PREFERENCE_FONTSIZE_REFLOW, BookApplication.PREFERENCE_FONTSIZE_REFLOW_DEFAULT)
        if (info.fontsize != null)
            old = info.fontsize!! / 100f
        k2?.let {
            old = it.fontSize
            it.close()
        }
        k2 = K2PdfOpt()
        val d: DisplayMetrics = context.resources.displayMetrics
        k2!!.create(rw, h, d.densityDpi)
        k2!!.fontSize = old
    }

    /**
     * Загружает bitmap для reflow.
     */
    fun load(bm: Bitmap) {
        this.bm?.recycle()
        this.bm = bm
        index = 0
        k2!!.load(bm)
    }

    /**
     * Загружает bitmap для reflow с указанием страницы и индекса.
     */
    fun load(bm: Bitmap, page: Int, index: Int) {
        this.bm?.recycle()
        this.bm = bm
        this.page = page
        this.index = index
        k2!!.load(bm)
    }

    /**
     * Возвращает количество reflow страниц.
     */
    fun count(): Int {
        if (k2 == null) return -1
        if (bm == null) return -1
        return k2!!.count
    }

    /**
     * Возвращает количество страниц (минимум 1).
     */
    fun emptyCount(): Int {
        var c = count()
        if (c == 0) c = 1
        return c
    }

    /**
     * Рендерит указанную страницу reflow.
     */
    fun render(page: Int): Bitmap = k2!!.renderPage(page)

    /**
     * Проверяет возможность прокрутки.
     */
    fun canScroll(pos: ZLViewEnums.PageIndex): Boolean {
        return when (pos) {
            ZLViewEnums.PageIndex.previous -> index > 0
            ZLViewEnums.PageIndex.next -> index + 1 < count()
            else -> true // current???
        }
    }

    /**
     * Обрабатывает завершение прокрутки.
     */
    fun onScrollingFinished(pos: ZLViewEnums.PageIndex) {
        when (pos) {
            ZLViewEnums.PageIndex.next -> {
                index++
                pending = 0
            }
            ZLViewEnums.PageIndex.current -> { // cancel
                pending = 0
                if (count() == -1) return
                if (index < 0) {
                    index = -1
                    recycle()
                    return
                }
                if (index >= count()) { // current points to next page +1
                    page += 1
                    index = 0
                    recycle()
                    return
                }
            }
            ZLViewEnums.PageIndex.previous -> {
                index--
                pending = 0
            }
        }
    }

    /**
     * Освобождает ресурсы.
     */
    fun recycle() {
        k2?.let {
            it.close()
            k2 = null
        }
        bm?.let {
            it.recycle()
            bm = null
        }
    }

    /**
     * Закрывает reflow и освобождает ресурсы.
     */
    fun close() {
        recycle()
    }

    /**
     * Рисует исходный bitmap с выделенным прямоугольником.
     */
    fun drawSrc(pluginview: Plugin.View, info: Info, r: Rect): Bitmap {
        val bm = drawSrc(pluginview, info)
        val c = Canvas(bm)
        val paint = Paint().apply {
            color = Color.MAGENTA
            isAntiAlias = false
            style = Paint.Style.STROKE
            strokeWidth = 0f
        }
        drawRect(c, r, paint)
        return bm
    }

    /**
     * Рисует исходный bitmap с выделенной точкой.
     */
    fun drawSrc(pluginview: Plugin.View, info: Info, p: Point): Bitmap {
        val bm = drawSrc(pluginview, info)
        val c = Canvas(bm)
        val paint = Paint().apply {
            isAntiAlias = false
            color = Color.MAGENTA
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 0f
        }
        c.drawCircle(p.x.toFloat(), p.y.toFloat(), 3f, paint)
        return bm
    }

    /**
     * Рисует исходный bitmap с разметкой регионов.
     */
    fun drawSrc(pluginview: Plugin.View, info: Info): Bitmap {
        val b = pluginview.render(w, h, this.page)!!
        val canvas = Canvas(b)
        draw(canvas, info.src.keys)
        return b
    }

    /**
     * Рисует результирующий bitmap с выделенным прямоугольником.
     */
    fun drawDst(info: Info, r: Rect): Bitmap? {
        val bm = drawDst(info) ?: return null
        val c = Canvas(bm)
        val paint = Paint().apply {
            isAntiAlias = false
            color = Color.MAGENTA
            style = Paint.Style.STROKE
            strokeWidth = 0f
        }
        drawRect(c, r, paint)
        return bm
    }

    /**
     * Рисует результирующий bitmap с выделенной точкой.
     */
    fun drawDst(info: Info, p: Point): Bitmap? {
        val bm = drawDst(info) ?: return null
        val c = Canvas(bm)
        val paint = Paint().apply {
            color = Color.MAGENTA
            isAntiAlias = false
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 3f
        }
        c.drawCircle(p.x.toFloat(), p.y.toFloat(), 3f, paint)
        return bm
    }

    private fun findPage(info: Info): Int {
        for (i in 0 until count()) {
            if (info.src == k2!!.getRectMaps(i))
                return i
        }
        return -1
    }

    /**
     * Рисует результирующий bitmap с разметкой регионов.
     */
    fun drawDst(info: Info): Bitmap? {
        val page = findPage(info)
        if (page == -1) return null
        val b = render(page)
        val canvas = Canvas(b)
        draw(canvas, info.dst.keys)
        return b
    }

    private fun draw(canvas: Canvas, keys: Set<Rect>) {
        val kk = keys.toTypedArray()
        val paint = Paint().apply {
            color = Color.BLUE
            isAntiAlias = false
            style = Paint.Style.STROKE
            strokeWidth = 0f
        }
        val text = Paint().apply {
            color = Color.RED
            strokeWidth = 0f
        }
        for (i in kk.indices) {
            val k = kk[i]
            drawRect(canvas, k, paint)

            val t = "" + i

            var size = 0
            val bounds = Rect()
            do {
                text.textSize = size.toFloat()
                text.getTextBounds(t, 0, t.length, bounds)
                size++
            } while (bounds.height() < k.height())

            val m = text.measureText(t)
            canvas.drawText(t, k.centerX() - m / 2, k.top + k.height().toFloat(), text)
        }
    }

    /**
     * Информация о регионах reflow.
     */
    class Info(reflower: Reflow, page: Int) {
        @JvmField val bm: Rect // размер исходного bitmap
        @JvmField val margin: Rect // отступы страницы
        @JvmField val src: Map<Rect, Rect>
        @JvmField val dst: Map<Rect, Rect>

        init {
            bm = Rect(0, 0, reflower.bm!!.width, reflower.bm!!.height)
            margin = Rect(reflower.getLeftMargin(), 0, reflower.getRightMargin(), 0)
            src = if (reflower.k2!!.count > 0)
                reflower.k2!!.getRectMaps(page)
            else
                HashMap()
            dst = HashMap()
            for (k in src.keys) {
                val v = src[k]
                dst[v!!] = k
            }
        }

        /**
         * Преобразует координаты из результирующего в исходный bitmap.
         */
        fun fromDst(d: Rect, x: Int, y: Int): Point {
            val s = dst[d]!!
            val kx = s.width().toDouble() / d.width()
            val ky = s.height().toDouble() / d.height()
            return Point(s.left + ((x - d.left) * kx).toInt(), s.top + ((y - d.top) * ky).toInt())
        }

        /**
         * Преобразует прямоугольник из исходного в результирующий bitmap.
         */
        fun fromSrc(s: Rect, r: Rect): Rect {
            val d = src[s]!!
            val kx = d.width().toDouble() / s.width()
            val ky = d.height().toDouble() / s.height()
            return Rect(
                d.left + ((r.left - s.left) * kx).toInt(),
                d.top + ((r.top - s.top) * ky).toInt(),
                d.right + ((r.right - s.right) * kx).toInt(),
                d.bottom + ((r.bottom - s.bottom) * ky).toInt()
            )
        }
    }
}
