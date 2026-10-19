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

package org.geometerplus.android.fbreader.config

import android.content.Context
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.options.Config.NotAvailableException

/**
 * Local configuration for regular option values.
 *
 * The original implementation bound to a {@code ConfigService} via AIDL. That service
 * is no longer registered in the app, so regular option reads always fell back to their
 * defaults and writes were dropped. Special option values (a small set of UI options)
 * were already stored directly in SharedPreferences and continue to do so here.
 */
class ConfigShadow(private val myContext: Context) : Config() {
    override fun isInitialized(): Boolean = false

    override fun runOnConnect(runnable: Runnable) {
        // no remote service to connect to
    }

    override fun listGroups(): List<String>? = emptyList()

    override fun listNames(group: String): List<String>? = emptyList()

    override fun removeGroup(name: String) {
        // no-op
    }

    override fun getSpecialBooleanValue(name: String, defaultValue: Boolean): Boolean =
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
            .getBoolean(name, defaultValue)

    override fun setSpecialBooleanValue(name: String, value: Boolean) {
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
            .edit().putBoolean(name, value).apply()
    }

    override fun getSpecialStringValue(name: String, defaultValue: String): String? =
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
            .getString(name, defaultValue)

    override fun setSpecialStringValue(name: String, value: String) {
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
            .edit().putString(name, value).apply()
    }

    @Throws(NotAvailableException::class)
    protected override fun getValueInternal(group: String, name: String): String? =
        throw NotAvailableException("Config is not initialized for $group:$name")

    protected override fun setValueInternal(group: String, name: String, value: String) {
        // no persistent storage
    }

    protected override fun unsetValueInternal(group: String, name: String) {
        // no persistent storage
    }

    @Throws(NotAvailableException::class)
    protected override fun requestAllValuesForGroupInternal(group: String): Map<String, String>? =
        throw NotAvailableException("Config is not initialized for $group")
}
