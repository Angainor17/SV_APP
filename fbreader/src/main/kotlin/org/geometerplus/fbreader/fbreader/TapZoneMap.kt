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

package org.geometerplus.fbreader.fbreader

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption
import org.geometerplus.zlibrary.core.options.ZLStringListOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.util.XmlUtil
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.util.HashMap
import java.util.LinkedList
import java.util.Locale

class TapZoneMap private constructor(name: String) {
    @JvmField
    val Name: String = name

    private val myOptionGroupName: String = "TapZones:" + name
    private val myZoneMap = HashMap<Zone, ZLStringOption>()
    private val myZoneMap2 = HashMap<Zone, ZLStringOption>()
    private val myHeight = ZLIntegerRangeOption(myOptionGroupName, "Height", 2, 5, 3)
    private val myWidth = ZLIntegerRangeOption(myOptionGroupName, "Width", 2, 5, 3)

    init {
        val mapFile = ZLFile.createFileByPath(
            "default/tapzones/" + name.lowercase(Locale.ROOT) + ".xml"
        )
        if (mapFile != null) {
            XmlUtil.parseQuietly(mapFile, Reader())
        }
    }

    val width: Int
        get() = myWidth.getValue()

    val height: Int
        get() = myHeight.getValue()

    fun isCustom(): Boolean = !ourPredefinedMaps.contains(Name)

    fun getActionByCoordinates(x: Int, y: Int, width: Int, height: Int, tap: Tap): String? {
        if (width == 0 || height == 0) {
            return null
        }
        val x2 = maxOf(0, minOf(width - 1, x))
        val y2 = maxOf(0, minOf(height - 1, y))
        return getActionByZone(myWidth.getValue() * x2 / width, myHeight.getValue() * y2 / height, tap)
    }

    fun getActionByZone(h: Int, v: Int, tap: Tap): String? {
        val option = getOptionByZone(Zone(h, v), tap)
        return option?.getValue()
    }

    private fun getOptionByZone(zone: Zone, tap: Tap): ZLStringOption? =
        when (tap) {
            Tap.singleTap -> myZoneMap[zone] ?: myZoneMap2[zone]
            Tap.singleNotDoubleTap -> myZoneMap[zone]
            Tap.doubleTap -> myZoneMap2[zone]
        }

    private fun createOptionForZone(zone: Zone, singleTap: Boolean, action: String?): ZLStringOption =
        ZLStringOption(
            myOptionGroupName,
            (if (singleTap) "Action" else "Action2") + ":" + zone.HIndex + ":" + zone.VIndex,
            action
        )

    fun setActionForZone(h: Int, v: Int, singleTap: Boolean, action: String) {
        val zone = Zone(h, v)
        val map = if (singleTap) myZoneMap else myZoneMap2
        val option = map[zone] ?: createOptionForZone(zone, singleTap, null).also { map[zone] = it }
        option.setValue(action)
    }

    enum class Tap {
        singleTap,
        singleNotDoubleTap,
        doubleTap,
    }

    private data class Zone(val HIndex: Int, val VIndex: Int)

    private inner class Reader : DefaultHandler() {
        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            try {
                if ("zone" == localName) {
                    val zone = Zone(
                        attributes.getValue("x")!!.toInt(),
                        attributes.getValue("y")!!.toInt()
                    )
                    val action = attributes.getValue("action")
                    val action2 = attributes.getValue("action2")
                    if (action != null) {
                        myZoneMap[zone] = createOptionForZone(zone, true, action)
                    }
                    if (action2 != null) {
                        myZoneMap2[zone] = createOptionForZone(zone, false, action2)
                    }
                } else if ("tapZones" == localName) {
                    val v = attributes.getValue("v")
                    if (v != null) {
                        myHeight.setValue(v.toInt())
                    }
                    val h = attributes.getValue("h")
                    if (h != null) {
                        myWidth.setValue(h.toInt())
                    }
                }
            } catch (e: Throwable) {
            }
        }
    }

    companion object {
        private val ourPredefinedMaps: MutableList<String> = LinkedList<String>().apply {
            add("right_to_left")
            add("left_to_right")
            add("down")
            add("up")
        }
        private val ourMapsOption = ZLStringListOption("TapZones", "List", ourPredefinedMaps, "\u0000")
        private val ourMaps = HashMap<String, TapZoneMap>()

        @JvmStatic
        fun zoneMapNames(): List<String> = ourMapsOption.getValue()

        @JvmStatic
        fun zoneMap(name: String): TapZoneMap {
            var map = ourMaps[name]
            if (map == null) {
                map = TapZoneMap(name)
                ourMaps[name] = map
            }
            return map
        }

        @JvmStatic
        fun createZoneMap(name: String, width: Int, height: Int): TapZoneMap? {
            if (ourMapsOption.getValue().contains(name)) {
                return null
            }
            val map = zoneMap(name)
            map.myWidth.setValue(width)
            map.myHeight.setValue(height)
            val lst = LinkedList(ourMapsOption.getValue())
            lst.add(name)
            ourMapsOption.setValue(lst)
            return map
        }

        @JvmStatic
        fun deleteZoneMap(name: String) {
            if (ourPredefinedMaps.contains(name)) {
                return
            }
            ourMaps.remove(name)
            val lst = LinkedList(ourMapsOption.getValue())
            lst.remove(name)
            ourMapsOption.setValue(lst)
        }
    }
}
