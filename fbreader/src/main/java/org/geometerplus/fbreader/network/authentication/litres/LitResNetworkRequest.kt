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

package org.geometerplus.fbreader.network.authentication.litres

import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest

import java.io.IOException
import java.io.InputStream

class LitResNetworkRequest(url: String, val reader: LitResAuthenticationXMLReader) : ZLNetworkRequest.PostWithMap(clean(url)) {

    init {
        val index = url.indexOf('?')
        if (index != -1) {
            for (param in url.substring(index + 1).split("&")) {
                val pp = param.split("=")
                if (pp.size == 2) {
                    addPostParameter(pp[0], pp[1])
                }
            }
        }
    }

    companion object {
        fun clean(url: String): String {
            val index = url.indexOf('?')
            return if (index != -1) url.substring(0, index) else url
        }
    }

    @Throws(IOException::class, ZLNetworkException::class)
    override fun handleStream(inputStream: InputStream, length: Int) {
        reader.read(inputStream)
        reader.exception?.let { throw it }
    }
}
