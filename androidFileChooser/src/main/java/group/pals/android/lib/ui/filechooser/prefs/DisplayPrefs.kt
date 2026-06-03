/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.prefs

import android.content.Context
import group.pals.android.R
import group.pals.android.lib.ui.filechooser.FileChooserActivity.ViewType
import group.pals.android.lib.ui.filechooser.services.IFileProvider.SortType

/**
 * Настройки отображения.
 *
 * @author Hai Bison
 * @since v4.3 beta
 */
object DisplayPrefs {

    /**
     * Время задержки для ожидания других потоков внутри потока... В миллисекундах.
     */
    const val DELAY_TIME_WAITING_THREADS = 10

    /**
     * Ёмкость истории по умолчанию. Поскольку нам нужно проверять дубликаты
     * перед показом списка истории, это значение должно быть небольшим.
     */
    const val DEF_HISTORY_CAPACITY = 51

    /**
     * Получает тип представления.
     *
     * @param c [Context]
     * @return [ViewType]
     */
    fun getViewType(c: Context): ViewType {
        return if (ViewType.List.ordinal == Prefs.p(c).getInt(
                c.getString(R.string.afc_pkey_display_view_type),
                c.resources.getInteger(R.integer.afc_pkey_display_view_type_def)
            )
        ) ViewType.List else ViewType.Grid
    }

    /**
     * Устанавливает тип представления.
     *
     * @param c [Context]
     * @param v [ViewType], если `null`, будет использовано значение по умолчанию.
     */
    fun setViewType(c: Context, v: ViewType?) {
        val key = c.getString(R.string.afc_pkey_display_view_type)
        if (v == null) {
            Prefs.p(c).edit().putInt(key, c.resources.getInteger(R.integer.afc_pkey_display_view_type_def)).commit()
        } else {
            Prefs.p(c).edit().putInt(key, v.ordinal).commit()
        }
    }

    /**
     * Получает тип сортировки.
     *
     * @param c [Context]
     * @return [SortType]
     */
    fun getSortType(c: Context): SortType {
        for (s in SortType.entries) {
            if (s.ordinal == Prefs.p(c).getInt(
                    c.getString(R.string.afc_pkey_display_sort_type),
                    c.resources.getInteger(R.integer.afc_pkey_display_sort_type_def)
                )
            ) {
                return s
            }
        }
        return SortType.SortByName
    }

    /**
     * Устанавливает [SortType].
     *
     * @param c [Context]
     * @param v [SortType], если `null`, будет использовано значение по умолчанию.
     */
    fun setSortType(c: Context, v: SortType?) {
        val key = c.getString(R.string.afc_pkey_display_sort_type)
        if (v == null) {
            Prefs.p(c).edit().putInt(key, c.resources.getInteger(R.integer.afc_pkey_display_sort_type_def)).commit()
        } else {
            Prefs.p(c).edit().putInt(key, v.ordinal).commit()
        }
    }

    /**
     * Получает направление сортировки.
     *
     * @param c [Context]
     * @return `true`, если сортировка по возрастанию, `false` в противном случае.
     */
    fun isSortAscending(c: Context): Boolean {
        return Prefs.p(c).getBoolean(
            c.getString(R.string.afc_pkey_display_sort_ascending),
            c.resources.getBoolean(R.bool.afc_pkey_display_sort_ascending_def)
        )
    }

    /**
     * Устанавливает направление сортировки.
     *
     * @param c [Context]
     * @param v [Boolean], если `null`, будет использовано значение по умолчанию.
     */
    fun setSortAscending(c: Context, v: Boolean?) {
        val value = v ?: c.resources.getBoolean(R.bool.afc_pkey_display_sort_ascending_def)
        Prefs.p(c).edit().putBoolean(c.getString(R.string.afc_pkey_display_sort_ascending), value).commit()
    }

    /**
     * Проверяет настройку показа времени для старых дней этого года.
     * По умолчанию `false`.
     *
     * @param c [Context]
     * @return `true` или `false`.
     * @since v4.7 beta
     */
    fun isShowTimeForOldDaysThisYear(c: Context): Boolean {
        return Prefs.p(c).getBoolean(
            c.getString(R.string.afc_pkey_display_show_time_for_old_days_this_year),
            c.resources.getBoolean(R.bool.afc_pkey_display_show_time_for_old_days_this_year_def)
        )
    }

    /**
     * Включает или отключает показ времени старых дней этого года.
     *
     * @param c [Context]
     * @param v ваш флаг. Если `null`, будет использовано значение по умолчанию (`false`).
     * @since v4.7 beta
     */
    fun setShowTimeForOldDaysThisYear(c: Context, v: Boolean?) {
        val value = v ?: c.resources.getBoolean(R.bool.afc_pkey_display_show_time_for_old_days_this_year_def)
        Prefs.p(c).edit().putBoolean(
            c.getString(R.string.afc_pkey_display_show_time_for_old_days_this_year),
            value
        ).commit()
    }

    /**
     * Проверяет настройку показа времени для старых дней прошлого года и старше.
     * По умолчанию `false`.
     *
     * @param c [Context]
     * @return `true` или `false`.
     * @since v4.7 beta
     */
    fun isShowTimeForOldDays(c: Context): Boolean {
        return Prefs.p(c).getBoolean(
            c.getString(R.string.afc_pkey_display_show_time_for_old_days),
            c.resources.getBoolean(R.bool.afc_pkey_display_show_time_for_old_days_def)
        )
    }

    /**
     * Включает или отключает показ времени старых дней прошлого года и старше.
     *
     * @param c [Context]
     * @param v ваш флаг. Если `null`, будет использовано значение по умолчанию (`false`).
     * @since v4.7 beta
     */
    fun setShowTimeForOldDays(c: Context, v: Boolean?) {
        val value = v ?: c.resources.getBoolean(R.bool.afc_pkey_display_show_time_for_old_days_def)
        Prefs.p(c).edit().putBoolean(
            c.getString(R.string.afc_pkey_display_show_time_for_old_days),
            value
        ).commit()
    }

    /**
     * Проверяет, включено ли запоминание последнего местоположения.
     *
     * @param c [Context]
     * @return `true`, если запоминание последнего местоположения включено.
     * @since v4.7 beta
     */
    fun isRememberLastLocation(c: Context): Boolean {
        return Prefs.p(c).getBoolean(
            c.getString(R.string.afc_pkey_display_remember_last_location),
            c.resources.getBoolean(R.bool.afc_pkey_display_remember_last_location_def)
        )
    }

    /**
     * Включает или отключает запоминание последнего местоположения.
     *
     * @param c [Context]
     * @param v ваш флаг. Если `null`, будет использовано значение по умолчанию (`true`).
     * @since v4.7 beta
     */
    fun setRememberLastLocation(c: Context, v: Boolean?) {
        val value = v ?: c.resources.getBoolean(R.bool.afc_pkey_display_remember_last_location_def)
        Prefs.p(c).edit().putBoolean(
            c.getString(R.string.afc_pkey_display_remember_last_location),
            value
        ).commit()
    }

    /**
     * Получает последнее местоположение.
     *
     * @param c [Context]
     * @return последнее местоположение или `null`, если недоступно.
     * @since v4.7 beta
     */
    fun getLastLocation(c: Context): String? {
        return Prefs.p(c).getString(c.getString(R.string.afc_pkey_display_last_location), null)
    }

    /**
     * Устанавливает последнее местоположение.
     *
     * @param c [Context]
     * @param v последнее местоположение.
     */
    fun setLastLocation(c: Context, v: String?) {
        Prefs.p(c).edit().putString(c.getString(R.string.afc_pkey_display_last_location), v).commit()
    }

    /*
     * ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ
     */

    /**
     * Опции отображения времени файла.
     *
     * @author Hai Bison
     * @see isShowTimeForOldDaysThisYear
     * @see isShowTimeForOldDays
     * @since v4.9 beta
     */
    class FileTimeDisplay(
        var isShowTimeForOldDaysThisYear: Boolean,
        var isShowTimeForOldDays: Boolean
    )
}
