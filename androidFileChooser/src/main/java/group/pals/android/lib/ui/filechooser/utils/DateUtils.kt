/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils

import android.content.Context
import group.pals.android.R
import group.pals.android.lib.ui.filechooser.prefs.DisplayPrefs.FileTimeDisplay
import java.util.Calendar

/**
 * Утилиты для работы с датой.
 *
 * @author Hai Bison
 * @since v4.7 beta
 */
object DateUtils {

    /**
     * Используется с методами форматирования [android.text.format.DateUtils].
     * Например: "10:01 AM".
     */
    const val FORMAT_SHORT_TIME = android.text.format.DateUtils.FORMAT_12HOUR or
            android.text.format.DateUtils.FORMAT_SHOW_TIME

    /**
     * Используется с методами форматирования [android.text.format.DateUtils].
     * Например: "Oct 01".
     */
    const val FORMAT_MONTH_AND_DAY = android.text.format.DateUtils.FORMAT_ABBREV_MONTH or
            android.text.format.DateUtils.FORMAT_SHOW_DATE or
            android.text.format.DateUtils.FORMAT_NO_YEAR

    /**
     * Используется с методами форматирования [android.text.format.DateUtils].
     * Например: "2012".
     */
    const val FORMAT_YEAR = android.text.format.DateUtils.FORMAT_SHOW_YEAR

    /**
     * Форматирует дату.
     *
     * @param context [Context]
     * @param millis время в миллисекундах.
     * @param fileTimeDisplay [FileTimeDisplay]
     * @return отформатированная строка.
     */
    fun formatDate(context: Context, millis: Long, fileTimeDisplay: FileTimeDisplay): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return formatDate(context, cal, fileTimeDisplay)
    }

    /**
     * Форматирует дату.
     *
     * @param context [Context]
     * @param date [Calendar]
     * @param fileTimeDisplay [FileTimeDisplay]
     * @return отформатированная строка для локального чтения человеком.
     */
    fun formatDate(context: Context, date: Calendar, fileTimeDisplay: FileTimeDisplay): String {
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)

        return when {
            android.text.format.DateUtils.isToday(date.timeInMillis) -> {
                android.text.format.DateUtils.formatDateTime(
                    context,
                    date.timeInMillis,
                    FORMAT_SHORT_TIME
                )
            }

            date.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    date.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> {
                "${context.getString(R.string.afc_yesterday)}, ${
                    android.text.format.DateUtils.formatDateTime(
                        context,
                        date.timeInMillis,
                        FORMAT_SHORT_TIME
                    )
                }"
            }

            date.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) -> {
                if (fileTimeDisplay.isShowTimeForOldDaysThisYear) {
                    android.text.format.DateUtils.formatDateTime(
                        context,
                        date.timeInMillis,
                        FORMAT_SHORT_TIME or FORMAT_MONTH_AND_DAY
                    )
                } else {
                    android.text.format.DateUtils.formatDateTime(
                        context,
                        date.timeInMillis,
                        FORMAT_MONTH_AND_DAY
                    )
                }
            }

            else -> {
                if (fileTimeDisplay.isShowTimeForOldDays) {
                    android.text.format.DateUtils.formatDateTime(
                        context,
                        date.timeInMillis,
                        FORMAT_SHORT_TIME or FORMAT_MONTH_AND_DAY or FORMAT_YEAR
                    )
                } else {
                    android.text.format.DateUtils.formatDateTime(
                        context,
                        date.timeInMillis,
                        FORMAT_MONTH_AND_DAY or FORMAT_YEAR
                    )
                }
            }
        }
    }
}
