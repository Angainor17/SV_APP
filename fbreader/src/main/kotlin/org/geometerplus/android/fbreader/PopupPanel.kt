/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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
import android.view.ViewGroup
import android.widget.RelativeLayout
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor

abstract class PopupPanel(fbReader: FBReaderApp) : ZLApplication.PopupPanel(fbReader) {

    @JvmField
    var StartPosition: ZLTextWordCursor? = null

    @Volatile
    protected var myWindow: SimplePopupWindow? = null

    @Volatile
    private var myActivity: Activity? = null

    @Volatile
    private var myRoot: RelativeLayout? = null

    protected fun getReader(): FBReaderApp = Application as FBReaderApp

    public override fun show_() {
        val activity = myActivity
        if (activity != null) {
            createControlPanel(activity, myRoot)
        }
        val window = myWindow
        if (window != null) {
            window.show()
        }
    }

    public override fun hide_() {
        val window = myWindow
        if (window != null) {
            window.hide()
        }
    }

    private fun removeWindow(activity: Activity) {
        val window = myWindow ?: return
        if (activity === window.getContext()) {
            val root = window.getParent() as ViewGroup
            window.hide()
            root.removeView(window)
            myWindow = null
        }
    }

    fun initPosition() {
        if (StartPosition == null) {
            StartPosition = ZLTextWordCursor(getReader().textView.startCursor)
        }
    }

    fun storePosition() {
        val start = StartPosition ?: return
        val reader = getReader()
        if (!start.equals(reader.textView.startCursor)) {
            reader.addInvisibleBookmark(start)
            reader.storePosition()
        }
    }

    fun setPanelInfo(activity: Activity, root: RelativeLayout?) {
        myActivity = activity
        myRoot = root
    }

    abstract fun createControlPanel(activity: Activity, root: RelativeLayout?)

    companion object {
        @JvmStatic
        fun removeAllWindows(application: ZLApplication, activity: Activity) {
            for (popup in application.popupPanels()) {
                if (popup is PopupPanel) {
                    popup.removeWindow(activity)
                }
            }
        }

        @JvmStatic
        fun restoreVisibilities(application: ZLApplication) {
            val popup = application.getActivePopup()
            if (popup is PopupPanel) {
                popup.show_()
            }
        }
    }
}
