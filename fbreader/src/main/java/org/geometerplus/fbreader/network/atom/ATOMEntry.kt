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

package org.geometerplus.fbreader.network.atom

import org.geometerplus.zlibrary.core.xml.ZLStringMap
import java.util.LinkedList

open class ATOMEntry(source: ZLStringMap) : ATOMCommonAttributes(source) {

    @JvmField
    var id: ATOMId? = null

    @JvmField
    val authors: LinkedList<ATOMAuthor> = LinkedList()

    @JvmField
    val categories: LinkedList<ATOMCategory> = LinkedList()

    @JvmField
    val contributors: LinkedList<ATOMContributor> = LinkedList()

    @JvmField
    val links: LinkedList<ATOMLink> = LinkedList()

    @JvmField
    var published: ATOMPublished? = null

    //public String Rights;  // TODO: implement ATOMTextConstruct
    //public final ATOMSource Source; // TODO: implement ATOMSource
    @JvmField
    var summary: CharSequence? = null // TODO: implement ATOMTextConstruct

    @JvmField
    var content: CharSequence? = null // TODO: implement ATOMContent

    @JvmField
    var title: CharSequence? = null   // TODO: implement ATOMTextConstruct

    @JvmField
    var updated: ATOMUpdated? = null

    override fun toString(): String {
        val buf = StringBuilder("[")
            .append(super.toString())
            .append(",\nId=").append(id)
            .append(",\nAuthors:[\n")

        var first = true
        for (author in authors) {
            if (!first) buf.append(",\n")
            first = false
            buf.append(author.toString())
        }
        buf.append("],\nCategories:[\n")
        first = true
        for (category in categories) {
            if (!first) buf.append(",\n")
            first = false
            buf.append(category.toString())
        }
        buf.append("],\nLinks:[\n")
        first = true
        for (link in links) {
            if (!first) buf.append(",\n")
            first = false
            buf.append(link.toString())
        }
        return buf
            .append("]")
            .append(",\nPublished=").append(published)
            //.append(",\nRights=").append(Rights)
            .append(",\nSummary=").append(summary)
            .append(",\nTitle=").append(title)
            .append(",\nUpdated=").append(updated)
            .append("]")
            .toString()
    }
}
