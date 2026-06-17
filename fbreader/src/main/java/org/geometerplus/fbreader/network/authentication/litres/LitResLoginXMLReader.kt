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

import org.geometerplus.zlibrary.core.network.ZLNetworkAuthenticationException
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.xml.ZLStringMap

class LitResLoginXMLReader : LitResAuthenticationXMLReader() {
    var firstName: String? = null
    var lastName: String? = null
    var userId: String? = null
    var sid: String? = null
    var canRebill: Boolean = false

    override fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean {
        val internedTag = tag.lowercase().intern()
        when (internedTag) {
            TAG_AUTHORIZATION_FAILED -> {
                setException(ZLNetworkAuthenticationException())
            }
            TAG_AUTHORIZATION_OK -> {
                firstName = attributes.getValue("first-name")
                lastName = attributes.getValue("first-name")
                userId = attributes.getValue("user-id")
                sid = attributes.getValue("sid")
                var stringCanRebill = attributes.getValue("can-rebill")
                if (stringCanRebill == null) {
                    stringCanRebill = attributes.getValue("can_rebill")
                }
                canRebill = stringCanRebill != null && "0" != stringCanRebill && !"no".equals(stringCanRebill, ignoreCase = true)
            }
            else -> {
                setException(ZLNetworkException.forCode(ZLNetworkException.ERROR_SOMETHING_WRONG, LitResUtil.HOST_NAME))
            }
        }
        return true
    }

    companion object {
        private const val TAG_AUTHORIZATION_OK = "catalit-authorization-ok"
        private const val TAG_AUTHORIZATION_FAILED = "catalit-authorization-failed"
    }
}
