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

package org.geometerplus.android.fbreader.dict

import android.app.ListActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView

import org.geometerplus.R
import org.geometerplus.android.util.PackageUtil
import org.geometerplus.android.util.UIMessageUtil
import org.geometerplus.zlibrary.core.resources.ZLResource

class DictionaryNotInstalledActivity : ListActivity() {

    private lateinit var myResource: ZLResource
    private lateinit var myDictionaryName: String
    private lateinit var myPackageName: String

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        myResource = ZLResource.resource("dialog").getResource("missingDictionary")
        myDictionaryName = intent.getStringExtra(DICTIONARY_NAME_KEY) ?: ""
        myPackageName = intent.getStringExtra(PACKAGE_NAME_KEY) ?: ""
        title = myResource.value.replace("%s", myDictionaryName)
        val adapter = Adapter()
        listAdapter = adapter
        listView.onItemClickListener = adapter
    }

    private fun installDictionary() {
        if (!PackageUtil.installFromMarket(this, myPackageName)) {
            UIMessageUtil.showErrorMessage(this, "cannotRunAndroidMarket", myDictionaryName)
        }
    }

    private inner class Adapter : BaseAdapter(), AdapterView.OnItemClickListener {
        private val myItems = arrayOf("install", "configure", "skip")

        override fun getCount(): Int = myItems.size

        override fun getItem(position: Int): String = myItems[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(parent.context).inflate(R.layout.menu_item, parent, false)
            val titleView = view.findViewById<TextView>(R.id.menu_item_title)
            titleView.text = myResource.getResource(myItems[position]).value.replace("%s", myDictionaryName)
            return view
        }

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            when (position) {
                0 -> installDictionary() // install
                1 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("fbreader-action:preferences#dictionary"))) // configure
                2 -> { } // skip
            }
            finish()
        }
    }

    companion object {
        const val DICTIONARY_NAME_KEY = "fbreader.dictionary.name"
        const val PACKAGE_NAME_KEY = "fbreader.package.name"
    }
}
