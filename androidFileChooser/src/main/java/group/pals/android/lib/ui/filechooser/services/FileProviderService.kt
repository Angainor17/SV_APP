/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Базовый сервис для провайдера файлов.
 *
 * @author Hai Bison
 * @since v3.1
 */
abstract class FileProviderService : Service(), IFileProvider {

    // Объект, который получает взаимодействия от клиентов.
    private val binder = LocalBinder()

    private var _displayHiddenFiles = false
    private var _regexFilenameFilter: String? = null
    private var _filterMode: IFileProvider.FilterMode = IFileProvider.FilterMode.FilesOnly
    private var _maxFileCount = 1024
    private var _sortType: IFileProvider.SortType = IFileProvider.SortType.SortByName
    private var _sortOrder: IFileProvider.SortOrder = IFileProvider.SortOrder.Ascending

    // Service

    override fun onBind(intent: Intent): IBinder = binder

    // IFileProvider

    override val isDisplayHiddenFiles: Boolean
        get() = _displayHiddenFiles

    override var displayHiddenFiles: Boolean
        get() = _displayHiddenFiles
        set(value) {
            _displayHiddenFiles = value
        }

    override val regexFilenameFilter: String?
        get() = _regexFilenameFilter

    override var regexFilenameFilterValue: String?
        get() = _regexFilenameFilter
        set(value) {
            _regexFilenameFilter = value
        }

    override val filterMode: IFileProvider.FilterMode
        get() = _filterMode

    override var filterModeValue: IFileProvider.FilterMode
        get() = _filterMode
        set(value) {
            _filterMode = value
        }

    override val sortType: IFileProvider.SortType
        get() = _sortType

    override var sortTypeValue: IFileProvider.SortType
        get() = _sortType
        set(value) {
            _sortType = value
        }

    override val sortOrder: IFileProvider.SortOrder
        get() = _sortOrder

    override var sortOrderValue: IFileProvider.SortOrder
        get() = _sortOrder
        set(value) {
            _sortOrder = value
        }

    override val maxFileCount: Int
        get() = _maxFileCount

    override var maxFileCountValue: Int
        get() = _maxFileCount
        set(value) {
            _maxFileCount = value
        }

    /**
     * Класс для доступа клиентов. Поскольку мы знаем, что этот сервис всегда
     * работает в том же процессе, что и его клиенты, нам не нужно иметь дело с IPC.
     */
    inner class LocalBinder : Binder() {
        val service: IFileProvider
            get() = this@FileProviderService
    }
}
