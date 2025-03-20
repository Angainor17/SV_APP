package su.sv.commonui.managers

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** Пример: 2019-12-21 */
private const val DATE_ONLY_TEMPLATE = "yyyy-MM-dd"

/** Пример: "20 окт. 2024" */
private const val FULL_DATE_TEMPLATE = "d MMM YYYY"

class DateFormatter @Inject constructor() {

    private val dateOnlyFormat = createFormat(DATE_ONLY_TEMPLATE)
    private val fullDateFormat = createFormat(FULL_DATE_TEMPLATE)

    /**
     * Форматирует дату
     * @see DATE_ONLY_TEMPLATE
     *
     * Пример: 2019-12-21
     */
    fun formatDateOnly(date: LocalDate): String {
        return dateOnlyFormat.format(date)
    }

    /**
     * Форматирует дату
     * @see DATE_ONLY_TEMPLATE
     *
     * Пример: 2019-12-21
     */
    fun formatDateFull(date: LocalDate): String {
        return fullDateFormat.format(date)
    }

    private fun createFormat(template: String) =
        DateTimeFormatter.ofPattern(template, Locale.getDefault())
}

