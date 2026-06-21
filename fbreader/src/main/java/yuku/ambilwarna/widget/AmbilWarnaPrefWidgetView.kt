package yuku.ambilwarna.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Style
import android.util.AttributeSet
import android.view.View

/**
 * Виджет для отображения цвета в PreferenceScreen.
 * Отображает квадрат с выбранным цветом и опционально крестик.
 */
class AmbilWarnaPrefWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint: Paint
    private val rectSize: Float
    private val strokeWidth: Float

    private var drawCross: Boolean = false

    init {
        val density = context.resources.displayMetrics.density
        rectSize = (24f * density + 0.5f).toInt().toFloat()
        strokeWidth = (1f * density + 0.5f).toInt().toFloat()

        paint = Paint().apply {
            color = 0xffffffff.toInt()
            style = Style.STROKE
            this.strokeWidth = this@AmbilWarnaPrefWidgetView.strokeWidth
        }
    }

    /**
     * Показать или скрыть крестик поверх виджета.
     * @param show true - показать крестик, false - скрыть
     */
    fun showCross(show: Boolean) {
        drawCross = show
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(
            strokeWidth,
            strokeWidth,
            rectSize - strokeWidth,
            rectSize - strokeWidth,
            paint
        )

        if (drawCross) {
            canvas.drawLine(
                strokeWidth,
                strokeWidth,
                rectSize - strokeWidth,
                rectSize - strokeWidth,
                paint
            )
            canvas.drawLine(
                strokeWidth,
                rectSize - strokeWidth,
                rectSize - strokeWidth,
                strokeWidth,
                paint
            )
        }
    }
}
