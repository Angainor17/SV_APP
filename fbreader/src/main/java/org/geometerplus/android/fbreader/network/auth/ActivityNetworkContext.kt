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

package org.geometerplus.android.fbreader.network.auth

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

import org.geometerplus.android.fbreader.network.NetworkLibraryActivity
import org.geometerplus.android.util.OrientationUtil

import java.net.URI

internal class ActivityNetworkContext(private val myActivity: Activity) : AndroidNetworkContext() {

    @Volatile private var myAuthorizationConfirmed = false
    @Volatile private var myAccountName: String? = null

    override fun getContext(): Context = myActivity

    @Synchronized
    fun onResume() {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        (this as java.lang.Object).notifyAll()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        var processed = true
        try {
            when (requestCode) {
                NetworkLibraryActivity.REQUEST_ACCOUNT_PICKER -> {
                    if (resultCode == Activity.RESULT_OK && data != null) {
                        myAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                    } else {
                        myAccountName = null
                    }
                }
                NetworkLibraryActivity.REQUEST_AUTHORISATION -> {
                    if (resultCode == Activity.RESULT_OK) {
                        myAuthorizationConfirmed = true
                    }
                }
                NetworkLibraryActivity.REQUEST_WEB_AUTHORISATION_SCREEN -> {
                    cookieStore().reset()
                }
                else -> processed = false
            }
        } finally {
            if (processed) {
                synchronized(this) {
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    (this as java.lang.Object).notifyAll()
                }
            }
            return processed
        }
    }

    override fun authenticateWeb(uri: URI, realm: String, authUrl: String, completeUrl: String, verificationUrl: String): Map<String, String> {
        System.err.println("+++ WEB AUTH +++")
        val intent = Intent(myActivity, WebAuthorisationScreen::class.java)
        intent.data = Uri.parse(authUrl)
        intent.putExtra(WebAuthorisationScreen.COMPLETE_URL_KEY, completeUrl)
        startActivityAndWait(intent, NetworkLibraryActivity.REQUEST_WEB_AUTHORISATION_SCREEN)
        System.err.println("--- WEB AUTH ---")
        return verify(verificationUrl)
    }

    private fun startActivityAndWait(intent: Intent, requestCode: Int) {
        synchronized(this) {
            OrientationUtil.startActivityForResult(myActivity, intent, requestCode)
            try {
                (this as Object).wait()
            } catch (e: InterruptedException) {
            }
        }
    }
}
