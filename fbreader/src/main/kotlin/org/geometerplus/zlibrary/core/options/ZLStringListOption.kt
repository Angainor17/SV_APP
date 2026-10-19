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

import org.geometerplus.zlibrary.core.util.MiscUtil

class ZLStringListOption(
    group: String,
    optionName: String,
    defaultValue: List<String>,
    delimiter: String,
) : ZLOption(group, optionName, MiscUtil.join(defaultValue, delimiter)) {

    private val myDelimiter: String = delimiter
    private var myValue: List<String>? = null
    private var myStringValue: String? = null

    constructor(group: String, optionName: String, defaultValue: String?, delimiter: String) :
        this(group, optionName, if (defaultValue != null) listOf(defaultValue) else emptyList(), delimiter)

    fun getValue(): List<String> {
        val stringValue = getConfigValue()
        if (stringValue != myStringValue) {
            myStringValue = stringValue
            myValue = MiscUtil.split(stringValue, myDelimiter)
        }
        return myValue ?: emptyList()
    }

    fun setValue(value: List<String>?) {
        val v = value ?: emptyList()
        if (v == myValue) {
            return
        }
        myValue = v.toList()
        val joined = MiscUtil.join(v, myDelimiter)
        myStringValue = joined
        setConfigValue(joined)
    }
}
