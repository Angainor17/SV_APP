/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.services

import android.os.Environment
import group.pals.android.lib.ui.filechooser.io.IFile
import group.pals.android.lib.ui.filechooser.io.IFileFilter
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile
import group.pals.android.lib.ui.filechooser.utils.FileComparator
import java.io.File
import java.io.FileFilter
import java.util.Collections

/**
 * Простой локальный провайдер файлов - как следует из названия.
 * Обрабатывает запросы файлов на локальном устройстве.
 *
 * @author Hai Bison
 * @since v2.1 alpha
 */
class LocalFileProvider : FileProviderService() {

    // Service

    override fun onCreate() {
        // Ничего не делаем
    }

    // IFileProvider

    override fun defaultPath(): IFile {
        val res = Environment.getExternalStorageDirectory()
        return if (res == null) fromPath("/") else LocalFile(res)
    }

    override fun fromPath(pathname: String): IFile {
        return LocalFile(pathname)
    }

    @Deprecated("Используйте listAllFiles")
    override fun listFiles(dir: IFile, hasMoreFiles: BooleanArray?): Array<IFile>? {
        if (!dir.canRead()) return null

        val files = listAllFiles(dir, hasMoreFiles) ?: return null
        return files.toTypedArray()
    }

    @Throws(Exception::class)
    override fun listAllFiles(dir: IFile, hasMoreFiles: BooleanArray?): List<IFile>? {
        if (dir !is File || !dir.canRead()) return null

        if (hasMoreFiles != null && hasMoreFiles.isNotEmpty()) {
            hasMoreFiles[0] = false
        }

        val files = mutableListOf<IFile>()

        try {
            val root = dir.parentFile()
            if (root?.parentFile() == null && accept(root!!)) {
                files.add(root)
            }

            val fileArray = (dir as File).listFiles(object : FileFilter {
                override fun accept(pathname: File): Boolean {
                    val file = LocalFile(pathname)
                    if (!accept(file)) {
                        return false
                    }
                    if (files.size >= maxFileCount) {
                        if (hasMoreFiles != null && hasMoreFiles.isNotEmpty()) {
                            hasMoreFiles[0] = true
                        }
                        return false
                    }
                    files.add(file)
                    return false
                }
            })

            if (fileArray != null) {
                Collections.sort(files, FileComparator(sortType, sortOrder))
                return files
            }

            return null
        } catch (t: Throwable) {
            return null
        }
    }

    @Throws(Exception::class)
    override fun listAllFiles(dir: IFile): List<IFile>? {
        if (dir !is File || !dir.canRead()) return null

        try {
            val files = mutableListOf<IFile>()

            val root = dir.parentFile()
            if (root?.parentFile() == null) {
                files.add(root!!)
            }

            val fileArray = (dir as File).listFiles(object : FileFilter {
                override fun accept(pathname: File): Boolean {
                    files.add(LocalFile(pathname))
                    return false
                }
            })

            if (fileArray != null) return files
            return null
        } catch (t: Throwable) {
            return null
        }
    }

    override fun listAllFiles(dir: IFile, filter: IFileFilter?): List<IFile>? {
        if (dir !is File) return null

        val result = mutableListOf<IFile>()
        try {
            val root = dir.parentFile()
            if (root == null || filter == null || filter.accept(root)) {
                root?.let { result.add(it) }
            }

            val fileArray = (dir as File).listFiles(object : FileFilter {
                override fun accept(pathname: File): Boolean {
                    val file = LocalFile(pathname)
                    if (filter == null || filter.accept(file)) {
                        result.add(file)
                    }
                    return false
                }
            })

            if (fileArray != null) return result
            return null
        } catch (t: Throwable) {
            return null
        }
    }

    override fun accept(pathname: IFile): Boolean {
        if (!isDisplayHiddenFiles && pathname.getName().startsWith(".")) {
            return false
        }

        when (filterMode) {
            IFileProvider.FilterMode.FilesOnly -> {
                if (regexFilenameFilter != null && pathname.isFile()) {
                    return pathname.getName().matches(regexFilenameFilter!!.toRegex())
                }
                return true
            }
            IFileProvider.FilterMode.AnyDirectories,
            IFileProvider.FilterMode.DirectoriesOnly -> {
                return pathname.isDirectory()
            }
            else -> {
                if (regexFilenameFilter != null && pathname.isFile()) {
                    return pathname.getName().matches(regexFilenameFilter!!.toRegex())
                }
                return true
            }
        }
    }
}
