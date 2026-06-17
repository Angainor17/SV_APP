package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.text.model.ZLTextMetrics

abstract class ZLTextStyle(
    @JvmField val Parent: ZLTextStyle?,
    @JvmField val Hyperlink: ZLTextHyperlink?
) {
    val parent: ZLTextStyle? get() = Parent
    val hyperlink: ZLTextHyperlink? get() = Hyperlink

    abstract fun getFontEntries(): List<FontEntry>
    abstract fun getFontSize(metrics: ZLTextMetrics): Int
    abstract fun isBold(): Boolean
    abstract fun isItalic(): Boolean
    abstract fun isUnderline(): Boolean
    abstract fun isStrikeThrough(): Boolean

    fun getLeftIndent(metrics: ZLTextMetrics): Int = getLeftMargin(metrics) + getLeftPadding(metrics)
    fun getRightIndent(metrics: ZLTextMetrics): Int = getRightMargin(metrics) + getRightPadding(metrics)

    abstract fun getLeftMargin(metrics: ZLTextMetrics): Int
    abstract fun getRightMargin(metrics: ZLTextMetrics): Int
    abstract fun getLeftPadding(metrics: ZLTextMetrics): Int
    abstract fun getRightPadding(metrics: ZLTextMetrics): Int
    abstract fun getFirstLineIndent(metrics: ZLTextMetrics): Int
    abstract fun getLineSpacePercent(): Int
    abstract fun getVerticalAlign(metrics: ZLTextMetrics): Int
    abstract fun isVerticallyAligned(): Boolean
    abstract fun getSpaceBefore(metrics: ZLTextMetrics): Int
    abstract fun getSpaceAfter(metrics: ZLTextMetrics): Int
    abstract fun getAlignment(): Byte
    abstract fun allowHyphenations(): Boolean
}
