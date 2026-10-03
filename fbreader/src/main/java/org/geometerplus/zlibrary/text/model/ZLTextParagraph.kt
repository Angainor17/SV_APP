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

interface ZLTextParagraph {
    fun iterator(): EntryIterator

    fun getKind(): Byte

    interface Entry {
        companion object {
            const val TEXT: Byte = 1
            const val IMAGE: Byte = 2
            const val CONTROL: Byte = 3
            const val HYPERLINK_CONTROL: Byte = 4
            const val STYLE_CSS: Byte = 5
            const val STYLE_OTHER: Byte = 6
            const val STYLE_CLOSE: Byte = 7
            const val FIXED_HSPACE: Byte = 8
            const val RESET_BIDI: Byte = 9
            const val AUDIO: Byte = 10
            const val VIDEO: Byte = 11
            const val EXTENSION: Byte = 12
        }
    }

    interface EntryIterator {
        fun getType(): Byte

        fun getTextData(): CharArray

        fun getTextOffset(): Int

        fun getTextLength(): Int

        fun getControlKind(): Byte

        fun getControlIsStart(): Boolean

        fun getHyperlinkType(): Byte

        fun getHyperlinkId(): String

        fun getImageEntry(): ZLImageEntry

        fun getVideoEntry(): ZLVideoEntry

        fun getExtensionEntry(): ExtensionEntry

        fun getStyleEntry(): ZLTextStyleEntry

        fun getFixedHSpaceLength(): Short

        fun next(): Boolean
    }

    interface Kind {
        companion object {
            const val TEXT_PARAGRAPH: Byte = 0
            // const val TREE_PARAGRAPH: Byte = 1
            const val EMPTY_LINE_PARAGRAPH: Byte = 2
            const val BEFORE_SKIP_PARAGRAPH: Byte = 3
            const val AFTER_SKIP_PARAGRAPH: Byte = 4
            const val END_OF_SECTION_PARAGRAPH: Byte = 5
            const val PSEUDO_END_OF_SECTION_PARAGRAPH: Byte = 6
            const val END_OF_TEXT_PARAGRAPH: Byte = 7
            const val ENCRYPTED_SECTION_PARAGRAPH: Byte = 8
        }
    }
}
