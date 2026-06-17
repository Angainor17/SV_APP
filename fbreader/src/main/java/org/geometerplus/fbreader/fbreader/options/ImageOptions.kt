package org.geometerplus.fbreader.fbreader.options

import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLColorOption
import org.geometerplus.zlibrary.core.options.ZLEnumOption
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.text.view.ZLTextViewBase

class ImageOptions {

    @JvmField val imageViewBackground = ZLColorOption("Colors", "ImageViewBackground", ZLColor(255, 255, 255))
    @JvmField val fitToScreen = ZLEnumOption("Options", "FitImagesToScreen", ZLTextViewBase.ImageFitting.covers)
    @JvmField val tapAction = ZLEnumOption("Options", "ImageTappingAction", TapActionEnum.openImageView)
    @JvmField val matchBackground = ZLBooleanOption("Colors", "ImageMatchBackground", true)

    enum class TapActionEnum {
        doNothing, selectImage, openImageView
    }
}
