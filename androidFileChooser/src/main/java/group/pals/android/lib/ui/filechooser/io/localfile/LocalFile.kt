/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.io.localfile

import android.os.Parcel
import group.pals.android.lib.ui.filechooser.io.IFile
import group.pals.android.lib.ui.filechooser.utils.history.History
import java.io.File

/**
 * Обёртка для [File].
 *
 * @author Hai Bison
 * @since v3.2
 */
open class LocalFile : File, IFile {

    companion object {
        private const val serialVersionUID: Long = 20697593838397580L

        @JvmField
        val CREATOR = object : android.os.Parcelable.Creator<LocalFile> {
            override fun createFromParcel(parcel: Parcel): LocalFile {
                return LocalFile(parcel)
            }

            override fun newArray(size: Int): Array<LocalFile?> {
                return arrayOfNulls(size)
            }
        }
    }

    constructor(pathname: String) : super(pathname)

    constructor(file: File) : this(file.getAbsolutePath())

    private constructor(parcel: Parcel) : this(parcel.readString() ?: "")

    // IFile implementation - делегируем к методам File
    override fun getSecondName(): String = name

    override fun parentFile(): IFile? {
        return parent?.let { ParentFile(it) }
    }

    /**
     * По умолчанию [File] сравнивается с другим по пути. Так что если два разных [File]
     * имеют одинаковый путь, они равны. Но чтобы [History] работал, мы не делаем этого.
     *
     * Поэтому мы переопределяем этот метод :-) Результат - сравнение оператором `==`.
     */
    override fun equals(other: Any?): Boolean = this === other

    override fun equalsToPath(file: IFile?): Boolean {
        return file != null && absolutePath == file.getAbsolutePath()
    }

    override fun clone(): IFile {
        return LocalFile(absolutePath)
    }

    // Parcelable

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(absolutePath)
    }
}
