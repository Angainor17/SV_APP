/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.book

class Book(
    id: Long,
    path: String?,
    title: String?,
    encoding: String?,
    language: String?,
) : AbstractBook(id, title, encoding, language) {

    private val myPath: String

    init {
        if (path == null) {
            throw IllegalArgumentException("Creating book with no file")
        }
        myPath = path
    }

    override fun getPath(): String = myPath

    override fun hashCode(): Int = myPath.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Book) {
            return false
        }
        return myPath == other.myPath
    }
}
