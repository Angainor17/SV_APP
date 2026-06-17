package org.geometerplus.zlibrary.text.view.style

import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.text.model.ZLTextMetrics
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink
import org.geometerplus.zlibrary.text.view.ZLTextStyle

abstract class ZLTextDecoratedStyle(base: ZLTextStyle, hyperlink: ZLTextHyperlink?) : ZLTextStyle(base, hyperlink ?: base.hyperlink) {

    protected val baseStyle: ZLTextBaseStyle = when (base) {
        is ZLTextBaseStyle -> base
        else -> (base as ZLTextDecoratedStyle).baseStyle
    }

    private var fontEntries: List<FontEntry>? = null
    private var isItalicCached: Boolean = false
    private var isBoldCached: Boolean = false
    private var isUnderlineCached: Boolean = false
    private var isStrikeThroughCached: Boolean = false
    private var lineSpacePercentCached: Int = 0

    private var isNotCached: Boolean = true

    private var fontSizeCached: Int = 0
    private var spaceBeforeCached: Int = 0
    private var spaceAfterCached: Int = 0
    private var verticalAlignCached: Int = 0
    private var isVerticallyAlignedCached: Boolean? = null
    private var leftMarginCached: Int = 0
    private var rightMarginCached: Int = 0
    private var leftPaddingCached: Int = 0
    private var rightPaddingCached: Int = 0
    private var firstLineIndentCached: Int = 0
    private var metricsCached: ZLTextMetrics? = null

    private fun initCache() {
        fontEntries = getFontEntriesInternal()
        isItalicCached = isItalicInternal()
        isBoldCached = isBoldInternal()
        isUnderlineCached = isUnderlineInternal()
        isStrikeThroughCached = isStrikeThroughInternal()
        lineSpacePercentCached = getLineSpacePercentInternal()
        isNotCached = false
    }

    private fun initMetricsCache(metrics: ZLTextMetrics) {
        metricsCached = metrics
        fontSizeCached = getFontSizeInternal(metrics)
        spaceBeforeCached = getSpaceBeforeInternal(metrics, fontSizeCached)
        spaceAfterCached = getSpaceAfterInternal(metrics, fontSizeCached)
        verticalAlignCached = getVerticalAlignInternal(metrics, fontSizeCached)
        leftMarginCached = getLeftMarginInternal(metrics, fontSizeCached)
        rightMarginCached = getRightMarginInternal(metrics, fontSizeCached)
        leftPaddingCached = getLeftPaddingInternal(metrics, fontSizeCached)
        rightPaddingCached = getRightPaddingInternal(metrics, fontSizeCached)
        firstLineIndentCached = getFirstLineIndentInternal(metrics, fontSizeCached)
    }

    override fun getFontEntries(): List<FontEntry> {
        if (isNotCached) initCache()
        return fontEntries!!
    }

    protected abstract fun getFontEntriesInternal(): List<FontEntry>

    override fun getFontSize(metrics: ZLTextMetrics): Int {
        if (metrics != metricsCached) initMetricsCache(metrics)
        return fontSizeCached
    }

    protected abstract fun getFontSizeInternal(metrics: ZLTextMetrics): Int

    override fun getSpaceBefore(metrics: ZLTextMetrics): Int {
        if (metrics != metricsCached) initMetricsCache(metrics)
        return spaceBeforeCached
    }

    protected abstract fun getSpaceBeforeInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    override fun getSpaceAfter(metrics: ZLTextMetrics): Int {
        if (metrics != metricsCached) initMetricsCache(metrics)
        return spaceAfterCached
    }

    protected abstract fun getSpaceAfterInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    override fun isItalic(): Boolean {
        if (isNotCached) initCache()
        return isItalicCached
    }

    protected abstract fun isItalicInternal(): Boolean

    override fun isBold(): Boolean {
        if (isNotCached) initCache()
        return isBoldCached
    }

    protected abstract fun isBoldInternal(): Boolean

    override fun isUnderline(): Boolean {
        if (isNotCached) initCache()
        return isUnderlineCached
    }

    protected abstract fun isUnderlineInternal(): Boolean

    override fun isStrikeThrough(): Boolean {
        if (isNotCached) initCache()
        return isStrikeThroughCached
    }

    protected abstract fun isStrikeThroughInternal(): Boolean

    override fun getVerticalAlign(metrics: ZLTextMetrics): Int {
        if (metrics != metricsCached) initMetricsCache(metrics)
        return verticalAlignCached
    }

    protected abstract fun getVerticalAlignInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    override fun isVerticallyAligned(): Boolean {
        if (isVerticallyAlignedCached == null) {
            isVerticallyAlignedCached = parent?.isVerticallyAligned() == true || isVerticallyAlignedInternal()
        }
        return isVerticallyAlignedCached!!
    }

    protected abstract fun isVerticallyAlignedInternal(): Boolean

    override fun getLeftMargin(metrics: ZLTextMetrics): Int {
        if (metrics != metricsCached) initMetricsCache(metrics)
        return leftMarginCached
    }

    protected abstract fun getLeftMarginInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    override fun getRightMargin(metrics: ZLTextMetrics): Int {
        if (metrics != metricsCached) initMetricsCache(metrics)
        return rightMarginCached
    }

    protected abstract fun getRightMarginInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    override fun getLeftPadding(metrics: ZLTextMetrics): Int {
        if (metrics != metricsCached) initMetricsCache(metrics)
        return leftPaddingCached
    }

    protected abstract fun getLeftPaddingInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    override fun getRightPadding(metrics: ZLTextMetrics): Int {
        if (metrics != metricsCached) initMetricsCache(metrics)
        return rightPaddingCached
    }

    protected abstract fun getRightPaddingInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    override fun getFirstLineIndent(metrics: ZLTextMetrics): Int {
        if (metrics != metricsCached) initMetricsCache(metrics)
        return firstLineIndentCached
    }

    protected abstract fun getFirstLineIndentInternal(metrics: ZLTextMetrics, fontSize: Int): Int

    override fun getLineSpacePercent(): Int {
        if (isNotCached) initCache()
        return lineSpacePercentCached
    }

    protected abstract fun getLineSpacePercentInternal(): Int
}
