package com.github.axet.bookreader.widgets

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.ColorUtils
import com.github.axet.androidlibrary.widgets.PopupWindowCompat
import com.github.axet.androidlibrary.widgets.ThemeUtils
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.Storage

/**
 * Popup для редактирования закладки.
 */
open class BookmarkPopup(
    private val v: View,
    private val l: Storage.Bookmark,
    private val bmv: ArrayList<View>
) {
    companion object {
        val COLORS = intArrayOf(
            0xffff0000.toInt(),
            0xffFF8000.toInt(),
            0xffFFFF00.toInt(),
            0xff00FF00.toInt(),
            0xff0000FF.toInt(),
            0xff3F00FF.toInt(),
            0xff7F00FF.toInt()
        )
    }

    val context: Context = v.context
    val w: PopupWindow

    init {
        val inflater = LayoutInflater.from(context)
        val name = EditText(context).apply {
            setText(l.name)
            if (l.name != null)
                setSelection(l.name!!.length)
        }
        val ll = object : LinearLayout(context) {
            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
                l.name = name.text.toString()
                l.last = System.currentTimeMillis()
                onDismiss()
            }
        }.apply {
            orientation = LinearLayout.VERTICAL
        }
        val hh = LinearLayout(context).apply {
            val dp5 = ThemeUtils.dp2px(context, 5f).toInt()
            setPadding(dp5, dp5, dp5, dp5)
        }
        val dp1 = ThemeUtils.dp2px(context, 1f).toInt()
        for (c in COLORS) {
            val color = inflater.inflate(R.layout.bm_color, null)
            val image = color.findViewById<ImageView>(R.id.color)
            image.setColorFilter(c)
            val check = color.findViewById<ImageView>(R.id.checkbox)
            check.visibility = if (l.color == c) View.VISIBLE else View.GONE
            check.setColorFilter(if (ColorUtils.calculateLuminance(c) < 0.5f) Color.WHITE else Color.GRAY)
            color.tag = c
            color.setOnClickListener {
                l.color = color.tag as Int
                l.last = System.currentTimeMillis()
                for (b in bmv)
                    b.setBackgroundColor(SelectionView.SELECTION_ALPHA shl 24 or (l.color and 0xffffff))
                for (i in 0 until hh.childCount) {
                    val childColor = hh.getChildAt(i)
                    val childCheck = childColor.findViewById<ImageView?>(R.id.checkbox)
                    childCheck?.visibility = if ((childColor.tag as? Int) == l.color) View.VISIBLE else View.GONE
                }
                onSelect(l.color)
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp1, dp1, dp1, dp1)
            }
            hh.addView(color, lp)
        }
        w = PopupWindow()
        val trash = ImageView(context).apply {
            setImageResource(com.github.axet.androidlibrary.R.drawable.ic_close_black_24dp)
            setColorFilter(ThemeUtils.getThemeColor(context, com.github.axet.androidlibrary.R.attr.colorAccent))
            setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle(R.string.delete_bookmark)
                    .setMessage(com.github.axet.androidlibrary.R.string.are_you_sure)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        onDelete(l)
                        w.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
        val trashLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            setMargins(dp1, dp1, dp1, dp1)
        }
        hh.addView(trash, trashLp)
        ll.addView(hh)
        ll.addView(name)
        w.contentView = ll
        w.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    }

    fun show() {
        PopupWindowCompat.showAsTooltip(w, v, Gravity.BOTTOM)
    }

    open fun onDelete(l: Storage.Bookmark) {}
    open fun onDismiss() {}
    open fun onSelect(color: Int) {}
}
