/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.services

import group.pals.android.lib.ui.filechooser.io.IFile
import group.pals.android.lib.ui.filechooser.io.IFileFilter

/**
 * Интерфейс для провайдеров [IFile].
 *
 * @author Hai Bison
 * @since v2.1 alpha
 */
interface IFileProvider {

    /**
     * @return `true`, если скрытые файлы отображаются.
     */
    val isDisplayHiddenFiles: Boolean

    /**
     * Устанавливает отображение скрытых файлов.
     */
    var displayHiddenFiles: Boolean

    /**
     * @return регулярное выражение для фильтрации имён файлов.
     */
    val regexFilenameFilter: String?

    /**
     * Устанавливает регулярное выражение для фильтрации имён файлов.
     */
    var regexFilenameFilterValue: String?

    /**
     * @return [FilterMode].
     */
    val filterMode: FilterMode

    /**
     * Устанавливает режим фильтрации.
     */
    var filterModeValue: FilterMode

    /**
     * @return [SortType].
     */
    val sortType: SortType

    /**
     * Устанавливает тип сортировки.
     */
    var sortTypeValue: SortType

    /**
     * @return [SortOrder].
     */
    val sortOrder: SortOrder

    /**
     * Устанавливает порядок сортировки.
     */
    var sortOrderValue: SortOrder

    /**
     * @return максимальное количество файлов для списка.
     */
    val maxFileCount: Int

    /**
     * Устанавливает максимальное количество файлов для списка.
     */
    var maxFileCountValue: Int

    /**
     * Получает путь по умолчанию для провайдера файлов.
     *
     * @return [IFile]
     */
    fun defaultPath(): IFile

    /**
     * Получает путь из строки пути.
     *
     * @param pathname строка пути.
     * @return путь из `pathname`.
     */
    fun fromPath(pathname: String): IFile

    /**
     * Список файлов внутри `dir`, результат должен быть отсортирован по [SortType] и [SortOrder].
     *
     * @param dir корневая директория, для которой нужно получить список файлов.
     * @param hasMoreFiles поскольку Java не позволяет переменные параметры, мы используем этот трюк.
     *                     Установите размер 1. Если `dir` имеет больше файлов, чем максимально допустимое количество,
     *                     элемент вернёт `true`, иначе `false`.
     * @return массив файлов или `null`, если произошло исключение.
     * @throws Exception
     * @deprecated
     */
    @Deprecated("Используйте listAllFiles")
    fun listFiles(dir: IFile, hasMoreFiles: BooleanArray?): Array<IFile>?

    /**
     * Список файлов внутри `dir`, результат должен быть отсортирован по [SortType] и [SortOrder].
     *
     * @param dir корневая директория, для которой нужно получить список файлов.
     * @param hasMoreFiles поскольку Java не позволяет переменные параметры, мы используем этот трюк.
     *                     Установите размер 1. Если `dir` имеет больше файлов, чем максимально допустимое количество,
     *                     элемент вернёт `true`, иначе `false`.
     * @return список файлов или `null`, если произошло исключение.
     * @throws Exception
     * @since v4.0 beta
     */
    @Throws(Exception::class)
    fun listAllFiles(dir: IFile, hasMoreFiles: BooleanArray?): List<IFile>?

    /**
     * Список всех файлов внутри `dir`, **без** фильтрации.
     *
     * @param dir корневая директория, для которой нужно получить список файлов.
     * @return список файлов или `null`, если произошло исключение.
     * @throws Exception
     * @since v4.0 beta
     */
    @Throws(Exception::class)
    fun listAllFiles(dir: IFile): List<IFile>?

    /**
     * Получает список файлов в директории, представленной `dir`.
     * Этот список затем фильтруется через [IFileFilter] и возвращаются соответствующие файлы.
     * Возвращает `null`, если `dir` не является директорией.
     * Если `filter` равен `null`, то все файлы проходят.
     *
     * @param dir [IFile]
     * @param filter фильтр для сопоставления имён, может быть `null`.
     * @return список файлов или `null`.
     * @since v4.3 beta
     */
    fun listAllFiles(dir: IFile, filter: IFileFilter?): List<IFile>?

    /**
     * Фильтрует `pathname` на основе конфигураций этого провайдера файлов.
     *
     * @param pathname [IFile]
     * @return `true`, если `pathname` прошёл все фильтры, `false` в противном случае.
     * @since v4.3 beta
     */
    fun accept(pathname: IFile): Boolean

    /**
     * Параметры сортировки [IFile].
     * Включает:
     * - [SortByName]
     * - [SortBySize]
     * - [SortByDate]
     *
     * @author Hai Bison
     * @since v2.1 alpha
     */
    enum class SortType {
        /** Сортировка по имени (директории сначала, без учёта регистра) */
        SortByName,
        /** Сортировка по размеру (директории сначала) */
        SortBySize,
        /** Сортировка по дате (директории сначала) */
        SortByDate
    }

    /**
     * Порядок сортировки [IFile].
     * Включает:
     * - [Ascending]
     * - [Descending]
     *
     * @author Hai Bison
     * @since v2.1 alpha
     */
    enum class SortOrder(val isAsc: Boolean) {
        /** Сортировка по возрастанию. */
        Ascending(true),
        /** Сортировка по убыванию. */
        Descending(false)
    }

    /**
     * Фильтр [IFile].
     * Включает:
     * - [FilesOnly]
     * - [DirectoriesOnly]
     * - [FilesAndDirectories]
     *
     * @author Hai Bison
     * @since v2.1 alpha
     */
    enum class FilterMode {
        /** Пользователь может выбирать только файлы */
        FilesOnly,
        /** Пользователь может выбирать только директории */
        DirectoriesOnly,
        /** Пользователь может выбирать файлы или директории */
        FilesAndDirectories,
        /** Любые директории */
        AnyDirectories
    }
}
