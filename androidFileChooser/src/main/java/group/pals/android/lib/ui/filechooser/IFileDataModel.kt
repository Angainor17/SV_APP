/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser

import group.pals.android.lib.ui.filechooser.io.IFile

/**
 * Этот класс используется для хранения данных ([IFile]) в [IFileAdapter].
 *
 * @author Hai Bison
 */
class IFileDataModel(
    /** Файл, который хранит этот контейнер. */
    val file: IFile
) {

    /** Статус выбора элемента в списке. */
    var isSelected: Boolean = false
        set(selected) {
            if (field != selected)
                field = selected
        }

    /** Помечен ли элемент для удаления. */
    var isTobeDeleted: Boolean = false
        set(tobeDeleted) {
            if (field != tobeDeleted)
                field = tobeDeleted
        }
}
