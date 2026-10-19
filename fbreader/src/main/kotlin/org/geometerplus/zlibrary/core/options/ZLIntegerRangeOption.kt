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

class ZLIntegerRangeOption(
    group: String,
    optionName: String,
    minValue: Int,
    maxValue: Int,
    defaultValue: Int,
) : ZLOption(group, optionName, valueInRange(defaultValue, minValue, maxValue).toString()) {

    @JvmField
    val MinValue: Int = minValue

    @JvmField
    val MaxValue: Int = maxValue

    private var myValue: Int = 0
    private var myStringValue: String? = null

    fun getValue(): Int {
        val stringValue = getConfigValue()
        if (stringValue != myStringValue) {
            myStringValue = stringValue
            val parsed = stringValue.toIntOrNull()
            if (parsed != null) {
                myValue = valueInRange(parsed, MinValue, MaxValue)
            }
        }
        return myValue
    }

    fun setValue(value: Int) {
        val clamped = valueInRange(value, MinValue, MaxValue)
        myValue = clamped
        myStringValue = clamped.toString()
        setConfigValue(clamped.toString())
    }

    companion object {
        private fun valueInRange(value: Int, min: Int, max: Int): Int = minOf(max, maxOf(min, value))
    }
}
