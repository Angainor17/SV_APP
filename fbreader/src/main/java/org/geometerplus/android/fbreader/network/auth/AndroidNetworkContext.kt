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

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

import org.geometerplus.zlibrary.core.network.JsonRequest
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest

import java.net.URI
import java.net.URISyntaxException

abstract class AndroidNetworkContext : ZLNetworkContext() {
    @Volatile private var myConnectivityManager: ConnectivityManager? = null

    override fun authenticate(uri: URI, realm: String, params: Map<String, String>): Map<String, String> {
        if (!"https".equals(uri.scheme, ignoreCase = true)) {
            return errorMap("Connection is not secure")
        }

        var authUrl: String? = null
        val account = getAccountName(uri.host, realm)
        if (account != null) {
            val urlWithAccount = params["auth-url-web-with-email"]
            if (urlWithAccount != null) {
                authUrl = url(uri, urlWithAccount.replace("{email}", account))
            }
        } else {
            authUrl = url(uri, params, "auth-url-web")
        }
        val completeUrl = url(uri, params, "complete-url-web")
        val verificationUrl = url(uri, params, "verification-url")
        if (authUrl == null || completeUrl == null || verificationUrl == null) {
            return errorMap("No data for web authentication")
        }
        return authenticateWeb(uri, realm, authUrl, completeUrl, verificationUrl)
    }

    protected abstract fun getContext(): Context

    protected abstract fun authenticateWeb(uri: URI, realm: String, authUrl: String, completeUrl: String, verificationUrl: String): Map<String, String>

    protected fun errorMap(message: String): Map<String, String> = mapOf("error" to message)

    protected fun errorMap(exception: Throwable): Map<String, String> {
        val message = exception.message
        return errorMap(message ?: exception.javaClass.name)
    }

    protected fun verify(verificationUrl: String): Map<String, String> {
        val result = HashMap<String, String>()
        performQuietly(object : JsonRequest(verificationUrl) {
            override fun processResponse(response: Any?) {
                @Suppress("UNCHECKED_CAST")
                result.putAll(response as Map<String, String>)
            }
        })
        return result
    }

    protected fun url(base: URI, params: Map<String, String>, key: String): String? {
        return url(base, params[key])
    }

    protected fun url(base: URI, path: String?): String? {
        if (path == null) {
            return null
        }
        try {
            val relative = URI(path)
            return if (relative.isAbsolute) null else base.resolve(relative).toString()
        } catch (e: URISyntaxException) {
            return null
        }
    }

    protected val activeNetworkInfo: NetworkInfo?
        get() {
            if (myConnectivityManager == null) {
                myConnectivityManager = getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            }
            return myConnectivityManager?.activeNetworkInfo
        }

    @Throws(ZLNetworkException::class)
    override fun perform(request: ZLNetworkRequest, socketTimeout: Int, connectionTimeout: Int) {
        val info = activeNetworkInfo
        if (info == null || !info.isConnected) {
            throw ZLNetworkException.forCode("networkNotAvailable")
        }

        super.perform(request, socketTimeout, connectionTimeout)
    }
}
