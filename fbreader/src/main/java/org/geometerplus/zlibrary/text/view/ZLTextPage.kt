package org.geometerplus.zlibrary.text.view

class ZLTextPage {
    internal val startCursor = ZLTextWordCursor()
    internal val endCursor = ZLTextWordCursor()
    internal val lineInfos = mutableListOf<ZLTextLineInfo>()
    @JvmField var textElementMap = ZLTextElementAreaVector()
    var topMargin: Int = 0
    internal var column0Height: Int = 0
    internal var paintState: Int = PaintStateEnum.NOTHING_TO_PAINT

    private var columnWidth: Int = 0
    private var height: Int = 0
    private var twoColumnView: Boolean = false

    fun setSize(columnWidth: Int, height: Int, twoColumnView: Boolean, keepEndNotStart: Boolean) {
        if (this.columnWidth == columnWidth && this.height == height && this.columnWidth == columnWidth) {
            return
        }
        this.columnWidth = columnWidth
        this.height = height
        this.twoColumnView = twoColumnView

        if (paintState != PaintStateEnum.NOTHING_TO_PAINT) {
            lineInfos.clear()
            if (keepEndNotStart) {
                if (!endCursor.isNull) {
                    startCursor.reset()
                    paintState = PaintStateEnum.END_IS_KNOWN
                } else if (!startCursor.isNull) {
                    endCursor.reset()
                    paintState = PaintStateEnum.START_IS_KNOWN
                }
            } else {
                if (!startCursor.isNull) {
                    endCursor.reset()
                    paintState = PaintStateEnum.START_IS_KNOWN
                } else if (!endCursor.isNull) {
                    startCursor.reset()
                    paintState = PaintStateEnum.END_IS_KNOWN
                }
            }
        }
    }

    fun reset() {
        startCursor.reset()
        endCursor.reset()
        lineInfos.clear()
        paintState = PaintStateEnum.NOTHING_TO_PAINT
    }

    fun moveStartCursor(cursor: ZLTextParagraphCursor) {
        startCursor.setCursor(cursor)
        endCursor.reset()
        lineInfos.clear()
        paintState = PaintStateEnum.START_IS_KNOWN
    }

    fun moveStartCursor(paragraphIndex: Int, wordIndex: Int, charIndex: Int) {
        if (startCursor.isNull) {
            startCursor.setCursor(endCursor)
        }
        startCursor.moveToParagraph(paragraphIndex)
        startCursor.moveTo(wordIndex, charIndex)
        endCursor.reset()
        lineInfos.clear()
        paintState = PaintStateEnum.START_IS_KNOWN
    }

    fun moveEndCursor(paragraphIndex: Int, wordIndex: Int, charIndex: Int) {
        if (endCursor.isNull) {
            endCursor.setCursor(startCursor)
        }
        endCursor.moveToParagraph(paragraphIndex)
        if (paragraphIndex > 0 && wordIndex == 0 && charIndex == 0) {
            endCursor.previousParagraph()
            endCursor.moveToParagraphEnd()
        } else {
            endCursor.moveTo(wordIndex, charIndex)
        }
        startCursor.reset()
        lineInfos.clear()
        paintState = PaintStateEnum.END_IS_KNOWN
    }

    fun getTextWidth(): Int = columnWidth

    fun getTextHeight(): Int = height

    fun twoColumnView(): Boolean = twoColumnView

    fun isEmptyPage(): Boolean {
        for (info in lineInfos) {
            if (info.isVisible) {
                return false
            }
        }
        return true
    }

    fun findLineFromStart(cursor: ZLTextWordCursor, overlappingValue: Int) {
        if (lineInfos.isEmpty() || overlappingValue == 0) {
            cursor.reset()
            return
        }
        var info: ZLTextLineInfo? = null
        var remaining = overlappingValue
        for (i in lineInfos) {
            info = i
            if (info.isVisible) {
                --remaining
                if (remaining == 0) {
                    break
                }
            }
        }
        cursor.setCursor(info!!.paragraphCursor)
        cursor.moveTo(info.endElementIndex, info.endCharIndex)
    }

    fun findLineFromEnd(cursor: ZLTextWordCursor, overlappingValue: Int) {
        if (lineInfos.isEmpty() || overlappingValue == 0) {
            cursor.reset()
            return
        }
        val infos = lineInfos
        val size = infos.size
        var info: ZLTextLineInfo? = null
        var remaining = overlappingValue
        for (i in size - 1 downTo 0) {
            info = infos[i]
            if (info!!.isVisible) {
                --remaining
                if (remaining == 0) {
                    break
                }
            }
        }
        cursor.setCursor(info!!.paragraphCursor)
        cursor.moveTo(info.startElementIndex, info.startCharIndex)
    }

    fun findPercentFromStart(cursor: ZLTextWordCursor, percent: Int) {
        if (lineInfos.isEmpty()) {
            cursor.reset()
            return
        }
        var h = height * percent / 100
        var visibleLineOccured = false
        var info: ZLTextLineInfo? = null
        for (i in lineInfos) {
            info = i
            if (info.isVisible) {
                visibleLineOccured = true
            }
            h -= info.height + info.descent + info.vSpaceAfter
            if (visibleLineOccured && h <= 0) {
                break
            }
        }
        cursor.setCursor(info!!.paragraphCursor)
        cursor.moveTo(info.endElementIndex, info.endCharIndex)
    }
}
