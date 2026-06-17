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

import android.app.Activity
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import org.apache.http.impl.cookie.BasicClientCookie2
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.zlibrary.ui.android.network.SQLiteCookieDatabase
import java.util.Calendar

internal class WebAuthorisationScreen : Activity() {

    private val myNetworkContext = ActivityNetworkContext(this)

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        requestWindowFeature(Window.FEATURE_PROGRESS)
        SQLiteCookieDatabase.init(this)
        CookieSyncManager.createInstance(applicationContext)
        CookieManager.getInstance().removeAllCookie()
        val intent = intent
        val data = intent.data
        if (data == null || data.host == null) {
            finish()
            return
        }
        val completeUrl = intent.getStringExtra(COMPLETE_URL_KEY) ?: ""

        OrientationUtil.setOrientation(this, intent)
        val view = WebView(this)
        view.settings.javaScriptEnabled = true

        view.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, progress: Int) {
                setProgress(progress * 100)
            }
        }
        view.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                title = url
                if (url != null && url.startsWith(completeUrl)) {
                    val cookies = HashMap<String, String>()
                    val cookieString = CookieManager.getInstance().getCookie(url)
                    if (cookieString != null) {
                        // cookieString is a string like NAME=VALUE [; NAME=VALUE]
                        for (pair in cookieString.split(";")) {
                            val parts = pair.split("=", limit = 2)
                            if (parts.size != 2) {
                                continue
                            }
                            cookies[parts[0].trim()] = parts[1].trim()
                        }
                    }
                    storeCookies(data.host!!, cookies)
                    this@WebAuthorisationScreen.setResult(RESULT_OK)
                    finish()
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler, error: SslError?) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.ECLAIR_MR1) {
                    // hack for auth problem in android 2.1
                    handler.proceed()
                } else {
                    super.onReceivedSslError(view, handler, error)
                }
            }
        }
        setContentView(view)
        view.loadUrl(intent.dataString ?: "")
    }

    private fun storeCookies(host: String, cookies: Map<String, String>) {
        val store = myNetworkContext.cookieStore()

        for ((key, value) in cookies) {
            val c = BasicClientCookie2(key, value)
            c.domain = host
            c.path = "/"
            val expire = Calendar.getInstance()
            expire.add(Calendar.YEAR, 1)
            c.expiryDate = expire.time
            c.isSecure = true
            c.setDiscard(false)
            store.addCookie(c)
        }
    }

    companion object {
        const val COMPLETE_URL_KEY = "android.fbreader.data.complete.url"
    }
}
