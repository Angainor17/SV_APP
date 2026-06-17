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

package org.geometerplus.android.fbreader.preferences.background

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.preference.Preference
import android.view.View
import android.widget.TextView

import org.geometerplus.R
import org.geometerplus.fbreader.fbreader.options.ColorProfile
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.ui.android.util.ZLAndroidColorUtil

open class BackgroundPreference(
    context: Context,
    private val myProfile: ColorProfile,
    private val myResource: ZLResource,
    private val myRequestCode: Int
) : Preference(context) {

    init {
        widgetLayoutResource = R.layout.background_preference
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        val titleView = view.findViewById<TextView>(R.id.background_preference_title)
        titleView.text = myResource.value

        val summaryView = view.findViewById<TextView>(R.id.background_preference_summary)
        val previewWidget = view.findViewById<View>(R.id.background_preference_widget)
        val value = myProfile.wallpaperOption.value
        if (value.isEmpty()) {
            summaryView.text = myResource.getResource("solidColor").value
            previewWidget.setBackgroundColor(
                ZLAndroidColorUtil.rgb(myProfile.backgroundOption.value)
            )
        } else {
            if (value.startsWith("/")) {
                summaryView.text = value.substring(value.lastIndexOf("/") + 1)
            } else {
                val key = value.substring(value.lastIndexOf("/") + 1, value.lastIndexOf("."))
                summaryView.text = myResource.getResource(key).value
            }
            try {
                previewWidget.background = BitmapDrawable(
                    context.resources,
                    ZLFile.createFileByPath(value)?.getInputStream()
                )
            } catch (t: Throwable) {
                // ignore
            }
        }
    }

    override fun onClick() {
        val call = Intent(context, Chooser::class.java)
            .putExtra(VALUE_KEY, myProfile.wallpaperOption.value)

        val color = myProfile.backgroundOption.value
        if (color != null) {
            call.putExtra(COLOR_KEY, ZLAndroidColorUtil.rgb(color))
        }

        (context as Activity).startActivityForResult(call, myRequestCode)
    }

    open fun update(data: Intent) {
        val value = data.getStringExtra(VALUE_KEY)
        if (value != null) {
            myProfile.wallpaperOption.value = value
        }
        val color = data.getIntExtra(COLOR_KEY, -1)
        if (color != -1) {
            myProfile.backgroundOption.setValue(ZLColor(color))
        }
        notifyChanged()
    }

    companion object {
        const val VALUE_KEY = "fbreader.background.value"
        const val COLOR_KEY = "fbreader.background.color"
    }
}
