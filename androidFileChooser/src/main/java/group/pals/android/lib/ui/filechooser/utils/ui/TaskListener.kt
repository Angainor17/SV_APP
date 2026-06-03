/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils.ui

/**
 * Слушатель для любой задачи, которую вы хотите назначить.
 *
 * @author Hai Bison
 * @since v1.8
 */
interface TaskListener {

    /**
     * Будет вызван после завершения задачи.
     *
     * @param ok `true`, если всё в порядке, `false` в противном случае.
     * @param any пользовательские данные, могут быть `null`.
     */
    fun onFinish(ok: Boolean, any: Any?)
}
