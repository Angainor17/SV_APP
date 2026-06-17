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
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import org.geometerplus.R
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor

class NavigationPopup(fbReader: FBReaderApp) : ZLApplication.PopupPanel(fbReader) {

    private val myFBReader: FBReaderApp = fbReader
    @Volatile
    private var myWindow: NavigationWindow? = null
    @Volatile
    private var myActivity: Activity? = null
    @Volatile
    private var myRoot: RelativeLayout? = null
    private var myStartPosition: ZLTextWordCursor? = null
    @Volatile
    private var myIsInProgress = false

    fun setPanelInfo(activity: Activity, root: RelativeLayout) {
        myActivity = activity
        myRoot = root
    }

    fun runNavigation() {
        if (myWindow == null || myWindow!!.visibility == View.GONE) {
            myIsInProgress = false
            if (myStartPosition == null) {
                myStartPosition = ZLTextWordCursor(myFBReader.getTextView().getStartCursor())
            }
            Application.showPopup(ID)
        }
    }

    override fun show_() {
        if (myActivity != null) {
            createPanel(myActivity!!, myRoot!!)
        }
        myWindow?.let {
            it.show()
            setupNavigation()
        }
    }

    fun showPopup() {
        show_()
    }

    override fun hide_() {
        myWindow?.hide()
    }

    override fun getId(): String = ID

    override fun update() {
        if (!myIsInProgress && myWindow != null) {
            setupNavigation()
        }
    }

    private fun createPanel(activity: Activity, root: RelativeLayout) {
        if (myWindow != null && activity == myWindow!!.context) {
            return
        }

        activity.layoutInflater.inflate(R.layout.navigation_panel, root)
        myWindow = root.findViewById(R.id.navigation_panel)

        val slider: SeekBar = myWindow!!.findViewById(R.id.navigation_slider)
        val text: TextView = myWindow!!.findViewById(R.id.navigation_text)

        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            private fun gotoPage(page: Int) {
                val view = myFBReader.getTextView()
                if (page == 1) {
                    view.gotoHome()
                } else {
                    view.gotoPage(page)
                }
                myFBReader.viewWidget.reset()
                myFBReader.viewWidget.repaint()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                myIsInProgress = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                myIsInProgress = false
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val page = progress + 1
                    val pagesNumber = seekBar.max + 1
                    gotoPage(page)
                    text.text = makeProgressText(page, pagesNumber)
                }
            }
        })

        val btnOk: Button = myWindow!!.findViewById(R.id.navigation_ok)
        val btnCancel: Button = myWindow!!.findViewById(R.id.navigation_cancel)
        val listener = View.OnClickListener { v ->
            val position = myStartPosition
            if (v == btnCancel && position != null) {
                myFBReader.getTextView().gotoPosition(position)
            } else if (v == btnOk) {
                if (myStartPosition != null && myStartPosition != myFBReader.getTextView().getStartCursor()) {
                    myFBReader.addInvisibleBookmark(myStartPosition!!)
                    myFBReader.storePosition()
                }
            }
            myStartPosition = null
            Application.hideActivePopup()
            myFBReader.viewWidget.reset()
            myFBReader.viewWidget.repaint()
        }
        btnOk.setOnClickListener(listener)
        btnCancel.setOnClickListener(listener)

        val buttonResource = ZLResource.resource("dialog").getResource("button")
        btnOk.text = buttonResource.getResource("ok").value
        btnCancel.text = buttonResource.getResource("cancel").value
    }

    private fun setupNavigation() {
        val slider: SeekBar = myWindow!!.findViewById(R.id.navigation_slider)
        val text: TextView = myWindow!!.findViewById(R.id.navigation_text)

        val textView = myFBReader.getTextView()
        val pagePosition = textView.pagePosition()

        if (slider.max != pagePosition.total - 1 || slider.progress != pagePosition.current - 1) {
            slider.max = pagePosition.total - 1
            slider.progress = pagePosition.current - 1
            text.text = makeProgressText(pagePosition.current, pagePosition.total)
        }
    }

    private fun makeProgressText(page: Int, pagesNumber: Int): String {
        val builder = StringBuilder()
        builder.append(page)
        builder.append("/")
        builder.append(pagesNumber)
        val tocElement = myFBReader.currentTOCElement
        if (tocElement != null) {
            builder.append("  ")
            builder.append(tocElement.getText())
        }
        return builder.toString()
    }

    internal fun removeWindow(activity: Activity) {
        if (myWindow != null && activity == myWindow!!.context) {
            val root = myWindow!!.parent as ViewGroup
            myWindow!!.hide()
            root.removeView(myWindow)
            myWindow = null
        }
    }

    companion object {
        const val ID = "NavigationPopup"
    }
}
