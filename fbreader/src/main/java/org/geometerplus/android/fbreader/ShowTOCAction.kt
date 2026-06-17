package org.geometerplus.android.fbreader

import android.content.Intent
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class ShowTOCAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {

    override fun isVisible(): Boolean = isTOCAvailable(Reader)

    override fun run(vararg params: Any?) {
        OrientationUtil.startActivity(
            BaseActivity, Intent(BaseActivity.applicationContext, TOCActivity::class.java)
        )
    }

    companion object {
        fun isTOCAvailable(reader: FBReaderApp?): Boolean {
            if (reader == null) {
                return false
            }
            val model: BookModel? = reader.model
            return model != null && model.tocTree.hasChildren()
        }
    }
}
