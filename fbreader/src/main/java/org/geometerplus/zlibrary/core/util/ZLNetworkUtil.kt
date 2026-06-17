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

package org.geometerplus.zlibrary.core.util

import org.geometerplus.zlibrary.core.library.ZLibrary
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder

object ZLNetworkUtil {
    @JvmStatic
    fun url(baseUrl: String, relativePath: String?): String? {
        if (relativePath == null || relativePath.isEmpty()) {
            return relativePath
        }

        var path = relativePath
        try {
            return URL(URL(baseUrl), path).toExternalForm()
        } catch (e: MalformedURLException) {
            // just use our old code below
        }

        if (path.contains("://")
            || path.matches("(?s)^[a-zA-Z][a-zA-Z0-9+-.]*:.*$".toRegex())) {
            return path
        }

        if (path[0] == '/') {
            var index = baseUrl.indexOf("://")
            index = baseUrl.indexOf("/", index + 3)
            return if (index == -1) {
                baseUrl + path
            } else {
                baseUrl.substring(0, index) + path
            }
        } else {
            var index = baseUrl.lastIndexOf('/')
            while (index > 0 && path!!.startsWith("../")) {
                index = baseUrl.lastIndexOf('/', index - 1)
                path = path!!.substring(3)
            }
            return baseUrl.substring(0, index + 1) + path!!
        }
    }

    @JvmStatic
    fun hasParameter(url: String, name: String): Boolean {
        var index = url.lastIndexOf('/') + 1
        if (index == -1 || index >= url.length) {
            return false
        }
        index = url.indexOf('?', index)
        while (index != -1) {
            val start = index + 1
            if (start >= url.length) {
                return false
            }
            val eqIndex = url.indexOf('=', start)
            if (eqIndex == -1) {
                return false
            }
            if (url.substring(start, eqIndex) == name) {
                return true
            }
            index = url.indexOf('&', start)
        }
        return false
    }

    @JvmStatic
    fun appendParameter(url: String, name: String?, value: String?): String {
        if (name == null || value == null) {
            return url
        }
        var value = value.trim()
        if (value.isEmpty()) {
            return url
        }
        value = URLEncoder.encode(value, "utf-8")
        var index = url.indexOf('?', url.lastIndexOf('/') + 1)
        val delimiter = if (index == -1) '?' else '&'
        while (index != -1) {
            val start = index + 1
            val eqIndex = url.indexOf('=', start)
            index = url.indexOf('&', start)
            if (eqIndex != -1 && url.substring(start, eqIndex) == name) {
                val end = if (index != -1) index else url.length
                if (url.substring(eqIndex + 1, end) == value) {
                    return url
                } else {
                    return StringBuilder(url).replace(eqIndex + 1, end, value).toString()
                }
            }
        }
        return StringBuilder(url).append(delimiter).append(name).append('=').append(value).toString()
    }

    @JvmStatic
    fun hostFromUrl(url: String): String {
        var host = url
        var index = host.indexOf("://")
        if (index != -1) {
            host = host.substring(index + 3)
        }
        index = host.indexOf("/")
        if (index != -1) {
            host = host.substring(0, index)
        }
        return host
    }

    @JvmStatic
    fun getUserAgent(): String {
        return String.format(
            "%s/%s (Android %s, %s, %s)",
            "FBReader",
            ZLibrary.Instance()?.versionName ?: "unknown",
            android.os.Build.VERSION.RELEASE,
            android.os.Build.DEVICE,
            android.os.Build.MODEL
        )
    }
}
