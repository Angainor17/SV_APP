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

package org.geometerplus.android.fbreader.sync

import android.app.Service
import android.net.ConnectivityManager
import org.fbreader.util.ComparisonUtil
import org.geometerplus.android.fbreader.network.auth.ServiceNetworkContext
import org.geometerplus.fbreader.fbreader.options.SyncOptions
import org.geometerplus.fbreader.network.sync.SyncUtil
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest
import org.geometerplus.zlibrary.core.options.ZLEnumOption

internal class SyncNetworkContext(
    service: Service,
    private val mySyncOptions: SyncOptions,
    private val myFeatureOption: ZLEnumOption<SyncOptions.Condition>
) : ServiceNetworkContext(service) {

    @Volatile private var myAccountName: String? = null

    @Throws(ZLNetworkException::class)
    override fun perform(request: ZLNetworkRequest, socketTimeout: Int, connectionTimeout: Int) {
        if (!canPerformRequest()) {
            throw SynchronizationDisabledException()
        }
        val accountName = SyncUtil.getAccountName(this)
        if (!ComparisonUtil.equal(myAccountName, accountName)) {
            reloadCookie()
            myAccountName = accountName
        }
        super.perform(request, socketTimeout, connectionTimeout)
    }

    private fun canPerformRequest(): Boolean {
        if (!mySyncOptions.enabled.value) {
            return false
        }

        return when (myFeatureOption.value) {
            SyncOptions.Condition.never -> false
            SyncOptions.Condition.always -> {
                val info = activeNetworkInfo
                info != null && info.isConnected
            }
            SyncOptions.Condition.viaWifi -> {
                val info = activeNetworkInfo
                info != null && info.isConnected && info.type == ConnectivityManager.TYPE_WIFI
            }
            null -> false
        }
    }
}
