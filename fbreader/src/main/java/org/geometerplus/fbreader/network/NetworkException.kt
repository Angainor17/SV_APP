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

package org.geometerplus.fbreader.network

import org.geometerplus.zlibrary.core.network.ZLNetworkException

abstract class NetworkException private constructor() : ZLNetworkException(null) {
    companion object {
        const val ERROR_INTERNAL = "internalError"
        const val ERROR_PURCHASE_NOT_ENOUGH_MONEY = "purchaseNotEnoughMoney"
        const val ERROR_PURCHASE_MISSING_BOOK = "purchaseMissingBook"
        const val ERROR_BOOK_NOT_PURCHASED = "bookNotPurchased"
        const val ERROR_DOWNLOAD_LIMIT_EXCEEDED = "downloadLimitExceeded"
        const val ERROR_EMAIL_NOT_SPECIFIED = "emailNotSpecified"
        const val ERROR_NO_USER_FOR_EMAIL = "noUserForEmail"
        const val ERROR_UNSUPPORTED_OPERATION = "unsupportedOperation"
        const val ERROR_NOT_AN_OPDS = "notAnOPDS"
        const val ERROR_NO_REQUIRED_INFORMATION = "noRequiredInformation"
        const val ERROR_CACHE_DIRECTORY_ERROR = "cacheDirectoryError"

        private const val serialVersionUID: Long = -637340804063605L
    }
}
