/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils

import android.os.Build
import group.pals.android.R
import group.pals.android.lib.ui.filechooser.io.IFile
import group.pals.android.lib.ui.filechooser.io.localfile.ParentFile
import group.pals.android.lib.ui.filechooser.services.IFileProvider
import java.io.File

/**
 * Утилиты для работы с файлами.
 *
 * @author Hai Bison
 * @since v4.3 beta
 */
object FileUtils {

    /**
     * Карта regex-ов для типов файлов, соответствующих resource ID иконок.
     */
    private val mapFileIcons = mapOf(
        MimeTypes.regexFileTypeAudios to R.drawable.afc_file_audio,
        MimeTypes.regexFileTypeVideos to R.drawable.afc_file_video,
        MimeTypes.regexFileTypeImages to R.drawable.afc_file_image,
        MimeTypes.regexFileTypeCompressed to R.drawable.afc_file_compressed,
        MimeTypes.regexFileTypePlainTexts to R.drawable.afc_file_plain_text
    )

    private fun accessDenied(f: IFile): Boolean {
        if (f.isFile()) {
            return !f.canRead()
        }

        if (f !is File) {
            return false
        }

        return if (Build.VERSION.SDK_INT >= 9) {
            !f.canExecute() || !f.canRead()
        } else {
            !f.canRead()
        }
    }

    /**
     * Получает ID ресурса иконки для [IFile].
     *
     * @param file [IFile]
     * @param filterMode режим фильтрации
     * @return ID ресурса иконки
     */
    fun getResIcon(file: IFile?, filterMode: IFileProvider.FilterMode): Int {
        if (file == null || !file.exists()) {
            return R.drawable.afc_item_file
        }

        if (file.isFile()) {
            return R.drawable.afc_item_file
        } else if (file.isDirectory()) {
            if (filterMode == IFileProvider.FilterMode.DirectoriesOnly) {
                if (file is File && !file.canWrite()) {
                    if (file is ParentFile) {
                        return R.drawable.afc_item_folder
                    } else if (accessDenied(file)) {
                        return R.drawable.afc_item_folder
                    } else {
                        return R.drawable.afc_folder_locked
                    }
                } else {
                    return R.drawable.afc_folder
                }
            } else {
                return R.drawable.afc_item_folder
            }
        }

        return R.drawable.afc_item_file
    }

    /**
     * Проверяет доступность файла.
     *
     * @param file [IFile]
     * @param regexp регулярное выражение для имени файла
     * @return `true`, если файл доступен
     */
    fun isAccessible(file: IFile?, regexp: String?): Boolean {
        if (file == null || !file.exists()) {
            return false
        }

        if (accessDenied(file)) {
            return false
        }

        if (file.isFile()) {
            return regexp == null || file.getName().matches(regexp.toRegex())
        } else if (file.isDirectory()) {
            return true
        }

        return false
    }

    /**
     * Проверяет, является ли имя файла допустимым.
     * См. [wiki](http://en.wikipedia.org/wiki/Filename) для дополнительной информации.
     *
     * @param name имя файла
     * @return `true`, если `name` допустимо, и наоборот (если содержит недопустимые символы или является `null`/пустым)
     */
    fun isFilenameValid(name: String?): Boolean {
        return name != null && name.trim().matches("[^\\\\/?%*:|\"<>]+".toRegex())
    }

    /**
     * Удаляет файл или директорию.
     *
     * @param file [IFile]
     * @param fileProvider [IFileProvider]
     * @param recursive если `true` и `file` является директорией, обходит директорию и удаляет все её файлы
     * @return поток, который удаляет файлы
     */
    fun createDeleteFileThread(
        file: IFile,
        fileProvider: IFileProvider,
        recursive: Boolean
    ): Thread {
        return Thread {
            deleteFile(file, fileProvider, recursive)
        }
    }

    private fun deleteFile(file: IFile, fileProvider: IFileProvider, recursive: Boolean) {
        if (Thread.currentThread().isInterrupted) return

        if (file.isFile()) {
            file.delete()
            return
        } else if (!file.isDirectory()) {
            return
        }

        if (!recursive) {
            file.delete()
            return
        }

        try {
            val files = fileProvider.listAllFiles(file)
            if (files == null) {
                file.delete()
                return
            }

            for (f in files) {
                if (Thread.currentThread().isInterrupted) return

                if (f.isFile()) {
                    f.delete()
                } else if (f.isDirectory()) {
                    if (recursive) {
                        deleteFile(f, fileProvider, recursive)
                    } else {
                        f.delete()
                    }
                }
            }

            file.delete()
        } catch (t: Throwable) {
            // Игнорируем ошибки
        }
    }
}
