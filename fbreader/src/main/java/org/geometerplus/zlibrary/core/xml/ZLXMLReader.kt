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

package org.geometerplus.zlibrary.core.xml

import java.util.HashMap

interface ZLXMLReader {
    fun dontCacheAttributeValues(): Boolean

    fun startDocumentHandler()

    fun endDocumentHandler()

    // returns true iff xml processing should be interrupted
    fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean

    fun endElementHandler(tag: String): Boolean

    fun characterDataHandler(ch: CharArray, start: Int, length: Int)

    fun characterDataHandlerFinal(ch: CharArray, start: Int, length: Int)

    fun processNamespaces(): Boolean

    fun namespaceMapChangedHandler(namespaces: MutableMap<String, String>)

    fun collectExternalEntities(entityMap: HashMap<String, CharArray>)

    fun externalDTDs(): List<String>
}
