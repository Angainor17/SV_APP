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

package org.geometerplus.android.fbreader

import android.app.Activity
import android.view.View
import android.widget.RelativeLayout
import org.geometerplus.R
import org.geometerplus.fbreader.fbreader.ActionCode
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.zlibrary.core.resources.ZLResource

class TextSearchPopup(fbReader: FBReaderApp) : PopupPanel(fbReader), View.OnClickListener {

    override fun getId(): String = ID

    override fun hide_() {
        reader.getTextView().clearFindResults()
        super.hide_()
    }

    @Synchronized
    override fun createControlPanel(activity: Activity, root: RelativeLayout) {
        if (myWindow != null && activity == myWindow!!.context) {
            return
        }

        activity.layoutInflater.inflate(R.layout.search_panel, root)
        myWindow = root.findViewById(R.id.search_panel)

        val resource = ZLResource.resource("textSearchPopup")
        setupButton(R.id.search_panel_previous, resource.getResource("findPrevious").value ?: "")
        setupButton(R.id.search_panel_next, resource.getResource("findNext").value ?: "")
        setupButton(R.id.search_panel_close, resource.getResource("close").value ?: "")
    }

    private fun setupButton(buttonId: Int, description: String) {
        val button = myWindow!!.findViewById<View>(buttonId)
        button.setOnClickListener(this)
        button.contentDescription = description
    }

    @Synchronized
    override fun update() {
        if (myWindow == null) {
            return
        }

        myWindow!!.findViewById<View>(R.id.search_panel_previous).isEnabled =
            reader.isActionEnabled(ActionCode.FIND_PREVIOUS)
        myWindow!!.findViewById<View>(R.id.search_panel_next).isEnabled =
            reader.isActionEnabled(ActionCode.FIND_NEXT)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.search_panel_previous -> reader.runAction(ActionCode.FIND_PREVIOUS)
            R.id.search_panel_next -> reader.runAction(ActionCode.FIND_NEXT)
            R.id.search_panel_close -> {
                reader.runAction(ActionCode.CLEAR_FIND_RESULTS)
                storePosition()
                startPosition = null
                reader.hideActivePopup()
            }
        }
    }

    companion object {
        const val ID = "TextSearchPopup"
    }
}
