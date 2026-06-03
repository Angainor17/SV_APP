/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.io.localfile

import java.io.File

/**
 * Обёртка для [File], представляющая родительскую директорию.
 *
 * @author Hai Bison
 * @since v3.2
 */
class ParentFile : LocalFile {

    companion object {
        const val parentSecondName = ".."
        private const val serialVersionUID: Long = 20697593838397580L
    }

    constructor(pathname: String) : super(pathname)

    constructor(file: File) : this(file.getAbsolutePath())

    override fun getSecondName(): String = parentSecondName
}
