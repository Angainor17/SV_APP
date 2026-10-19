/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.core.application

import org.fbreader.util.Boolean3
import org.geometerplus.zlibrary.core.util.SystemInfo
import org.geometerplus.zlibrary.core.view.ZLView
import org.geometerplus.zlibrary.core.view.ZLViewWidget
import java.util.Timer
import java.util.TimerTask

abstract class ZLApplication protected constructor(@JvmField val SystemInfo: SystemInfo?) {
    private val myIdToActionMap = HashMap<String, ZLAction>()
    private val myDummyExecutor = object : SynchronousExecutor {
        override fun execute(action: Runnable, uiPostAction: Runnable?) {
            action.run()
        }

        override fun executeAux(key: String, action: Runnable) {
            action.run()
        }
    }
    internal val myPopups = HashMap<String, PopupPanel>()
    private val myTimerTaskPeriods = HashMap<Runnable, Long>()
    private val myTimerTasks = HashMap<Runnable, TimerTask>()
    private val myTimerLock = Any()

    @Volatile
    private var myWindow: ZLApplicationWindow? = null

    @Volatile
    private var myView: ZLView? = null

    private var myActivePopup: PopupPanel? = null

    @Volatile
    private var myTimer: Timer? = null

    init {
        ourInstance = this
    }

    fun setView(view: ZLView?) {
        if (view != null) {
            myView = view
            val widget = getViewWidget()
            if (widget != null) {
                widget.reset()
                widget.repaint()
            }
            hideActivePopup()
        }
    }

    fun getCurrentView(): ZLView? = myView

    fun setWindow(window: ZLApplicationWindow) {
        myWindow = window
    }

    fun initWindow() {
        setView(myView)
    }

    protected fun setTitle(title: String) {
        myWindow?.setWindowTitle(title)
    }

    protected fun showErrorMessage(resourceKey: String) {
        myWindow?.showErrorMessage(resourceKey)
    }

    protected fun showErrorMessage(resourceKey: String, parameter: String) {
        myWindow?.showErrorMessage(resourceKey, parameter)
    }

    protected fun createExecutor(key: String): SynchronousExecutor {
        val window = myWindow
        return window?.createExecutor(key) ?: myDummyExecutor
    }

    protected fun processException(e: Exception) {
        myWindow?.processException(e)
    }

    fun getViewWidget(): ZLViewWidget? = myWindow?.getViewWidget()

    fun onRepaintFinished() {
        myWindow?.refresh()
        for (popup in popupPanels()) {
            popup.update()
        }
    }

    fun hideActivePopup() {
        val popup = myActivePopup
        if (popup != null) {
            popup.hide_()
            myActivePopup = null
        }
    }

    fun showPopup(id: String) {
        hideActivePopup()
        myActivePopup = myPopups[id]
        myActivePopup?.show_()
    }

    fun addAction(actionId: String, action: ZLAction) {
        myIdToActionMap[actionId] = action
    }

    fun removeAction(actionId: String) {
        myIdToActionMap.remove(actionId)
    }

    fun isActionVisible(actionId: String): Boolean {
        val action = myIdToActionMap[actionId]
        return action != null && action.isVisible()
    }

    fun isActionEnabled(actionId: String?): Boolean {
        if (actionId == null) {
            return false
        }
        val action = myIdToActionMap[actionId]
        return action != null && action.isEnabled()
    }

    fun isActionChecked(actionId: String): Boolean3 {
        val action = myIdToActionMap[actionId]
        return if (action != null) action.isChecked() else Boolean3.UNDEFINED
    }

    fun runAction(actionId: String?, vararg params: Any?) {
        if (actionId == null) {
            return
        }
        val action = myIdToActionMap[actionId]
        if (action != null) {
            action.checkAndRun(*params)
        }
    }

    // may be protected
    abstract fun keyBindings(): ZLKeyBindings

    fun runActionByKey(key: Int, longPress: Boolean): Boolean {
        val actionId = keyBindings().getBinding(key, longPress)
        val action = myIdToActionMap[actionId]
        return action != null && action.checkAndRun()
    }

    fun closeWindow(): Boolean {
        onWindowClosing()
        myWindow?.close()
        return true
    }

    open fun onWindowClosing() {
    }

    fun popupPanels(): Collection<PopupPanel> = myPopups.values

    fun getActivePopup(): PopupPanel? = myActivePopup

    fun getPopupById(id: String): PopupPanel? = myPopups[id]

    fun getBatteryLevel(): Int = myWindow?.getBatteryLevel() ?: 0

    private fun addTimerTaskInternal(runnable: Runnable, periodMilliseconds: Long) {
        val task = MyTimerTask(runnable)
        myTimer!!.schedule(task, periodMilliseconds / 2, periodMilliseconds)
        myTimerTasks[runnable] = task
    }

    fun startTimer() {
        synchronized(myTimerLock) {
            if (myTimer == null) {
                myTimer = Timer()
                for (entry in myTimerTaskPeriods.entries) {
                    addTimerTaskInternal(entry.key, entry.value)
                }
            }
        }
    }

    fun stopTimer() {
        synchronized(myTimerLock) {
            if (myTimer != null) {
                myTimer!!.cancel()
                myTimer = null
                myTimerTasks.clear()
            }
        }
    }

    fun addTimerTask(runnable: Runnable, periodMilliseconds: Long) {
        synchronized(myTimerLock) {
            removeTimerTask(runnable)
            myTimerTaskPeriods[runnable] = periodMilliseconds
            if (myTimer != null) {
                addTimerTaskInternal(runnable, periodMilliseconds)
            }
        }
    }

    fun removeTimerTask(runnable: Runnable) {
        synchronized(myTimerLock) {
            val task = myTimerTasks[runnable]
            if (task != null) {
                task.cancel()
                myTimerTasks.remove(runnable)
            }
            myTimerTaskPeriods.remove(runnable)
        }
    }

    interface SynchronousExecutor {
        fun execute(action: Runnable, uiPostAction: Runnable?)

        fun executeAux(key: String, action: Runnable)
    }

    // Action
    abstract class ZLAction {
        open fun isVisible(): Boolean = true

        open fun isEnabled(): Boolean = isVisible()

        open fun isChecked(): Boolean3 = Boolean3.UNDEFINED

        fun checkAndRun(vararg params: Any?): Boolean {
            if (isEnabled()) {
                run(*params)
                return true
            }
            return false
        }

        protected abstract fun run(vararg params: Any?)
    }

    abstract class PopupPanel(@JvmField protected val Application: ZLApplication) {
        init {
            Application.myPopups[getId()] = this
        }

        abstract fun getId(): String

        abstract fun update()

        abstract fun hide_()

        abstract fun show_()
    }

    private class MyTimerTask(private val myRunnable: Runnable) : TimerTask() {
        override fun run() {
            myRunnable.run()
        }
    }

    companion object {
        const val NoAction = "none"

        private var ourInstance: ZLApplication? = null

        @JvmStatic
        fun Instance(): ZLApplication = ourInstance!!
    }
}
