package yuku.ambilwarna

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Кастомная View для отображения цветового поля (насыщенность/яркость).
 * Использует HSV модель для выбора цвета.
 */
class AmbilWarnaKotak @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val color = floatArrayOf(1f, 1f, 1f)
    private var paint: Paint? = null
    private var luar: Shader? = null

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (paint == null) {
            paint = Paint()
            luar = LinearGradient(
                0f, 0f, 0f, measuredHeight.toFloat(),
                0xffffffff.toInt(), 0xff000000.toInt(),
                Shader.TileMode.CLAMP
            )
        }

        val rgb = Color.HSVToColor(color)
        val dalam = LinearGradient(
            0f, 0f, measuredWidth.toFloat(), 0f,
            0xffffffff.toInt(), rgb,
            Shader.TileMode.CLAMP
        )
        val shader = ComposeShader(luar!!, dalam, PorterDuff.Mode.MULTIPLY)
        paint!!.shader = shader
        canvas.drawRect(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat(), paint!!)
    }

    /**
     * Устанавливает тон (Hue) для цветового поля.
     * @param hue значение тона (0-360)
     */
    fun setHue(hue: Float) {
        color[0] = hue
        invalidate()
    }
}
