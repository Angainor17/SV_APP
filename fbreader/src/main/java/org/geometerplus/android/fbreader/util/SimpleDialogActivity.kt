package org.geometerplus.android.fbreader.util

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.geometerplus.R
import org.geometerplus.zlibrary.core.resources.ZLResource

abstract class SimpleDialogActivity : Activity() {

    private var myTextView: TextView? = null
    private var myButtonsView: View? = null
    private var myOkButton: Button? = null
    private var myCancelButton: Button? = null
    private var myButtonsResource: ZLResource? = null
    private var myFinishListener: View.OnClickListener? = null

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        myTextView = null
        myButtonsView = null
        myOkButton = null
        myCancelButton = null
        setContentView(R.layout.simple_dialog)
    }

    protected fun textView(): TextView {
        if (myTextView == null) {
            myTextView = findViewById(R.id.simple_dialog_text)
        }
        return myTextView!!
    }

    protected fun buttonsView(): View {
        if (myButtonsView == null) {
            myButtonsView = findViewById(R.id.simple_dialog_buttons)
        }
        return myButtonsView!!
    }

    protected fun okButton(): Button {
        if (myOkButton == null) {
            myOkButton = buttonsView().findViewById(R.id.ok_button)
        }
        return myOkButton!!
    }

    protected fun cancelButton(): Button {
        if (myCancelButton == null) {
            myCancelButton = buttonsView().findViewById(R.id.cancel_button)
        }
        return myCancelButton!!
    }

    private fun buttonsResource(): ZLResource {
        if (myButtonsResource == null) {
            myButtonsResource = ZLResource.resource("dialog").getResource("button")
        }
        return myButtonsResource!!
    }

    protected fun setButtonTexts(okKey: String?, cancelKey: String?) {
        if (okKey != null) {
            okButton().text = buttonsResource().getResource(okKey).value
            okButton().visibility = View.VISIBLE
        } else {
            okButton().visibility = View.GONE
        }
        if (cancelKey != null) {
            cancelButton().text = buttonsResource().getResource(cancelKey).value
            cancelButton().visibility = View.VISIBLE
        } else {
            cancelButton().visibility = View.GONE
        }
    }

    protected fun finishListener(): View.OnClickListener {
        if (myFinishListener == null) {
            myFinishListener = View.OnClickListener { finish() }
        }
        return myFinishListener!!
    }
}
