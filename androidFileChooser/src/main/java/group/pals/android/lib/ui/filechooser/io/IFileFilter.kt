/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.io

/**
 * Интерфейс для фильтрации объектов [IFile] на основе их имён или
 * других произвольных условий.
 *
 * @author Hai Bison
 * @since v4.3 beta 1
 */
fun interface IFileFilter {

    /**
     * Указывает, должен ли конкретный файл быть включён в список путей.
     *
     * @param pathname абстрактный файл для проверки.
     * @return `true`, если файл должен быть включён, `false` в противном случае.
     */
    fun accept(pathname: IFile): Boolean
}
