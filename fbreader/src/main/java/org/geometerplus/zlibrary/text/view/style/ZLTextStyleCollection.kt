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

package org.geometerplus.zlibrary.text.view.style

import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import org.geometerplus.zlibrary.core.util.XmlUtil
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.util.ArrayList
import java.util.Collections

class ZLTextStyleCollection(screen: String) {
    @JvmField
    val Screen: String = screen

    private val myDescriptionList: List<ZLTextNGStyleDescription>
    private val myDescriptionMap = arrayOfNulls<ZLTextNGStyleDescription>(256)
    private var myBaseStyle: ZLTextBaseStyle? = null

    init {
        val descriptions =
            SimpleCSSReader().read(ZLResourceFile.createResourceFile("default/styles.css"))
        myDescriptionList = Collections.unmodifiableList(ArrayList(descriptions.values))
        for (entry in descriptions.entries) {
            myDescriptionMap[entry.key and 0xFF] = entry.value
        }
        XmlUtil.parseQuietly(
            ZLResourceFile.createResourceFile("default/styles.xml"),
            TextStyleReader(),
        )
    }

    val baseStyle: ZLTextBaseStyle
        get() = myBaseStyle!!

    fun getDescriptionList(): List<ZLTextNGStyleDescription> = myDescriptionList

    fun getDescription(kind: Byte): ZLTextNGStyleDescription? =
        myDescriptionMap[kind.toInt() and 0xFF]

    private inner class TextStyleReader : DefaultHandler() {
        private fun intValue(attributes: Attributes, name: String, defaultValue: Int): Int {
            val value = attributes.getValue(name)
            if (value != null) {
                try {
                    return value.toInt()
                } catch (e: NumberFormatException) {
                }
            }
            return defaultValue
        }

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String?,
            attributes: Attributes,
        ) {
            if ("base" == localName && Screen == attributes.getValue("screen")) {
                myBaseStyle = ZLTextBaseStyle(
                    Screen,
                    attributes.getValue("family"),
                    intValue(attributes, "fontSize", 0),
                )
            }
        }
    }
}
