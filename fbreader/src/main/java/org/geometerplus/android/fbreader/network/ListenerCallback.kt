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

package org.geometerplus.android.fbreader.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.authentication.litres.LitResAuthenticationManager
import org.geometerplus.zlibrary.core.network.ZLNetworkException

class ListenerCallback : BroadcastReceiver(), UserRegistrationConstants {

    override fun onReceive(context: Context, intent: Intent) {
        val library = Util.networkLibrary(context)

        if (Util.SIGNIN_ACTION == intent.action) {
            val url = intent.getStringExtra(UserRegistrationConstants.CATALOG_URL)
            val link = library.getLinkByUrl(url)
            if (link != null) {
                val mgr = link.authenticationManager()
                if (mgr is LitResAuthenticationManager) {
                    Thread {
                        try {
                            processSignup(mgr, intent)
                        } catch (e: ZLNetworkException) {
                            e.printStackTrace()
                        }
                    }.start()
                }
            }
        } else {
            library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode)
        }
    }

    companion object {
        @Throws(ZLNetworkException::class)
        private fun processSignup(mgr: LitResAuthenticationManager, data: Intent) {
            mgr.initUser(
                data.getStringExtra(UserRegistrationConstants.USER_REGISTRATION_USERNAME) ?: "",
                data.getStringExtra(UserRegistrationConstants.USER_REGISTRATION_LITRES_SID) ?: "",
                "",
                false
            )
            try {
                mgr.authorise(
                    data.getStringExtra(UserRegistrationConstants.USER_REGISTRATION_USERNAME) ?: "",
                    data.getStringExtra(UserRegistrationConstants.USER_REGISTRATION_PASSWORD) ?: ""
                )
                mgr.initialize()
            } catch (e: ZLNetworkException) {
                mgr.logOut()
                throw e
            }
        }
    }
}
