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

package org.geometerplus.zlibrary.core.encodings

import android.util.Xml
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.Locale

abstract class FilteredEncodingCollection : EncodingCollection() {
    private val myEncodings = ArrayList<Encoding>()
    private val myEncodingByAlias = HashMap<String, Encoding>()

    init {
        try {
            val file = ZLResourceFile.createResourceFile("encodings/Encodings.xml")
            Xml.parse(file.getInputStream(), Xml.Encoding.UTF_8, EncodingCollectionReader())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    abstract fun isEncodingSupported(name: String): Boolean

    override fun encodings(): List<Encoding> = Collections.unmodifiableList(myEncodings)

    override fun getEncoding(alias: String): Encoding? {
        var e = myEncodingByAlias[alias]
        if (e == null && isEncodingSupported(alias)) {
            e = Encoding(null, alias, alias)
            myEncodingByAlias[alias] = e
            myEncodings.add(e)
        }
        return e
    }

    override fun getEncoding(code: Int): Encoding? = getEncoding(code.toString())

    fun providesConverterFor(alias: String): Boolean =
        myEncodingByAlias.containsKey(alias) || isEncodingSupported(alias)

    private inner class EncodingCollectionReader : DefaultHandler() {
        private var myCurrentFamilyName: String? = null
        private var myCurrentEncoding: Encoding? = null

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            when (localName) {
                "group" -> myCurrentFamilyName = attributes.getValue("name")
                "encoding" -> {
                    val name = attributes.getValue("name").lowercase(Locale.ROOT)
                    val region = attributes.getValue("region")
                    if (isEncodingSupported(name)) {
                        val encoding = Encoding(myCurrentFamilyName, name, "$name ($region)")
                        myCurrentEncoding = encoding
                        myEncodings.add(encoding)
                        myEncodingByAlias[name] = encoding
                    } else {
                        myCurrentEncoding = null
                    }
                }
                "code" -> {
                    val encoding = myCurrentEncoding
                    if (encoding != null) {
                        myEncodingByAlias[attributes.getValue("number")] = encoding
                    }
                }
                "alias" -> {
                    val encoding = myCurrentEncoding
                    if (encoding != null) {
                        myEncodingByAlias[attributes.getValue("name").lowercase(Locale.ROOT)] = encoding
                    }
                }
            }
        }
    }
}
