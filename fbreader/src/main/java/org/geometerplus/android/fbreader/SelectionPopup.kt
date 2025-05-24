/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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
package org.geometerplus.android.fbreader

import android.app.Activity
import android.view.View
import android.widget.RelativeLayout
import org.geometerplus.R
import org.geometerplus.fbreader.fbreader.ActionCode
import org.geometerplus.fbreader.fbreader.FBReaderApp

open class SelectionPopup(fbReader: FBReaderApp?) : PopupPanel(fbReader), View.OnClickListener {

    override fun getId(): String {
        return ID
    }

    override fun createControlPanel(activity: Activity, root: RelativeLayout) {
        if (myWindow != null && activity === myWindow.context) {
            return
        }

        activity.layoutInflater.inflate(R.layout.selection_panel, root)
        myWindow = root.findViewById<View?>(R.id.selection_panel) as SimplePopupWindow?

        setupButton(R.id.selection_panel_copy)
        setupButton(R.id.selection_panel_share)
        setupButton(R.id.selection_panel_translate)
        setupButton(R.id.selection_panel_bookmark)
        setupButton(R.id.selection_panel_question)
        setupButton(R.id.selection_panel_alert)
        setupButton(R.id.selection_panel_close)
    }

    private fun setupButton(buttonId: Int) {
        val button = myWindow.findViewById<View>(buttonId)
        button.setOnClickListener(this)
    }

    fun move(selectionStartY: Int, selectionEndY: Int) {
        if (myWindow == null) {
            return
        }

        val layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL)

        val verticalPosition = getVerticalPosition(selectionStartY, selectionEndY)

        layoutParams.addRule(verticalPosition)
        myWindow.setLayoutParams(layoutParams)
    }

    private fun getVerticalPosition(selectionStartY: Int, selectionEndY: Int): Int {
        val verticalPosition: Int
        val screenHeight = (myWindow.parent as View).height
        val diffTop = screenHeight - selectionEndY
        val diffBottom = selectionStartY
        verticalPosition = if (diffTop > diffBottom) {
            if (diffTop > myWindow.height + 20) RelativeLayout.ALIGN_PARENT_BOTTOM else RelativeLayout.CENTER_VERTICAL
        } else {
            if (diffBottom > myWindow.height + 20) RelativeLayout.ALIGN_PARENT_TOP else RelativeLayout.CENTER_VERTICAL
        }
        return verticalPosition
    }

    protected override fun update() {
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.selection_panel_copy -> Application.runAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD)
            R.id.selection_panel_share -> Application.runAction(ActionCode.SELECTION_SHARE)
            R.id.selection_panel_translate -> Application.runAction(ActionCode.SELECTION_TRANSLATE)
            R.id.selection_panel_bookmark -> Application.runAction(ActionCode.SELECTION_BOOKMARK)
            R.id.selection_panel_alert -> Application.runAction(ActionCode.ASK_QUESTION)
            R.id.selection_panel_question -> Application.runAction(ActionCode.TEL_ABOUT_MISSPELL)
            R.id.selection_panel_close -> Application.runAction(ActionCode.SELECTION_CLEAR)
        }
        Application.hideActivePopup()
    }

    companion object {
        const val ID: String = "SelectionPopup"
    }
}
