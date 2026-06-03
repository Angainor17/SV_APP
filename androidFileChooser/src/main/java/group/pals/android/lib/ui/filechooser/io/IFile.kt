/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.io

import android.os.Parcelable

/**
 * Интерфейс для "файла", используемого в этой библиотеке.
 * Если вы хотите использовать эту библиотеку для своей файловой системы,
 * реализуйте этот интерфейс в вашем "файле".
 *
 * @author Hai Bison
 * @since v3.2
 */
interface IFile : Parcelable {

    /**
     * Возвращает абсолютный путь этого абстрактного пути.
     */
    fun getAbsolutePath(): String

    /**
     * Возвращает имя файла или директории, обозначенной этим абстрактным путём.
     */
    fun getName(): String

    /**
     * Возвращает второе имя файла (для отображения в списке).
     */
    fun getSecondName(): String

    /**
     * Проверяет, является ли файл директорией.
     */
    fun isDirectory(): Boolean

    /**
     * Проверяет, является ли файл обычным файлом.
     */
    fun isFile(): Boolean

    /**
     * Возвращает длину файла в байтах.
     */
    fun length(): Long

    /**
     * Возвращает время последнего изменения файла.
     */
    fun lastModified(): Long

    /**
     * Возвращает родительскую директорию.
     */
    fun parentFile(): IFile?

    /**
     * Проверяет существование файла.
     */
    fun exists(): Boolean

    /**
     * Создаёт директорию.
     */
    fun mkdir(): Boolean

    /**
     * Удаляет файл.
     */
    fun delete(): Boolean

    /**
     * Сравнивает с другим файлом по пути.
     */
    fun equalsToPath(file: IFile?): Boolean

    /**
     * Создаёт копию файла.
     */
    fun clone(): IFile

    /**
     * Проверяет возможность чтения.
     */
    fun canRead(): Boolean
}
