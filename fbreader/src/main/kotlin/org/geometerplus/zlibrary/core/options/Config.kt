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

import java.util.Collections
import java.util.HashMap
import java.util.HashSet

abstract class Config protected constructor() {
    private val myNullString = StringBuilder("__NULL__").toString()
    private val myCache: MutableMap<StringPair, String> =
        Collections.synchronizedMap(HashMap<StringPair, String>())
    private val myCachedGroups = HashSet<String>()

    init {
        ourInstance = this
    }

    open fun getValue(id: StringPair, defaultValue: String): String {
        val cached = myCache[id]
        if (cached != null) {
            return if (cached !== myNullString) cached else defaultValue
        }
        val value: String = if (myCachedGroups.contains(id.Group)) {
            myNullString
        } else {
            try {
                getValueInternal(id.Group, id.Name) ?: myNullString
            } catch (e: NotAvailableException) {
                return defaultValue
            }
        }
        myCache[id] = value
        return if (value !== myNullString) value else defaultValue
    }

    open fun setValue(id: StringPair, value: String) {
        val oldValue = myCache[id]
        if (oldValue != null && oldValue == value) {
            return
        }
        myCache[id] = value
        setValueInternal(id.Group, id.Name, value)
    }

    fun requestAllValuesForGroup(group: String) {
        synchronized(myCachedGroups) {
            if (myCachedGroups.contains(group)) {
                return
            }
            val values: Map<String, String>
            try {
                values = requestAllValuesForGroupInternal(group) ?: return
            } catch (e: NotAvailableException) {
                return
            }
            for ((key, value) in values) {
                setToCache(group, key, value)
            }
            myCachedGroups.add(group)
        }
    }

    open fun unsetValue(id: StringPair) {
        myCache[id] = myNullString
        unsetValueInternal(id.Group, id.Name)
    }

    protected fun setToCache(group: String, name: String, value: String?) {
        myCache[StringPair(group, name)] = value ?: myNullString
    }

    abstract fun isInitialized(): Boolean

    abstract fun runOnConnect(runnable: Runnable)

    abstract fun listGroups(): List<String>?

    abstract fun listNames(group: String): List<String>?

    abstract fun removeGroup(name: String)

    abstract fun getSpecialBooleanValue(name: String, defaultValue: Boolean): Boolean

    abstract fun setSpecialBooleanValue(name: String, value: Boolean)

    abstract fun getSpecialStringValue(name: String, defaultValue: String): String?

    abstract fun setSpecialStringValue(name: String, value: String)

    @Throws(NotAvailableException::class)
    protected abstract fun getValueInternal(group: String, name: String): String?

    protected abstract fun setValueInternal(group: String, name: String, value: String)

    protected abstract fun unsetValueInternal(group: String, name: String)

    @Throws(NotAvailableException::class)
    protected abstract fun requestAllValuesForGroupInternal(group: String): Map<String, String>?

    class NotAvailableException(message: String) : Exception(message)

    companion object {
        private var ourInstance: Config? = null

        @JvmStatic
        fun Instance(): Config = ourInstance!!
    }
}
