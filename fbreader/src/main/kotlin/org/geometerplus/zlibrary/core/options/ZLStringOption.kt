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

class ZLStringOption(group: String, optionName: String, defaultValue: String?) :
    ZLOption(group, optionName, defaultValue) {

    fun getValue(): String {
        val specialName = mySpecialName
        return if (specialName != null && !org.geometerplus.zlibrary.core.options.Config.Instance().isInitialized()) {
            org.geometerplus.zlibrary.core.options.Config.Instance().getSpecialStringValue(specialName, myDefaultStringValue) ?: myDefaultStringValue
        } else {
            getConfigValue()
        }
    }

    fun setValue(value: String?) {
        if (value == null) {
            return
        }
        val specialName = mySpecialName
        if (specialName != null) {
            org.geometerplus.zlibrary.core.options.Config.Instance().setSpecialStringValue(specialName, value)
        }
        setConfigValue(value)
    }

    override fun saveSpecialValue() {
        val specialName = mySpecialName
        if (specialName != null && org.geometerplus.zlibrary.core.options.Config.Instance().isInitialized()) {
            org.geometerplus.zlibrary.core.options.Config.Instance().setSpecialStringValue(specialName, getValue())
        }
    }
}
