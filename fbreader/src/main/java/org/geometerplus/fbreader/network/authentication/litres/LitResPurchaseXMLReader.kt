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

import org.geometerplus.fbreader.network.AlreadyPurchasedException
import org.geometerplus.fbreader.network.NetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkAuthenticationException
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.xml.ZLStringMap

internal class LitResPurchaseXMLReader : LitResAuthenticationXMLReader() {

    var account: String? = null
    var bookId: String? = null

    override fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean {
        val internedTag = tag.lowercase().intern()
        if (TAG_AUTHORIZATION_FAILED == internedTag) {
            setException(ZLNetworkAuthenticationException())
        } else {
            account = attributes.getValue("account")
            bookId = attributes.getValue("art")
            when (internedTag) {
                TAG_PURCHASE_OK -> {
                    // nop
                }
                TAG_PURCHASE_FAILED -> {
                    val error = attributes.getValue("error")
                    when (error) {
                        "1" -> setException(ZLNetworkException.forCode(NetworkException.ERROR_PURCHASE_NOT_ENOUGH_MONEY))
                        "2" -> setException(ZLNetworkException.forCode(NetworkException.ERROR_PURCHASE_MISSING_BOOK))
                        "3" -> setException(AlreadyPurchasedException())
                        else -> setException(ZLNetworkException.forCode(NetworkException.ERROR_INTERNAL))
                    }
                }
                else -> {
                    setException(ZLNetworkException.forCode(ZLNetworkException.ERROR_SOMETHING_WRONG, LitResUtil.HOST_NAME))
                }
            }
        }
        return true
    }

    companion object {
        private const val TAG_AUTHORIZATION_FAILED = "catalit-authorization-failed"
        private const val TAG_PURCHASE_OK = "catalit-purchase-ok"
        private const val TAG_PURCHASE_FAILED = "catalit-purchase-failed"
    }
}
