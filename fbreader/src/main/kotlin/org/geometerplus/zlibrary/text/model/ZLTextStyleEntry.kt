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

import org.fbreader.util.Boolean3
import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.core.fonts.FontManager

abstract class ZLTextStyleEntry protected constructor(depth: Short) {
    @JvmField
    val Depth: Short = depth

    private var myFeatureMask: Short = 0
    private val myLengths = arrayOfNulls<Length>(Feature.NUMBER_OF_LENGTHS)
    private var myAlignmentType: Byte = 0
    private var myFontEntries: List<FontEntry>? = null
    private var mySupportedFontModifiers: Byte = 0
    private var myFontModifiers: Byte = 0
    private var myVerticalAlignCode: Byte = 0

    fun isFeatureSupported(featureId: Int): Boolean =
        isFeatureSupported(myFeatureMask, featureId)

    fun setLength(featureId: Int, size: Short, unit: Byte) {
        myFeatureMask = (myFeatureMask.toInt() or (1 shl featureId)).toShort()
        myLengths[featureId] = Length(size, unit)
    }

    fun getLength(featureId: Int, metrics: ZLTextMetrics, fontSize: Int): Int =
        compute(myLengths[featureId]!!, metrics, fontSize, featureId)

    fun hasNonZeroLength(featureId: Int): Boolean =
        myLengths[featureId]!!.Size.toInt() != 0

    fun getAlignmentType(): Byte = myAlignmentType

    fun setAlignmentType(alignmentType: Byte) {
        myFeatureMask = (myFeatureMask.toInt() or (1 shl Feature.ALIGNMENT_TYPE)).toShort()
        myAlignmentType = alignmentType
    }

    fun setFontFamilies(fontManager: FontManager, fontFamiliesIndex: Int) {
        myFeatureMask = (myFeatureMask.toInt() or (1 shl Feature.FONT_FAMILY)).toShort()
        myFontEntries = fontManager.getFamilyEntries(fontFamiliesIndex)
    }

    fun getFontEntries(): List<FontEntry>? = myFontEntries

    fun setFontModifiers(supported: Byte, values: Byte) {
        myFeatureMask = (myFeatureMask.toInt() or (1 shl Feature.FONT_STYLE_MODIFIER)).toShort()
        mySupportedFontModifiers = supported
        myFontModifiers = values
    }

    fun setFontModifier(modifier: Byte, on: Boolean) {
        myFeatureMask = (myFeatureMask.toInt() or (1 shl Feature.FONT_STYLE_MODIFIER)).toShort()
        mySupportedFontModifiers = (mySupportedFontModifiers.toInt() or modifier.toInt()).toByte()
        myFontModifiers = if (on) {
            (myFontModifiers.toInt() or modifier.toInt()).toByte()
        } else {
            (myFontModifiers.toInt() and modifier.toInt().inv()).toByte()
        }
    }

    fun getFontModifier(modifier: Byte): Boolean3 {
        if ((mySupportedFontModifiers.toInt() and modifier.toInt()) == 0) {
            return Boolean3.UNDEFINED
        }
        return if ((myFontModifiers.toInt() and modifier.toInt()) == 0) Boolean3.FALSE else Boolean3.TRUE
    }

    fun getVerticalAlignCode(): Byte = myVerticalAlignCode

    fun setVerticalAlignCode(code: Byte) {
        myFeatureMask = (myFeatureMask.toInt() or (1 shl Feature.NON_LENGTH_VERTICAL_ALIGN)).toShort()
        myVerticalAlignCode = code
    }

    override fun toString(): String {
        val buffer = StringBuilder("StyleEntry[")
        buffer.append("features: ").append(myFeatureMask).append(";")
        if (isFeatureSupported(Feature.LENGTH_SPACE_BEFORE)) {
            buffer.append("space-before: ").append(myLengths[Feature.LENGTH_SPACE_BEFORE]).append(";")
        }
        if (isFeatureSupported(Feature.LENGTH_SPACE_AFTER)) {
            buffer.append("space-after: ").append(myLengths[Feature.LENGTH_SPACE_AFTER]).append(";")
        }
        buffer.append("]")
        return buffer.toString()
    }

    interface Feature {
        companion object {
            const val LENGTH_PADDING_LEFT = 0
            const val LENGTH_PADDING_RIGHT = 1
            const val LENGTH_MARGIN_LEFT = 2
            const val LENGTH_MARGIN_RIGHT = 3
            const val LENGTH_FIRST_LINE_INDENT = 4
            const val LENGTH_SPACE_BEFORE = 5
            const val LENGTH_SPACE_AFTER = 6
            const val LENGTH_FONT_SIZE = 7
            const val LENGTH_VERTICAL_ALIGN = 8
            const val NUMBER_OF_LENGTHS = 9
            const val ALIGNMENT_TYPE = NUMBER_OF_LENGTHS
            const val FONT_FAMILY = NUMBER_OF_LENGTHS + 1
            const val FONT_STYLE_MODIFIER = NUMBER_OF_LENGTHS + 2
            const val NON_LENGTH_VERTICAL_ALIGN = NUMBER_OF_LENGTHS + 3
            // not transferred at the moment
            const val DISPLAY = NUMBER_OF_LENGTHS + 4
        }
    }

    interface FontModifier {
        companion object {
            const val FONT_MODIFIER_BOLD: Byte = 1
            const val FONT_MODIFIER_ITALIC: Byte = 2
            const val FONT_MODIFIER_UNDERLINED: Byte = 4
            const val FONT_MODIFIER_STRIKEDTHROUGH: Byte = 8
            const val FONT_MODIFIER_SMALLCAPS: Byte = 16
            const val FONT_MODIFIER_INHERIT: Byte = 32
            const val FONT_MODIFIER_SMALLER: Byte = 64
            const val FONT_MODIFIER_LARGER: Byte = -128
        }
    }

    interface SizeUnit {
        companion object {
            const val PIXEL: Byte = 0
            const val POINT: Byte = 1
            const val EM_100: Byte = 2
            const val REM_100: Byte = 3
            const val EX_100: Byte = 4
            const val PERCENT: Byte = 5
        }
    }

    class Length(
        @JvmField val Size: Short,
        @JvmField val Unit: Byte,
    ) {
        override fun toString(): String = "$Size.$Unit"
    }

    companion object {
        @JvmStatic
        fun isFeatureSupported(mask: Short, featureId: Int): Boolean =
            (mask.toInt() and (1 shl featureId)) != 0

        private fun fullSize(metrics: ZLTextMetrics, fontSize: Int, featureId: Int): Int =
            when (featureId) {
                Feature.LENGTH_MARGIN_LEFT,
                Feature.LENGTH_MARGIN_RIGHT,
                Feature.LENGTH_PADDING_LEFT,
                Feature.LENGTH_PADDING_RIGHT,
                Feature.LENGTH_FIRST_LINE_INDENT,
                -> metrics.FullWidth

                Feature.LENGTH_SPACE_BEFORE,
                Feature.LENGTH_SPACE_AFTER,
                -> metrics.FullHeight

                Feature.LENGTH_VERTICAL_ALIGN,
                Feature.LENGTH_FONT_SIZE,
                -> fontSize

                else -> metrics.FullWidth
            }

        @JvmStatic
        fun compute(length: Length, metrics: ZLTextMetrics, fontSize: Int, featureId: Int): Int =
            when (length.Unit) {
                SizeUnit.PIXEL -> length.Size.toInt()
                SizeUnit.POINT -> length.Size.toInt() * metrics.DPI / 72
                SizeUnit.EM_100 -> (length.Size.toInt() * fontSize + 50) / 100
                SizeUnit.REM_100 -> (length.Size.toInt() * metrics.FontSize + 50) / 100
                SizeUnit.EX_100 -> (length.Size.toInt() * fontSize / 2 + 50) / 100
                SizeUnit.PERCENT -> (length.Size.toInt() * fullSize(metrics, fontSize, featureId) + 50) / 100
                else -> length.Size.toInt()
            }
    }
}
