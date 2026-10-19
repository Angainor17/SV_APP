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

import org.fbreader.util.Boolean3
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.text.model.ZLTextAlignmentType
import org.geometerplus.zlibrary.text.model.ZLTextMetrics
import org.geometerplus.zlibrary.text.model.ZLTextStyleEntry
import java.util.HashMap

class ZLTextNGStyleDescription internal constructor(
    selector: String,
    valueMap: Map<String, String>,
) {
    val Name: String? = valueMap["fbreader-name"]

    val FontFamilyOption: ZLStringOption = createOption(selector, "font-family", valueMap)
    val FontSizeOption: ZLStringOption = createOption(selector, "font-size", valueMap)
    val FontWeightOption: ZLStringOption = createOption(selector, "font-weight", valueMap)
    val FontStyleOption: ZLStringOption = createOption(selector, "font-style", valueMap)
    val TextDecorationOption: ZLStringOption = createOption(selector, "text-decoration", valueMap)
    val HyphenationOption: ZLStringOption = createOption(selector, "hyphens", valueMap)
    val MarginTopOption: ZLStringOption = createOption(selector, "margin-top", valueMap)
    val MarginBottomOption: ZLStringOption = createOption(selector, "margin-bottom", valueMap)
    val MarginLeftOption: ZLStringOption = createOption(selector, "margin-left", valueMap)
    val MarginRightOption: ZLStringOption = createOption(selector, "margin-right", valueMap)
    val TextIndentOption: ZLStringOption = createOption(selector, "text-indent", valueMap)
    val AlignmentOption: ZLStringOption = createOption(selector, "text-align", valueMap)
    val VerticalAlignOption: ZLStringOption = createOption(selector, "vertical-align", valueMap)
    val LineHeightOption: ZLStringOption = createOption(selector, "line-height", valueMap)

    fun getFontSize(metrics: ZLTextMetrics, parentFontSize: Int): Int {
        val length = parseLength(FontSizeOption.getValue())
        if (length == null) {
            return parentFontSize
        }
        return ZLTextStyleEntry.compute(
            length, metrics, parentFontSize, ZLTextStyleEntry.Feature.LENGTH_FONT_SIZE,
        )
    }

    fun getVerticalAlign(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int {
        val length = parseLength(VerticalAlignOption.getValue())
        if (length == null) {
            return base
        }
        return ZLTextStyleEntry.compute(
            length, metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_FONT_SIZE,
        )
    }

    fun hasNonZeroVerticalAlign(): Boolean {
        val length = parseLength(VerticalAlignOption.getValue())
        return length != null && length.Size.toInt() != 0
    }

    fun getLeftMargin(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int {
        val length = parseLength(MarginLeftOption.getValue())
        if (length == null) {
            return base
        }
        return base + ZLTextStyleEntry.compute(
            length, metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_MARGIN_LEFT,
        )
    }

    fun getRightMargin(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int {
        val length = parseLength(MarginRightOption.getValue())
        if (length == null) {
            return base
        }
        return base + ZLTextStyleEntry.compute(
            length, metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_MARGIN_RIGHT,
        )
    }

    fun getLeftPadding(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int = base

    fun getRightPadding(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int = base

    fun getFirstLineIndent(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int {
        val length = parseLength(TextIndentOption.getValue())
        if (length == null) {
            return base
        }
        return ZLTextStyleEntry.compute(
            length, metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_FIRST_LINE_INDENT,
        )
    }

    fun getSpaceBefore(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int {
        val length = parseLength(MarginTopOption.getValue())
        if (length == null) {
            return base
        }
        return ZLTextStyleEntry.compute(
            length, metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_SPACE_BEFORE,
        )
    }

    fun getSpaceAfter(metrics: ZLTextMetrics, base: Int, fontSize: Int): Int {
        val length = parseLength(MarginBottomOption.getValue())
        if (length == null) {
            return base
        }
        return ZLTextStyleEntry.compute(
            length, metrics, fontSize, ZLTextStyleEntry.Feature.LENGTH_SPACE_AFTER,
        )
    }

    fun isBold(): Boolean3 {
        val fontWeight = FontWeightOption.getValue()
        if ("bold" == fontWeight) {
            return Boolean3.TRUE
        } else if ("normal" == fontWeight) {
            return Boolean3.FALSE
        } else {
            return Boolean3.UNDEFINED
        }
    }

    fun isItalic(): Boolean3 {
        val fontStyle = FontStyleOption.getValue()
        if ("italic" == fontStyle || "oblique" == fontStyle) {
            return Boolean3.TRUE
        } else if ("normal" == fontStyle) {
            return Boolean3.FALSE
        } else {
            return Boolean3.UNDEFINED
        }
    }

    fun isUnderlined(): Boolean3 {
        val textDecoration = TextDecorationOption.getValue()
        if ("underline" == textDecoration) {
            return Boolean3.TRUE
        } else if ("" == textDecoration || "inherit" == textDecoration) {
            return Boolean3.UNDEFINED
        } else {
            return Boolean3.FALSE
        }
    }

    fun isStrikedThrough(): Boolean3 {
        val textDecoration = TextDecorationOption.getValue()
        if ("line-through" == textDecoration) {
            return Boolean3.TRUE
        } else if ("" == textDecoration || "inherit" == textDecoration) {
            return Boolean3.UNDEFINED
        } else {
            return Boolean3.FALSE
        }
    }

    fun getAlignment(): Byte {
        val alignment = AlignmentOption.getValue()
        if (alignment.length == 0) {
            return ZLTextAlignmentType.ALIGN_UNDEFINED
        } else if ("center" == alignment) {
            return ZLTextAlignmentType.ALIGN_CENTER
        } else if ("left" == alignment) {
            return ZLTextAlignmentType.ALIGN_LEFT
        } else if ("right" == alignment) {
            return ZLTextAlignmentType.ALIGN_RIGHT
        } else if ("justify" == alignment) {
            return ZLTextAlignmentType.ALIGN_JUSTIFY
        } else {
            return ZLTextAlignmentType.ALIGN_UNDEFINED
        }
    }

    fun allowHyphenations(): Boolean3 {
        val hyphen = HyphenationOption.getValue()
        if ("auto" == hyphen) {
            return Boolean3.TRUE
        } else if ("none" == hyphen) {
            return Boolean3.FALSE
        } else {
            return Boolean3.UNDEFINED
        }
    }

    companion object {
        private val ourCache = HashMap<String, Any?>()
        private val ourNullObject = Any()

        private fun createOption(selector: String, name: String, valueMap: Map<String, String>): ZLStringOption =
            ZLStringOption("Style", "$selector::$name", valueMap[name])

        private fun parseLength(value: String): ZLTextStyleEntry.Length? {
            if (value.length == 0) {
                return null
            }

            val cached = ourCache[value]
            if (cached != null) {
                return if (cached === ourNullObject) null else cached as ZLTextStyleEntry.Length
            }

            var length: ZLTextStyleEntry.Length? = null
            try {
                if (value.endsWith("%")) {
                    length = ZLTextStyleEntry.Length(
                        value.substring(0, value.length - 1).toShort(),
                        ZLTextStyleEntry.SizeUnit.PERCENT,
                    )
                } else if (value.endsWith("rem")) {
                    length = ZLTextStyleEntry.Length(
                        (100 * value.substring(0, value.length - 2).toDouble()).toInt().toShort(),
                        ZLTextStyleEntry.SizeUnit.REM_100,
                    )
                } else if (value.endsWith("em")) {
                    length = ZLTextStyleEntry.Length(
                        (100 * value.substring(0, value.length - 2).toDouble()).toInt().toShort(),
                        ZLTextStyleEntry.SizeUnit.EM_100,
                    )
                } else if (value.endsWith("ex")) {
                    length = ZLTextStyleEntry.Length(
                        (100 * value.substring(0, value.length - 2).toDouble()).toInt().toShort(),
                        ZLTextStyleEntry.SizeUnit.EX_100,
                    )
                } else if (value.endsWith("px")) {
                    length = ZLTextStyleEntry.Length(
                        value.substring(0, value.length - 2).toShort(),
                        ZLTextStyleEntry.SizeUnit.PIXEL,
                    )
                } else if (value.endsWith("pt")) {
                    length = ZLTextStyleEntry.Length(
                        value.substring(0, value.length - 2).toShort(),
                        ZLTextStyleEntry.SizeUnit.POINT,
                    )
                }
            } catch (e: Exception) {
                // ignore
            }
            ourCache[value] = length ?: ourNullObject
            return length
        }
    }
}
