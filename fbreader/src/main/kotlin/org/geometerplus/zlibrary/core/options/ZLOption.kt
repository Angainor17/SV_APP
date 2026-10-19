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

abstract class ZLOption protected constructor(
    group: String,
    optionName: String,
    defaultStringValue: String?,
) {
    @JvmField
    val myId: StringPair = StringPair(group, optionName)

    @JvmField
    var Config: ConfigInstance = ConfigInstance()

    protected var myDefaultStringValue: String = defaultStringValue ?: ""
    protected var mySpecialName: String? = null

    fun setSpecialName(specialName: String) {
        mySpecialName = specialName
    }

    open fun saveSpecialValue() {
    }

    protected fun getConfigValue(): String {
        val config = org.geometerplus.zlibrary.core.options.Config.Instance()
        return if (config != null) config.getValue(myId, myDefaultStringValue) else myDefaultStringValue
    }

    protected fun setConfigValue(value: String) {
        val config = org.geometerplus.zlibrary.core.options.Config.Instance()
        if (config != null) {
            if (myDefaultStringValue != value) {
                config.setValue(myId, value)
            } else {
                config.unsetValue(myId)
            }
        }
    }

    open class ConfigInstance {
        open fun Instance(): Config = org.geometerplus.zlibrary.core.options.Config.Instance()
    }
}
