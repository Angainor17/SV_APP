package org.geometerplus.android.fbreader

import android.content.Intent
import android.os.Parcelable
import android.view.View
import com.github.johnpersano.supertoasts.SuperActivityToast
import com.github.johnpersano.supertoasts.SuperToast
import com.github.johnpersano.supertoasts.util.OnClickWrapper
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.bookmark.EditBookmarkActivity
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.zlibrary.core.resources.ZLResource

internal class SelectionBookmarkAction(baseApplication: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseApplication, fbreader) {

    override fun run(vararg params: Any?) {
        val bookmark: Bookmark? = if (params.isNotEmpty()) {
            params[0] as Bookmark
        } else {
            Reader.addSelectionBookmark()
        }
        if (bookmark == null) {
            return
        }

        val toast = SuperActivityToast(BaseActivity, SuperToast.Type.BUTTON)
        toast.setText(bookmark.text)
        toast.setDuration(SuperToast.Duration.EXTRA_LONG)
        toast.setButtonIcon(
            android.R.drawable.ic_menu_edit,
            ZLResource.resource("dialog").getResource("button").getResource("edit").value ?: ""
        )
        toast.setOnClickWrapper(OnClickWrapper("bkmk", object : SuperToast.OnClickListener {
            override fun onClick(view: View, token: Parcelable?) {
                val intent =
                    Intent(BaseActivity.applicationContext, EditBookmarkActivity::class.java)
                FBReaderIntents.putBookmarkExtra(intent, bookmark)
                OrientationUtil.startActivity(BaseActivity, intent)
            }
        }))
        BaseActivity.showToast(toast)
    }
}
