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
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter

import org.geometerplus.R
import org.geometerplus.android.util.FileChooserUtil
import org.geometerplus.fbreader.Paths
import org.geometerplus.zlibrary.core.resources.ZLResource

import yuku.ambilwarna.AmbilWarnaDialog

class Chooser : ListActivity(), AdapterView.OnItemClickListener {

    private val myResource = ZLResource.resource("Preferences").getResource("colors").getResource("background")
    private val myColorChooserListener = object : AmbilWarnaDialog.OnAmbilWarnaListener {
        override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
            val result = Intent()
                .putExtra(BackgroundPreference.VALUE_KEY, "")
                .putExtra(BackgroundPreference.COLOR_KEY, color)
            setResult(RESULT_OK, result)
            finish()
        }

        override fun onCancel(dialog: AmbilWarnaDialog) {
        }
    }

    override fun onStart() {
        super.onStart()
        title = myResource.value
        val adapter = ArrayAdapter<String>(
            this, R.layout.background_chooser_item, R.id.background_chooser_item_title
        )
        val chooserResource = myResource.getResource("chooser")
        adapter.add(chooserResource.getResource("solidColor").value)
        adapter.add(chooserResource.getResource("predefined").value)
        adapter.add(chooserResource.getResource("selectFile").value)
        listAdapter = adapter
        listView.onItemClickListener = this
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        when (position) {
            0 -> {
                val buttonResource = ZLResource.resource("dialog").getResource("button")
                AmbilWarnaDialog(
                    this,
                    intent.getIntExtra(BackgroundPreference.COLOR_KEY, 0),
                    myColorChooserListener,
                    buttonResource.getResource("ok").value,
                    buttonResource.getResource("cancel").value
                ).show()
            }
            1 -> startActivityForResult(Intent(this, PredefinedImages::class.java), 1)
            2 -> {
                val initialDir: String
                val currentValue = intent.getStringExtra(BackgroundPreference.VALUE_KEY)
                if (currentValue != null && currentValue.startsWith("/")) {
                    initialDir = currentValue.substring(0, currentValue.lastIndexOf("/"))
                } else {
                    val path = Paths.wallpaperPathOption.value
                    initialDir = if (path.isNotEmpty()) path[0] else ""
                }
                FileChooserUtil.runFileChooser(
                    this, 2, myResource.value, initialDir, ".+\\.(jpe?g|png)"
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                1 -> {
                    setResult(RESULT_OK, data)
                    finish()
                }
                2 -> {
                    val paths = FileChooserUtil.filePathsFromData(data)
                    if (paths.size == 1) {
                        setResult(RESULT_OK, Intent().putExtra(
                            BackgroundPreference.VALUE_KEY, paths[0]
                        ))
                        finish()
                    }
                }
            }
        }
    }
}
