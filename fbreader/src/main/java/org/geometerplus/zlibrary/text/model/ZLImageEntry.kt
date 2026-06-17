package org.geometerplus.zlibrary.text.model

import org.geometerplus.zlibrary.core.image.ZLImage

class ZLImageEntry(
    private val imageMap: Map<String, ZLImage>,
    @JvmField val id: String,
    @JvmField val vOffset: Short,
    @JvmField val isCover: Boolean
) {
    val image: ZLImage?
        get() = imageMap[id]
}
