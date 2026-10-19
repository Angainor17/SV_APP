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

package org.geometerplus.zlibrary.text.model

interface ZLTextAlignmentType {
    companion object {
        const val ALIGN_UNDEFINED: Byte = 0
        const val ALIGN_LEFT: Byte = 1
        const val ALIGN_RIGHT: Byte = 2
        const val ALIGN_CENTER: Byte = 3
        const val ALIGN_JUSTIFY: Byte = 4
        const val ALIGN_LINESTART: Byte = 5 // left for LTR languages and right for RTL
    }
}
