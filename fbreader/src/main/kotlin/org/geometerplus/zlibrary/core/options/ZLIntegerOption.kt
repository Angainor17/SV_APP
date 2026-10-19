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

class ZLIntegerOption(group: String, optionName: String, defaultValue: Int) :
    ZLOption(group, optionName, defaultValue.toString()) {

    private var myValue: Int = 0
    private var myStringValue: String? = null

    fun getValue(): Int {
        val stringValue = getConfigValue()
        if (stringValue != myStringValue) {
            myStringValue = stringValue
            myValue = stringValue.toIntOrNull() ?: myValue
        }
        return myValue
    }

    fun setValue(value: Int) {
        myValue = value
        myStringValue = value.toString()
        setConfigValue(value.toString())
    }
}
