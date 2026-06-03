/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.prefs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import group.pals.android.R

/**
 * Удобный класс для работы с настройками.
 *
 * @author Hai Bison
 * @since v4.3 beta
 */
object Prefs {

    /**
     * Этот уникальный ID используется для хранения настроек.
     *
     * @since v4.9 beta
     */
    const val UID = "9795e88b-2ab4-4b81-a548-409091a1e0c6"

    /**
     * Генерирует глобальное имя файла настроек этой библиотеки.
     *
     * @param context [Context] - будет использован для получения контекста приложения.
     * @return глобальное имя файла настроек.
     */
    fun genPreferenceFilename(context: Context): String {
        return "${context.getString(R.string.afc_lib_name)}_$UID"
    }

    /**
     * Получает новые [SharedPreferences].
     *
     * @param context [Context]
     * @return [SharedPreferences]
     */
    fun p(context: Context): SharedPreferences {
        // всегда используем контекст приложения
        return context.applicationContext.getSharedPreferences(
            genPreferenceFilename(context),
            Context.MODE_MULTI_PROCESS
        )
    }

    /**
     * Настраивает `pm` для использования глобального уникального имени файла
     * и глобального режима доступа. Вы должны использовать этот метод,
     * если позволяете пользователю изменять настройки через UI
     * (например, [PreferenceActivity], [PreferenceFragment]...).
     *
     * @param c [Context]
     * @param pm [PreferenceManager]
     * @since v4.9 beta
     */
    fun setupPreferenceManager(c: Context, pm: PreferenceManager) {
        pm.sharedPreferencesMode = Context.MODE_MULTI_PROCESS
        pm.sharedPreferencesName = genPreferenceFilename(c)
    }
}
