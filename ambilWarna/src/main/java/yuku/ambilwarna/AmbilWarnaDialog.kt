package yuku.ambilwarna

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.RelativeLayout

/**
 * Диалоговое окно для выбора цвета (Color Picker).
 * Позволяет пользователю выбрать цвет с помощью HSV модели.
 *
 * Пример использования:
 * ```kotlin
 * val dialog = AmbilWarnaDialog(
 *     context,
 *     currentColor,
 *     object : AmbilWarnaDialog.OnAmbilWarnaListener {
 *         override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
 *             // Пользователь выбрал цвет
 *         }
 *         override fun onCancel(dialog: AmbilWarnaDialog) {
 *             // Пользователь отменил выбор
 *         }
 *     },
 *     "OK",
 *     "Отмена"
 * )
 * dialog.show()
 * ```
 */
class AmbilWarnaDialog(
    context: Context,
    color: Int,
    private val listener: OnAmbilWarnaListener?,
    private val positiveButtonText: CharSequence,
    private val negativeButtonText: CharSequence
) {

    /**
     * Интерфейс для обработки результатов выбора цвета.
     */
    interface OnAmbilWarnaListener {
        /**
         * Вызывается когда пользователь подтверждает выбор цвета.
         * @param dialog экземпляр диалога
         * @param color выбранный цвет
         */
        fun onOk(dialog: AmbilWarnaDialog, color: Int)

        /**
         * Вызывается когда пользователь отменяет выбор цвета.
         * @param dialog экземпляр диалога
         */
        fun onCancel(dialog: AmbilWarnaDialog)
    }

    private val dialog: AlertDialog
    private val viewHue: View
    private val viewSatVal: AmbilWarnaKotak
    private val viewCursor: ImageView
    private val viewOldColor: View
    private val viewNewColor: View
    private val viewTarget: ImageView
    private val viewContainer: ViewGroup
    private val currentColorHsv = FloatArray(3)

    init {
        Color.colorToHSV(color, currentColorHsv)

        val view = LayoutInflater.from(context).inflate(R.layout.ambilwarna_dialog, null)
        viewHue = view.findViewById(R.id.ambilwarna_viewHue)
        viewSatVal = view.findViewById(R.id.ambilwarna_viewSatBri)
        viewCursor = view.findViewById(R.id.ambilwarna_cursor)
        viewOldColor = view.findViewById(R.id.ambilwarna_warnaLama)
        viewNewColor = view.findViewById(R.id.ambilwarna_warnaBaru)
        viewTarget = view.findViewById(R.id.ambilwarna_target)
        viewContainer = view.findViewById(R.id.ambilwarna_viewContainer)

        viewSatVal.setHue(hue)
        viewOldColor.setBackgroundColor(color)
        viewNewColor.setBackgroundColor(color)

        // Обработка касаний на полосе тона (Hue)
        viewHue.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_MOVE ||
                event.action == MotionEvent.ACTION_DOWN ||
                event.action == MotionEvent.ACTION_UP
            ) {
                var y = event.y
                if (y < 0f) y = 0f
                if (y > viewHue.measuredHeight) {
                    y = viewHue.measuredHeight - 0.001f // Чтобы избежать перехода с конца в начало
                }
                var hue = 360f - 360f / viewHue.measuredHeight * y
                if (hue == 360f) hue = 0f
                setHue(hue)

                // Обновление вида
                viewSatVal.setHue(this.hue)
                moveCursor()
                viewNewColor.setBackgroundColor(getColor())
                true
            } else {
                false
            }
        }

        // Обработка касаний на поле насыщенности/яркости
        viewSatVal.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_MOVE ||
                event.action == MotionEvent.ACTION_DOWN ||
                event.action == MotionEvent.ACTION_UP
            ) {
                var x = event.x
                var y = event.y

                if (x < 0f) x = 0f
                if (x > viewSatVal.measuredWidth) x = viewSatVal.measuredWidth.toFloat()
                if (y < 0f) y = 0f
                if (y > viewSatVal.measuredHeight) y = viewSatVal.measuredHeight.toFloat()

                setSat(1f / viewSatVal.measuredWidth * x)
                setVal(1f - (1f / viewSatVal.measuredHeight * y))

                // Обновление вида
                moveTarget()
                viewNewColor.setBackgroundColor(getColor())
                true
            } else {
                false
            }
        }

        // Создание диалога
        dialog = AlertDialog.Builder(context)
            .setPositiveButton(positiveButtonText) { _, _ ->
                listener?.onOk(this, getColor())
            }
            .setNegativeButton(negativeButtonText) { _, _ ->
                listener?.onCancel(this)
            }
            .setOnCancelListener {
                // Если нажата кнопка "Назад", вызываем callback
                listener?.onCancel(this)
            }
            .create()

        // Убираем все отступы в диалоге
        dialog.setView(view, 0, 0, 0, 0)

        // Перемещаем курсор иtarget при первой отрисовке
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                moveCursor()
                moveTarget()
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    /**
     * Перемещает курсор на полосе тона.
     */
    private fun moveCursor() {
        var y = viewHue.measuredHeight - (hue * viewHue.measuredHeight / 360f)
        if (y == viewHue.measuredHeight.toFloat()) y = 0f

        val layoutParams = viewCursor.layoutParams as RelativeLayout.LayoutParams
        layoutParams.leftMargin = (viewHue.left - Math.floor(viewCursor.measuredWidth / 2.0) - viewContainer.paddingLeft).toInt()
        layoutParams.topMargin = (viewHue.top + y - Math.floor(viewCursor.measuredHeight / 2.0) - viewContainer.paddingTop).toInt()
        viewCursor.layoutParams = layoutParams
    }

    /**
     * Перемещает курсор на поле насыщенности/яркости.
     */
    private fun moveTarget() {
        val x = sat * viewSatVal.measuredWidth
        val y = (1f - value) * viewSatVal.measuredHeight

        val layoutParams = viewTarget.layoutParams as RelativeLayout.LayoutParams
        layoutParams.leftMargin = (viewSatVal.left + x - Math.floor(viewTarget.measuredWidth / 2.0) - viewContainer.paddingLeft).toInt()
        layoutParams.topMargin = (viewSatVal.top + y - Math.floor(viewTarget.measuredHeight / 2.0) - viewContainer.paddingTop).toInt()
        viewTarget.layoutParams = layoutParams
    }

    /**
     * Возвращает текущий выбранный цвет.
     */
    private fun getColor(): Int = Color.HSVToColor(currentColorHsv)

    /**
     * Возвращает текущее значение тона (Hue).
     */
    private val hue: Float
        get() = currentColorHsv[0]

    /**
     * Устанавливает значение тона (Hue).
     */
    private fun setHue(hue: Float) {
        currentColorHsv[0] = hue
    }

    /**
     * Возвращает текущее значение насыщенности (Saturation).
     */
    private val sat: Float
        get() = currentColorHsv[1]

    /**
     * Устанавливает значение насыщенности (Saturation).
     */
    private fun setSat(sat: Float) {
        currentColorHsv[1] = sat
    }

    /**
     * Возвращает текущее значение яркости (Value).
     */
    private val value: Float
        get() = currentColorHsv[2]

    /**
     * Устанавливает значение яркости (Value).
     */
    private fun setVal(value: Float) {
        currentColorHsv[2] = value
    }

    /**
     * Показывает диалог выбора цвета.
     */
    fun show() {
        dialog.show()
    }

    /**
     * Возвращает AlertDialog для дополнительной настройки.
     */
    fun getDialog(): AlertDialog = dialog
}
