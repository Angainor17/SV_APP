package com.github.axet.bookreader.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.github.axet.androidlibrary.widgets.PopupWindowCompat
import com.github.axet.androidlibrary.widgets.ThemeUtils
import com.github.axet.bookreader.R
import org.geometerplus.fbreader.fbreader.TapZoneMap
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions

/**
 * View для отображения активных зон касания.
 */
class ActiveAreasView(context: Context) : RelativeLayout(context) {

    companion object {
        const val PERC = 10000 // точность
        const val BACKGROUND = 0x22333333
    }

    private val maps = HashMap<String, Rect>()
    private val views = HashMap<String, ZoneView>()

    private val names = HashMap<String, String>().apply {
        put("menu", context.getString(R.string.sv_controls_fullscreen))
        put("navigate", context.getString(R.string.sv_controls_navigate))
        put("nextPage", context.getString(R.string.sv_controls_nextpage))
        put("previousPage", context.getString(R.string.sv_controls_prevpage))
        put("brightness", context.getString(R.string.sv_controls_brightness))
    }

    /**
     * Возвращает карту зон касания.
     */
    fun getZoneMap(app: FBReaderView.FBReaderApp): TapZoneMap {
        val prefs: PageTurningOptions = app.PageTurningOptions
        var id = prefs.tapZoneMap.value
        if ("" == id)
            id = if (prefs.horizontal.value) "right_to_left" else "up"
        return TapZoneMap.zoneMap(id)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = measuredWidth
        val h = measuredHeight
        for (k in maps.keys) {
            val r = maps[k]!!
            val v = views[k]!!
            val dp2 = ThemeUtils.dp2px(context, 2f).toInt()
            val lp = v.layoutParams as MarginLayoutParams
            lp.setMargins(w * r.left / PERC + dp2, h * r.top / PERC + dp2, 0, 0)
            lp.width = w * r.width() / PERC - dp2 * 2
            lp.height = h * r.height() / PERC - dp2 * 2
            v.requestLayout()
            if (v.text.measuredWidth > lp.width) {
                val textLp = v.text.layoutParams as MarginLayoutParams
                textLp.width = v.text.measuredWidth
                textLp.height = v.text.measuredHeight
                PopupWindowCompat.setRotationCompat(v.text, 90f)
                v.text.requestLayout()
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    private fun substract(r: Rect) {
        for (s in maps.keys) {
            val z = maps[s]!!
            val a = Rect(r)
            if (a.intersect(z)) {
                val ll = a.left - z.left
                val rr = z.right - a.right
                if (ll != 0 || rr != 0) {
                    if (ll < rr)
                        z.left = a.right
                    else
                        z.right = a.left
                }
                val tt = a.top - z.top
                val bb = z.bottom - a.bottom
                if (tt != 0 || bb != 0) {
                    if (tt < bb)
                        z.top = a.bottom
                    else
                        z.bottom = a.top
                }
            }
        }
    }

    /**
     * Создаёт отображение зон для указанного приложения.
     */
    fun create(app: FBReaderView.FBReaderApp, ww: Int) {
        val zz = getZoneMap(app)
        val w = PERC / zz.width
        val h = PERC / zz.height
        for (x in 0 until zz.width) {
            for (y in 0 until zz.height) {
                val z = zz.getActionByZone(
                    x, y,
                    if (app.MiscOptions.EnableDoubleTap.value) TapZoneMap.Tap.singleNotDoubleTap else TapZoneMap.Tap.singleTap
                )
                if (!app.isActionEnabled(z))
                    continue
                val r = maps[z]
                val xx = w * x // смещение по x
                val yy = h * y // смещение по y
                val c = Rect(xx, yy, xx + w, yy + h)
                if (r == null)
                    maps[z] = c
                else
                    r.union(c)
            }
        }
        if (app.MiscOptions.AllowScreenBrightnessAdjustment.value) {
            val bw = if (app.viewWidget is ScrollWidget)
                (app.viewWidget as ScrollWidget).gesturesListener.brightness.areaWidth * PERC / ww
            else
                PERC / 10 // FBView.onFingerPress
            val r = Rect(0, 0, bw, PERC)
            substract(r)
            maps["brightness"] = r
        }
        for (k in maps.keys) {
            val v = ZoneView(context)
            v.text.text = names[k]
            views[k] = v
            addView(v)
        }
        val lp = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        layoutParams = lp
    }

    /**
     * View для отображения одной зоны.
     */
    inner class ZoneView(context: Context) : FrameLayout(context) {
        val text: TextView
        private val g: GradientDrawable

        init {
            text = TextView(getContext()).apply {
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
            }
            addView(text, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
            g = GradientDrawable().apply {
                cornerRadius = ThemeUtils.dp2px(context, 20f).toFloat() // радиус скругления углов
                gradientType = GradientDrawable.LINEAR_GRADIENT
                setColor(BACKGROUND)
            }
            background = g
            val lp = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            layoutParams = lp
            ViewCompat.setAlpha(text, 0.7f)
        }
    }
}
