/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.util

import android.database.Cursor
import android.database.sqlite.SQLiteStatement
import java.util.Date

object SQLiteUtil {
    @JvmStatic
    fun bindString(statement: SQLiteStatement, index: Int, value: String?) {
        if (value != null) {
            statement.bindString(index, value)
        } else {
            statement.bindNull(index)
        }
    }

    @JvmStatic
    fun bindLong(statement: SQLiteStatement, index: Int, value: Long?) {
        if (value != null) {
            statement.bindLong(index, value)
        } else {
            statement.bindNull(index)
        }
    }

    @JvmStatic
    fun bindDate(statement: SQLiteStatement, index: Int, value: Date?) {
        if (value != null) {
            statement.bindLong(index, value.time)
        } else {
            statement.bindNull(index)
        }
    }

    @JvmStatic
    fun getDate(cursor: Cursor, index: Int): Date? {
        return if (cursor.isNull(index)) {
            null
        } else {
            Date(cursor.getLong(index))
        }
    }
}
