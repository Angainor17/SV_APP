package su.sv.commonui.ext

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Форматирует число в строку с разделением пробелами по три знака
 * Пример: "12 000"
 */
fun Int.formatDecimal(): String {
    val formatter = NumberFormat.getInstance(Locale("ru", "RU")) as DecimalFormat
    return formatter.format(this)
}
