package org.geometerplus.zlibrary.text.view

import androidx.collection.LruCache
import org.geometerplus.zlibrary.text.model.ZLTextModel

internal class CursorManager(
    private val myModel: ZLTextModel,
    @JvmField val ExtensionManager: ExtensionElementManager?
) : LruCache<Int, ZLTextParagraphCursor>(200) {

    override fun create(index: Int): ZLTextParagraphCursor {
        return ZLTextParagraphCursor(this, myModel, index)
    }
}
