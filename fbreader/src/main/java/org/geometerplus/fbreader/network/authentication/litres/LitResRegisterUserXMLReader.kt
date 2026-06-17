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

import org.geometerplus.fbreader.network.NetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.xml.ZLStringMap

class LitResRegisterUserXMLReader : LitResAuthenticationXMLReader() {

    var sid: String? = null

    override fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean {
        val internedTag = tag.lowercase().intern()
        when (internedTag) {
            TAG_REGISTRATION_FAILED -> {
                val error = attributes.getValue("error")
                when (error) {
                    "1" -> setException(AlreadyInUseException("usernameAlreadyInUse"))
                    "2" -> setException(ZLNetworkException.forCode("usernameNotSpecified"))
                    "3" -> setException(ZLNetworkException.forCode("passwordNotSpecified"))
                    "4" -> setException(ZLNetworkException.forCode("invalidEMail"))
                    "5" -> setException(ZLNetworkException.forCode("tooManyRegistrations"))
                    "6" -> setException(AlreadyInUseException("emailAlreadyInUse"))
                    else -> {
                        val comment = attributes.getValue("coment")
                        if (comment != null) {
                            setException(ZLNetworkException(comment))
                        } else {
                            setException(ZLNetworkException.forCode(NetworkException.ERROR_INTERNAL))
                        }
                    }
                }
            }
            TAG_AUTHORIZATION_OK -> {
                sid = attributes.getValue("sid")
                if (sid == null) {
                    setException(ZLNetworkException.forCode("somethingWrongMessage", LitResUtil.HOST_NAME))
                }
            }
            else -> {
                setException(ZLNetworkException.forCode("somethingWrongMessage", LitResUtil.HOST_NAME))
            }
        }
        return true
    }

    class AlreadyInUseException(code: String) : ZLNetworkException(errorMessage(code))

    companion object {
        private const val TAG_AUTHORIZATION_OK = "catalit-authorization-ok"
        private const val TAG_REGISTRATION_FAILED = "catalit-registration-failed"
    }
}
