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

package org.geometerplus.zlibrary.core.image

abstract class ZLImageProxy : ZLImage {
    @Volatile
    private var myIsSynchronized: Boolean = false

    val isSynchronized: Boolean
        get() {
            if (myIsSynchronized && isOutdated()) {
                myIsSynchronized = false
            }
            return myIsSynchronized
        }

    protected fun setSynchronized() {
        myIsSynchronized = true
    }

    protected open fun isOutdated(): Boolean = false

    open fun startSynchronization(synchronizer: Synchronizer, postAction: Runnable) {
        synchronizer.startImageLoading(this, postAction)
    }

    abstract fun sourceType(): SourceType

    abstract val realImage: ZLImage

    abstract val id: String

    override fun toString(): String = "${javaClass.name}[$id; synchronized=$isSynchronized]"

    enum class SourceType {
        FILE, NETWORK, SERVICE,
    }

    interface Synchronizer {
        fun startImageLoading(image: ZLImageProxy, postAction: Runnable)

        fun synchronize(image: ZLImageProxy, postAction: Runnable)
    }
}
