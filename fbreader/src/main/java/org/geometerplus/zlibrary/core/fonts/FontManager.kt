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

package org.geometerplus.zlibrary.core.fonts

import java.util.Collections
import java.util.HashMap

class FontManager {
    @JvmField
    val Entries: MutableMap<String, FontEntry> = Collections.synchronizedMap(HashMap<String, FontEntry>())

    private val myFamilyLists = ArrayList<MutableList<String>>()

    @Synchronized
    fun index(families: MutableList<String>): Int {
        for (i in 0 until myFamilyLists.size) {
            if (myFamilyLists[i] == families) {
                return i
            }
        }
        myFamilyLists.add(ArrayList(families))
        return myFamilyLists.size - 1
    }

    @Synchronized
    fun getFamilyEntries(index: Int): List<FontEntry> = try {
        val families = myFamilyLists[index]
        val entries = ArrayList<FontEntry>(families.size)
        for (f in families) {
            val e = Entries[f]
            entries.add(e ?: FontEntry.systemEntry(f))
        }
        entries
    } catch (e: Exception) {
        emptyList()
    }
}
