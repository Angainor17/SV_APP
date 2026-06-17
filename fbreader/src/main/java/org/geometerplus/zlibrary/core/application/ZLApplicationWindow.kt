package org.geometerplus.zlibrary.core.application

import org.geometerplus.zlibrary.core.view.ZLViewWidget

interface ZLApplicationWindow {
    fun setWindowTitle(title: String)
    fun showErrorMessage(resourceKey: String)
    fun showErrorMessage(resourceKey: String, parameter: String)
    fun createExecutor(key: String): ZLApplication.SynchronousExecutor
    fun processException(e: Exception)
    fun refresh()
    val viewWidget: ZLViewWidget
    fun close()
    val batteryLevel: Int
}
