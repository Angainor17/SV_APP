package org.geometerplus.fbreader.fbreader

import org.geometerplus.fbreader.network.NetworkImage
import org.geometerplus.fbreader.network.opds.OPDSBookItem
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.zlibrary.core.image.ZLImageData
import org.geometerplus.zlibrary.core.image.ZLImageManager
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.view.ZLPaintContext
import org.geometerplus.zlibrary.text.view.ExtensionElement
import org.geometerplus.zlibrary.text.view.ZLTextElementArea
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink

class BookElement(private val view: FBView) : ExtensionElement() {

    private var item: OPDSBookItem? = null
    private var cover: NetworkImage? = null

    fun setData(item: OPDSBookItem) {
        val bookUrl = item.getUrl(UrlInfo.Type.Book)
        var coverUrl = item.getUrl(UrlInfo.Type.Image)
        if (coverUrl == null) {
            coverUrl = item.getUrl(UrlInfo.Type.Thumbnail)
        }
        if (bookUrl == null || coverUrl == null) {
            this.item = null
            this.cover = null
        } else {
            this.item = item
            this.cover = NetworkImage(coverUrl, view.Application.SystemInfo).apply {
                synchronize()
            }
        }
    }

    fun isInitialized(): Boolean = item != null && cover != null

    fun getItem(): OPDSBookItem? = item

    fun getImageData(): ZLImageData? {
        val cover = this.cover ?: return null
        return ZLImageManager.Instance().getImageData(cover)
    }

    override fun getWidth(): Int {
        // 1/\phi (= 0.618) inch width + 1/10 inch left & right margin
        return minOf(ZLibrary.Instance().displayDPI * 818 / 1000, view.textColumnWidth)
    }

    override fun getHeight(): Int {
        // 1 inch height + 1/15 inch top & bottom margin
        return ZLibrary.Instance().displayDPI * 17 / 15
    }

    override fun draw(context: ZLPaintContext, area: ZLTextElementArea) {
        val vMargin = ZLibrary.Instance().displayDPI / 15
        val hMargin = ZLibrary.Instance().displayDPI / 10
        val imageData = getImageData()
        if (imageData != null) {
            context.drawImage(
                area.xStart + hMargin, area.yEnd - vMargin,
                imageData,
                ZLPaintContext.Size(
                    area.xEnd - area.xStart - 2 * hMargin + 1,
                    area.yEnd - area.yStart - 2 * vMargin + 1
                ),
                ZLPaintContext.ScalingType.FitMaximum,
                ZLPaintContext.ColorAdjustingMode.NONE
            )
        } else {
            val color = view.getTextColor(ZLTextHyperlink.NO_LINK)
            context.setLineColor(color)
            context.setFillColor(color, 0x33)
            val xStart = area.xStart + hMargin
            val xEnd = area.xEnd - hMargin
            val yStart = area.yStart + vMargin
            val yEnd = area.yEnd - vMargin
            context.fillRectangle(xStart, yStart, xEnd, yEnd)
            context.drawLine(xStart, yStart, xStart, yEnd)
            context.drawLine(xStart, yEnd, xEnd, yEnd)
            context.drawLine(xEnd, yEnd, xEnd, yStart)
            context.drawLine(xEnd, yStart, xStart, yStart)
        }
    }
}
