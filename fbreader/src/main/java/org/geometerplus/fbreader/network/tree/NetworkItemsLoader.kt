/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.network.tree

import org.geometerplus.fbreader.network.NetworkItem
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException

abstract class NetworkItemsLoader(
    @JvmField val networkContext: ZLNetworkContext,
    @JvmField val tree: NetworkCatalogTree
) : Runnable {

    private enum class InterruptionState {
        NONE,
        REQUESTED,
        CONFIRMED
    }

    private val interruptLock = Any()
    private var postRunnable: Runnable? = null
    private var finishedFlag = false
    private var interruptionState = InterruptionState.NONE

    fun start() {
        val loaderThread = Thread(this)
        loaderThread.priority = Thread.MIN_PRIORITY
        loaderThread.start()
    }

    override fun run() {
        val library = tree.library

        synchronized(library) {
            if (library.isLoadingInProgress(tree)) {
                return
            }
            library.storeLoader(tree, this)
        }

        try {
            library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode)

            try {
                doBefore()
            } catch (e: ZLNetworkException) {
                onFinish(e, false)
                return
            }

            try {
                load()
                onFinish(null, isLoadingInterrupted)
            } catch (e: ZLNetworkException) {
                onFinish(e, isLoadingInterrupted)
            }
        } finally {
            library.removeStoredLoader(tree)
            library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode)
            synchronized(this) {
                postRunnable?.run()
                finishedFlag = true
            }
        }
    }

    fun canResumeLoading(): Boolean {
        synchronized(interruptLock) {
            if (interruptionState == InterruptionState.REQUESTED) {
                interruptionState = InterruptionState.NONE
            }
            return interruptionState == InterruptionState.NONE
        }
    }

    protected val isLoadingInterrupted: Boolean
        get() = synchronized(interruptLock) { interruptionState == InterruptionState.CONFIRMED }

    fun interrupt() {
        synchronized(interruptLock) {
            if (interruptionState == InterruptionState.NONE) {
                interruptionState = InterruptionState.REQUESTED
            }
        }
    }

    fun confirmInterruption(): Boolean {
        synchronized(interruptLock) {
            if (interruptionState == InterruptionState.REQUESTED) {
                interruptionState = InterruptionState.CONFIRMED
            }
            return interruptionState == InterruptionState.CONFIRMED
        }
    }

    open fun onNewItem(item: NetworkItem) {
        tree.addItem(item)
    }

    @Synchronized
    fun setPostRunnable(action: Runnable) {
        if (finishedFlag) {
            action.run()
        } else {
            postRunnable = action
        }
    }

    protected abstract fun onFinish(exception: ZLNetworkException?, interrupted: Boolean)

    @Throws(ZLNetworkException::class)
    protected abstract fun doBefore()

    @Throws(ZLNetworkException::class)
    protected abstract fun load()
}
