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

package org.geometerplus.zlibrary.core.application

import org.geometerplus.fbreader.Paths
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.options.ZLStringListOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.util.XmlUtil
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.util.TreeMap
import java.util.TreeSet

class ZLKeyBindings private constructor(private val myName: String) {

    private val myActionMap = TreeMap<Int, ZLStringOption>()
    private val myLongPressActionMap = TreeMap<Int, ZLStringOption>()
    private var myKeysOption: ZLStringListOption? = null

    constructor() : this("Keys")

    init {
        Config.Instance().runOnConnect(Initializer())
    }

    private fun createOption(key: Int, longPress: Boolean, defaultValue: String?): ZLStringOption {
        val group = myName + ":" + (if (longPress) LONG_PRESS_ACTION else ACTION)
        return ZLStringOption(group, key.toString(), defaultValue)
    }

    fun getOption(key: Int, longPress: Boolean): ZLStringOption {
        val map = if (longPress) myLongPressActionMap else myActionMap
        return map[key] ?: createOption(key, longPress, ZLApplication.NoAction).also { map[key] = it }
    }

    fun bindKey(key: Int, longPress: Boolean, actionId: String) {
        val keysOption = myKeysOption ?: return
        val stringKey = key.toString()
        val keys = keysOption.getValue()
        if (!keys.contains(stringKey)) {
            val newKeys = ArrayList(keys)
            newKeys.add(stringKey)
            newKeys.sort()
            keysOption.setValue(newKeys)
        }
        getOption(key, longPress).setValue(actionId)
    }

    fun getBinding(key: Int, longPress: Boolean): String = getOption(key, longPress).getValue()

    fun hasBinding(key: Int, longPress: Boolean): Boolean =
        ZLApplication.NoAction != getBinding(key, longPress)

    private inner class Initializer : Runnable {
        override fun run() {
            val keys = TreeSet<String>()
            Reader(keys).readQuietly("default/keymap.xml")
            Reader(keys).readQuietly(Paths.systemShareDirectory() + "/keymap.xml")
            Reader(keys).readQuietly(Paths.bookPath()[0] + "/keymap.xml")
            myKeysOption = ZLStringListOption(myName, "KeyList", ArrayList(keys), ",")
        }
    }

    private inner class Reader(private val myKeySet: MutableSet<String>) : DefaultHandler() {
        fun readQuietly(path: String) {
            ZLFile.createFileByPath(path)?.let { XmlUtil.parseQuietly(it, this) }
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            if ("binding" == localName) {
                val stringKey = attributes.getValue("key")
                val actionId = attributes.getValue("action")
                if (stringKey != null && actionId != null) {
                    try {
                        val key = stringKey.toInt()
                        myKeySet.add(stringKey)
                        myActionMap[key] = createOption(key, false, actionId)
                    } catch (e: NumberFormatException) {
                        // ignore
                    }
                }
            }
        }
    }

    companion object {
        private const val ACTION = "Action"
        private const val LONG_PRESS_ACTION = "LongPressAction"
    }
}
