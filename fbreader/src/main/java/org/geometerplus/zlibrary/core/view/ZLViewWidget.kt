package org.geometerplus.zlibrary.core.view

interface ZLViewWidget {
    fun reset()
    fun repaint()
    fun startManualScrolling(x: Int, y: Int, direction: ZLViewEnums.Direction)
    fun scrollManuallyTo(x: Int, y: Int)
    fun startAnimatedScrolling(pageIndex: ZLViewEnums.PageIndex, x: Int, y: Int, direction: ZLViewEnums.Direction, speed: Int)
    fun startAnimatedScrolling(pageIndex: ZLViewEnums.PageIndex, direction: ZLViewEnums.Direction, speed: Int)
    fun startAnimatedScrolling(x: Int, y: Int, speed: Int)
    fun getScreenBrightness(): Int
    fun setScreenBrightness(percent: Int)
}
