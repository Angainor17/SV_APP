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

import android.app.ListActivity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView

import org.geometerplus.R
import org.geometerplus.fbreader.fbreader.WallpapersUtil
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.resources.ZLResource

class PredefinedImages : ListActivity(), AdapterView.OnItemClickListener {

    private val myResource = ZLResource.resource("Preferences").getResource("colors").getResource("background")

    override fun onStart() {
        super.onStart()
        title = myResource.value
        val adapter = object : ArrayAdapter<ZLFile>(
            this, R.layout.background_predefined_item, R.id.background_predefined_item_title
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)

                val titleView = view.findViewById<TextView>(R.id.background_predefined_item_title)
                val name = getItem(position)!!.shortName
                val key = name.substring(0, name.indexOf("."))
                titleView.text = myResource.getResource(key).value

                val previewWidget = view.findViewById<View>(R.id.background_predefined_item_preview)
                try {
                    previewWidget.background = BitmapDrawable(resources, getItem(position)!!.getInputStream())
                } catch (t: Throwable) {
                }

                return view
            }
        }
        for (file in WallpapersUtil.predefinedWallpaperFiles()) {
            adapter.add(file)
        }
        listAdapter = adapter
        listView.onItemClickListener = this
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        setResult(RESULT_OK, Intent().putExtra(
            BackgroundPreference.VALUE_KEY,
            (listAdapter.getItem(position) as ZLFile).path
        ))
        finish()
    }
}
