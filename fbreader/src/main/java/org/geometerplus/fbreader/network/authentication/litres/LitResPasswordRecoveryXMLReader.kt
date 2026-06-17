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

class LitResPasswordRecoveryXMLReader : LitResAuthenticationXMLReader() {

    override fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean {
        val internedTag = tag.lowercase().intern()
        when (internedTag) {
            TAG_PASSWORD_RECOVERY_FAILED -> {
                val error = attributes.getValue("error")
                when (error) {
                    "1" -> setException(ZLNetworkException.forCode(NetworkException.ERROR_NO_USER_FOR_EMAIL))
                    "2" -> setException(ZLNetworkException.forCode(NetworkException.ERROR_EMAIL_NOT_SPECIFIED))
                    else -> {
                        val comment = attributes.getValue("coment")
                        if (comment != null) {
                            setException(ZLNetworkException(comment))
                        } else {
                            setException(ZLNetworkException.forCode(NetworkException.ERROR_INTERNAL, error))
                        }
                    }
                }
            }
            TAG_PASSWORD_RECOVERY_OK -> {
                // NOP
            }
            else -> {
                setException(ZLNetworkException.forCode(ZLNetworkException.ERROR_SOMETHING_WRONG, LitResUtil.HOST_NAME))
            }
        }
        return true
    }

    companion object {
        private const val TAG_PASSWORD_RECOVERY_OK = "catalit-pass-recover-ok"
        private const val TAG_PASSWORD_RECOVERY_FAILED = "catalit-pass-recover-failed"
    }
}
