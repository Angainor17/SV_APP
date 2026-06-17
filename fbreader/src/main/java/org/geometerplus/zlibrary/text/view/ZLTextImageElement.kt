package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.core.image.ZLImageData

class ZLTextImageElement(
    @JvmField val Id: String,
    @JvmField val ImageData: ZLImageData?,
    @JvmField val URL: String?,
    @JvmField val IsCover: Boolean
) : ZLTextElement()
