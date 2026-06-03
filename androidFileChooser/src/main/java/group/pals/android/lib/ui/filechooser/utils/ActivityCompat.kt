/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils

import android.app.Activity

/**
 * Помощник для доступа к функциям [Activity], представленным в новых уровнях API,
 * обратно совместимым способом.
 *
 * **Примечание:** Сначала проверьте уровень API с помощью [android.os.Build.VERSION.SDK_INT].
 *
 * @author Hai Bison
 * @since v4.3 beta
 */
object ActivityCompat {

    /**
     * @param a [Activity]
     * @see Activity.invalidateOptionsMenu
     */
    fun invalidateOptionsMenu(a: Activity) {
        a.invalidateOptionsMenu()
    }
}
