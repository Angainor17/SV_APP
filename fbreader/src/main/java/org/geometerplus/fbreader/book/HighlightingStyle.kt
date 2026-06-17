package org.geometerplus.fbreader.book

import org.geometerplus.zlibrary.core.util.ZLColor

class HighlightingStyle(
    @JvmField val id: Int,
    @JvmField val lastUpdateTimestamp: Long,
    internal var name: String,
    internal var backgroundColor: ZLColor?,
    internal var foregroundColor: ZLColor?
) {

    fun getNameOrNull(): String? = if (name.isEmpty()) null else name

    fun setName(name: String?) {
        this.name = name ?: ""
    }

    fun getBackgroundColor(): ZLColor? = backgroundColor

    fun setBackgroundColor(bgColor: ZLColor?) {
        backgroundColor = bgColor
    }

    fun getForegroundColor(): ZLColor? = foregroundColor

    fun setForegroundColor(fgColor: ZLColor?) {
        foregroundColor = fgColor
    }
}
