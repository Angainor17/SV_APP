/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.tips

import android.app.Activity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import org.geometerplus.R
import org.geometerplus.fbreader.Paths
import org.geometerplus.fbreader.tips.TipsManager
import org.geometerplus.zlibrary.core.resources.ZLResource

class TipsActivity : Activity() {

    private lateinit var myManager: TipsManager

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        myManager = TipsManager(Paths.systemInfo(this))

        val doInitialize = INITIALIZE_ACTION == intent.action

        setContentView(R.layout.tip)
        val dialogResource = ZLResource.resource("dialog")
        val resource = dialogResource.getResource("tips")
        val buttonResource = dialogResource.getResource("button")
        val checkBox: CheckBox = findViewById(R.id.tip_checkbox)

        title = resource.getResource("title").value

        if (doInitialize) {
            checkBox.visibility = View.GONE

            showText(resource.getResource("initializationText").value)

            val yesButton: Button = findViewById<View>(R.id.tip_buttons).findViewById(R.id.ok_button)
            yesButton.text = buttonResource.getResource("yes").value
            yesButton.setOnClickListener {
                TipsManager.tipsAreInitializedOption.value = true
                TipsManager.showTipsOption.value = true
                myManager.startDownloading()
                finish()
            }

            val noButton: Button = findViewById<View>(R.id.tip_buttons).findViewById(R.id.cancel_button)
            noButton.text = buttonResource.getResource("no").value
            noButton.setOnClickListener {
                TipsManager.tipsAreInitializedOption.value = true
                TipsManager.showTipsOption.value = false
                finish()
            }
        } else {
            checkBox.text = resource.getResource("dontShowAgain").value

            val okButton: Button = findViewById<View>(R.id.tip_buttons).findViewById(R.id.ok_button)
            okButton.text = buttonResource.getResource("ok").value
            okButton.setOnClickListener {
                TipsManager.showTipsOption.value = !checkBox.isChecked
                finish()
            }

            val nextTipButton: Button = findViewById<View>(R.id.tip_buttons).findViewById(R.id.cancel_button)
            nextTipButton.text = resource.getResource("more").value
            nextTipButton.setOnClickListener {
                showTip(nextTipButton)
            }

            showTip(nextTipButton)
        }
    }

    private fun showTip(nextTipButton: Button) {
        val tip = myManager.getNextTip()
        if (tip != null) {
            title = tip.Title
            showText(tip.Content)
        }
        nextTipButton.isEnabled = myManager.hasNextTip()
    }

    private fun showText(text: CharSequence) {
        val textView: TextView = findViewById(R.id.tip_text)
        textView.text = text
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    companion object {
        const val INITIALIZE_ACTION = "android.fbreader.action.tips.INITIALIZE"
        const val SHOW_TIP_ACTION = "android.fbreader.action.tips.SHOW_TIP"
    }
}
