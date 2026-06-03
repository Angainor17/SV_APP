/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils

import group.pals.android.lib.ui.filechooser.io.IFile
import group.pals.android.lib.ui.filechooser.io.localfile.ParentFile
import group.pals.android.lib.ui.filechooser.services.IFileProvider
import org.fbreader.util.NaturalOrderComparator

/**
 * Компаратор для [IFile].
 *
 * Правила:
 * - директории сначала;
 * - остальные свойства основаны на параметрах конструктора.
 *
 * @author Hai Bison
 * @since v1.91
 */
class FileComparator(
    private val sortType: IFileProvider.SortType,
    private val sortOrder: IFileProvider.SortOrder
) : Comparator<IFile> {

    companion object {
        private val naturalOrderComparator = NaturalOrderComparator()
    }

    override fun compare(lhs: IFile, rhs: IFile): Int {
        if ((lhs.isDirectory() && rhs.isDirectory()) || (lhs.isFile() && rhs.isFile())) {
            // По умолчанию сравниваем по имени (без учёта регистра)
            var res = naturalOrderComparator.compare(lhs.getSecondName(), rhs.getSecondName())

            when (sortType) {
                IFileProvider.SortType.SortByName -> {
                    // Сортировка по имени уже выполнена
                }
                IFileProvider.SortType.SortBySize -> {
                    if (lhs.length() > rhs.length()) {
                        res = 1
                    } else if (lhs.length() < rhs.length()) {
                        res = -1
                    }
                }
                IFileProvider.SortType.SortByDate -> {
                    if (lhs.lastModified() > rhs.lastModified()) {
                        res = 1
                    } else if (lhs.lastModified() < rhs.lastModified()) {
                        res = -1
                    }
                }
            }

            // Не влияяем на сортировку родительского элемента (переход в родительскую директорию)
            if (lhs.getSecondName() == ParentFile.parentSecondName || rhs.getSecondName() == ParentFile.parentSecondName) {
                return 1
            } else {
                return if (sortOrder.isAsc) res else -res
            }
        }

        return if (lhs.isDirectory()) -1 else 1
    }
}
