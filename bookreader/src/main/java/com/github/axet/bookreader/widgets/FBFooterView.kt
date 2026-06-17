package com.github.axet.bookreader.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import com.github.axet.androidlibrary.widgets.ThemeUtils
import com.github.axet.bookreader.R
import org.geometerplus.fbreader.fbreader.FBView
import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.text.view.ZLTextView
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidPaintContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * View для отображения нижней панели читалки с прогрессом, временем и батареей.
 */
class FBFooterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val nextGeneratedId = AtomicInteger(1)

        /**
         * Генерирует уникальный ID для view.
         */
        fun generateViewId(): Int { // ViewCompat API27
            if (Build.VERSION.SDK_INT >= 17)
                return View.generateViewId()
            while (true) {
                val result = nextGeneratedId.get()
                var newValue = result + 1
                if (newValue > 0x00FFFFFF) newValue = 1 // Сброс на 1, не 0
                if (nextGeneratedId.compareAndSet(result, newValue))
                    return result
            }
        }
    }

    private var fb: FBReaderView? = null
    private var customview: FBReaderView.CustomView? = null
    private var footer: FBView.Footer? = null
    private var pagePosition: ZLTextView.PagePosition? = null
    private var family: String? = null
    private var tf: Typeface? = null

    constructor(context: Context, fb: FBReaderView) : this(context) {
        id = generateViewId()
        create(fb)
    }

    /**
     * Создаёт содержимое footer.
     */
    fun create(fb: FBReaderView) {
        this.fb = fb
        update()
        orientation = HORIZONTAL
        val cProfile = fb.app.viewOptions.getColorProfile()
        setBackgroundColor(0xffffff and cProfile.footerNGBackgroundOption.value!!.intValue() or 0xff000000.toInt())
        var lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp.gravity = Gravity.CENTER
        addView(TOCMarks(context), lp)
        val footerOptions = fb.app.viewOptions.getFooterOptions()
        val lpText = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, footer!!.height)
        lpText.gravity = Gravity.CENTER
        if (footerOptions.showProgressAsPages())
            addView(ProgressAsPages(context), lpText)
        if (footerOptions.showProgressAsPercentage() && pagePosition!!.total != 0)
            addView(ProgressAsPercentage(context), lpText)
        if (footerOptions.showClock.value) {
            val clock = Clock(context)
            val dp4 = ThemeUtils.dp2px(context, 4f).toInt()
            val dp2 = ThemeUtils.dp2px(context, 2f).toInt()
            clock.setPadding(dp4, 0, dp2, 0)
            addView(clock, lpText)
        }
        if (footerOptions.showBattery.value) {
            val image = AppCompatImageView(context)
            image.setImageResource(R.drawable.ic_battery_std_24)
            image.setColorFilter(0xffffff and cProfile.footerNGForegroundOption.value!!.intValue() or 0xff000000.toInt())
            val lpImage = LinearLayout.LayoutParams(footer!!.height, footer!!.height)
            lpImage.gravity = Gravity.CENTER
            addView(image, lpImage)
            addView(Battery(context), lpText)
        }
        setPadding(0, 0, customview!!.rightMargin, 0)
    }

    /**
     * Обновляет состояние footer.
     */
    fun update() {
        customview = fb!!.app.bookTextView as FBReaderView.CustomView
        footer = customview!!.footer
        pagePosition = customview!!.pagePosition()
        family = fb!!.app.viewOptions.getFooterOptions().font.value
        tf = AndroidFontUtil.typeface(fb!!.app.SystemInfo, FontEntry.systemEntry(family!!), footer!!.height > 10, false)
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            if (v is FontTextView)
                v.update()
            if (v is TOCMarks)
                v.invalidate()
        }
    }

    override fun invalidate() {
        super.invalidate()
        if (fb == null)
            return // старые android (API10) вызывают invalidate в конструкторе
        update()
    }

    /**
     * View для отображения TOC меток.
     */
    inner class TOCMarks(context: Context) : View(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            val w = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
            val h = footer!!.height
            setMeasuredDimension(w, h)
        }

        override fun onDraw(c: Canvas) {
            val paintContext = ZLAndroidPaintContext(
                fb!!.app.SystemInfo,
                c,
                ZLAndroidPaintContext.Geometry(
                    width,
                    height,
                    width,
                    footer!!.height,
                    0,
                    height
                ),
                0
            )
            footer!!.paint(paintContext)
        }
    }

    /**
     * Базовый TextView с кастомным шрифтом.
     */
    open inner class FontTextView(context: Context) : View(context) {
        protected var text: String = ""
        protected val paint = Paint()
        protected val r = Rect()

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            var w = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
            w = r.width() + paddingLeft + paddingRight
            val h = footer!!.height
            setMeasuredDimension(w, h)
        }

        open fun update() {
            paint.typeface = tf
            paint.textSize = footer!!.height + 2f
            val cProfile = fb!!.app.viewOptions.getColorProfile()
            paint.color = 0xffffff and cProfile.footerNGForegroundOption.value!!.intValue() or 0xff000000.toInt()
        }

        fun updateText(str: String) {
            text = str
            val r = Rect()
            paint.getTextBounds(text, 0, text.length, r)
            if (this.r != r) {
                this.r.set(r)
                requestLayout()
            } else {
                invalidate()
            }
        }

        override fun onDraw(c: Canvas) {
            c.drawText(text, paddingLeft.toFloat(), height / 2 - ((paint.descent() + paint.ascent()) / 2), paint)
        }
    }

    /**
     * Прогресс в формате "текущая/всего страниц".
     */
    inner class ProgressAsPages(context: Context) : FontTextView(context) {
        override fun update() {
            super.update()
            updateText("${pagePosition!!.current}/${pagePosition!!.total}")
        }
    }

    /**
     * Прогресс в процентах.
     */
    inner class ProgressAsPercentage(context: Context) : FontTextView(context) {
        override fun update() {
            super.update()
            updateText("${100 * pagePosition!!.current / pagePosition!!.total}%")
        }
    }

    /**
     * Отображение текущего времени.
     */
    inner class Clock(context: Context) : FontTextView(context) {
        override fun update() {
            super.update()
            updateText(ZLibrary.Instance().getCurrentTimeString())
        }
    }

    /**
     * Отображение уровня батареи.
     */
    inner class Battery(context: Context) : FontTextView(context) {
        override fun update() {
            super.update()
            updateText("${fb!!.app.getBatteryLevel()}%")
        }
    }
}
