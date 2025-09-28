package su.sv.commonui.managers

import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** Пример: "20 окт." */
private const val SHORT_DATE_TEMPLATE = "d MMM"

/** Пример: "17:40" */
private const val SHORT_TIME_TEMPLATE = "HH:mm"

class DateFormatter @Inject constructor() {

    private val shortDateFormat = createFormat(SHORT_DATE_TEMPLATE)
    private val shortTimeFormat = createFormat(SHORT_TIME_TEMPLATE)

    /**
     * Форматирует дату
     * @see SHORT_DATE_TEMPLATE
     *
     * Пример: 20 окт
     */
    fun formatShortDateOnly(date: LocalDateTime?): String {
        if (date == null) return ""

        return shortDateFormat.format(date).replace(".", "")
    }

    /**
     * Форматирует время
     * @see SHORT_TIME_TEMPLATE
     *
     * Пример: 17:40
     */
    fun formatShortTimeOnly(date: LocalDateTime?): String {
        if (date == null) return ""

        return shortTimeFormat.format(date)
    }

    private fun createFormat(template: String) =
        DateTimeFormatter.ofPattern(template, Locale.getDefault())
}

