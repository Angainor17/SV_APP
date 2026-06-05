package com.github.axet.bookreader.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.LineBackgroundSpan
import java.util.Collections
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Утилита для форматирования текста.
 * Содержит методы для подсветки синтаксиса и форматирования JSON.
 */
object TextFormatter {

    /**
     * Проверяет, пересекается ли текущее совпадение с существующими заменами.
     */
    @JvmStatic
    fun find(reps: ArrayList<Replacement>, m: Matcher): Boolean {
        for (s in reps) {
            if (s.start <= m.start() && m.start() < s.end || s.start <= m.end() && m.end() < s.end)
                return true
        }
        return false
    }

    /**
     * Добавляет замены для найденных совпадений.
     */
    @JvmStatic
    fun replace(reps: ArrayList<Replacement>, json: String, pattern: String, matcher: MatcherReplacement) {
        val p = Pattern.compile(pattern, Pattern.MULTILINE or Pattern.DOTALL)
        val m = p.matcher(json)
        while (m.find()) {
            if (!find(reps, m))
                reps.add(Replacement(m.start(), m.end(), Matcher.quoteReplacement(matcher.run(m))))
        }
    }

    /**
     * Применяет все замены к исходному тексту.
     */
    @JvmStatic
    fun process(reps: ArrayList<Replacement>, json: String, sb: StringBuffer) {
        Collections.sort(reps) { o1, o2 -> o1.start.compareTo(o2.start) }
        var pos = 0
        for (i in reps.indices) {
            val s = reps[i]
            sb.append(json, pos, s.start)
            sb.append(reps[i].text)
            pos = s.end
        }
        sb.append(json, pos, json.length)
    }

    /**
     * Преобразует JSON в HTML для отображения в TextView с подсветкой синтаксиса.
     */
    @JvmStatic
    fun json2textview(json: String): String {
        val reps = ArrayList<Replacement>()
        val sb = StringBuffer()
        sb.append("<tt>")
        replace(reps, json, "\"[^\"]*\"", object : MatcherReplacement {
            override fun run(m: Matcher): String {
                return "<font color=\"green\">${m.group(0)}</font>"
            }
        })
        replace(reps, json, "[0-9]+", object : MatcherReplacement {
            override fun run(m: Matcher): String {
                return "<b><font color=\"blue\">${m.group(0)}</font></b>"
            }
        })
        replace(reps, json, "\n", object : MatcherReplacement {
            override fun run(m: Matcher): String {
                return "<br/>"
            }
        })
        replace(reps, json, "\\s", object : MatcherReplacement {
            override fun run(m: Matcher): String {
                return "&ensp;"
            }
        })
        process(reps, json, sb)
        sb.append("</tt>")
        return sb.toString()
    }

    /**
     * Подсвечивает строку в тексте.
     */
    @JvmStatic
    fun highlightLine(h: SpannableStringBuilder, s: Int, e: Int) {
        h.setSpan(BackgroundColorSpan(Color.RED), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        h.setSpan(HihglightLineSpan(), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /**
     * Интерфейс для обработки совпадений.
     */
    interface MatcherReplacement {
        fun run(m: Matcher): String
    }

    /**
     * Класс для хранения информации о замене.
     */
    class Replacement(
        val start: Int,
        val end: Int,
        val text: String
    )

    /**
     * Span для подсветки всей строки фоном.
     */
    class HihglightLineSpan : LineBackgroundSpan {
        private val paint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.MAGENTA
        }

        override fun drawBackground(
            c: Canvas,
            p: Paint,
            left: Int,
            right: Int,
            top: Int,
            baseline: Int,
            bottom: Int,
            text: CharSequence,
            start: Int,
            end: Int,
            lnum: Int
        ) {
            val clipRect = Rect()
            c.getClipBounds(clipRect)
            clipRect.top = top
            clipRect.bottom = bottom
            c.drawRect(clipRect, paint)
        }
    }
}
