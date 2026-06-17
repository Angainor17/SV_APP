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

package org.geometerplus.fbreader.network

import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest

open class NetworkOperationData(
    @JvmField val link: INetworkLink,
    @JvmField var loader: NetworkItemsLoader?
) {
    @JvmField
    @Volatile
    var resumeURI: String? = null

    protected fun clear() {
        resumeURI = null
    }

    fun resume(): ZLNetworkRequest? {
        val request = link.resume(this)
        clear()
        return request
    }
}
