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

package org.geometerplus.zlibrary.core.options

import org.geometerplus.zlibrary.core.util.ZLColor

class ZLColorOption(group: String, optionName: String, defaultValue: ZLColor?) :
    ZLOption(group, optionName, stringColorValue(defaultValue)) {

    private var myValue: ZLColor? = null
    private var myStringValue: String? = null

    fun getValue(): ZLColor? {
        val stringValue = getConfigValue()
        if (stringValue != myStringValue) {
            myStringValue = stringValue
            val parsed = stringValue.toIntOrNull()
            if (parsed != null) {
                myValue = if (parsed != -1) ZLColor(parsed) else null
            }
        }
        return myValue
    }

    fun setValue(value: ZLColor?) {
        if (value == null) {
            return
        }
        myValue = value
        myStringValue = stringColorValue(value)
        setConfigValue(stringColorValue(value))
    }

    companion object {
        private fun stringColorValue(color: ZLColor?): String = (color?.intValue() ?: -1).toString()
    }
}
