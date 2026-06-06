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

interface ATOMConstants {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_HTML = "html"
        const val TYPE_XHTML = "xhtml"
        const val TYPE_DEFAULT = TYPE_TEXT

        const val REL_ALTERNATE = "alternate"
        const val REL_RELATED = "related"
        const val REL_SELF = "self"
        const val REL_ENCLOSURE = "enclosure"
        const val REL_VIA = "via"
    }
}
