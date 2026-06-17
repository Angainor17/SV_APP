/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.network.opds

import org.geometerplus.fbreader.network.atom.ATOMEntry
import org.geometerplus.zlibrary.core.xml.ZLStringMap

open class OPDSEntry(attributes: ZLStringMap) : ATOMEntry(attributes) {

    val dcIdentifiers: MutableList<String> = mutableListOf()
    var dcLanguage: String? = null
    var dcPublisher: String? = null
    var dcIssued: DCDate? = null
    var seriesTitle: String? = null
    var seriesIndex: Float = 0f

    override fun toString(): String {
        val buf = StringBuilder("[")
        buf.append(super.toString())
        buf.append(",DCLanguage=").append(dcLanguage)
        buf.append(",DCPublisher=").append(dcPublisher)
        buf.append(",DCIssued=").append(dcIssued)
        buf.append(",SeriesTitle=").append(seriesTitle)
        buf.append(",SeriesIndex=").append(seriesIndex)
        buf.append("]")
        return buf.toString()
    }
}
