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

package org.geometerplus.android.fbreader

import android.app.NotificationManager
import android.content.Context
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookUtil

object NotificationUtil {
    const val MISSING_BOOK_ID = 0x0fffffff
    private const val DOWNLOAD_ID_MIN = 0x10000000
    private const val DOWNLOAD_ID_MAX = 0x1fffffff

    @JvmStatic
    fun getDownloadId(file: String): Int {
        return DOWNLOAD_ID_MIN + Math.abs(file.hashCode()) % (DOWNLOAD_ID_MAX - DOWNLOAD_ID_MIN + 1)
    }

    @JvmStatic
    fun drop(context: Context, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
    }

    @JvmStatic
    fun drop(context: Context, book: Book?) {
        if (book == null) {
            return
        }
        val file = BookUtil.fileByBook(book).physicalFile
        if (file == null) {
            return
        }
        drop(context, getDownloadId(file.path))
    }
}
