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

package org.geometerplus.android.fbreader.network

import android.app.ListActivity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView

import org.geometerplus.R
import org.geometerplus.android.fbreader.api.PluginApi

internal abstract class MenuActivity : ListActivity(), AdapterView.OnItemClickListener {

    protected lateinit var myInfos: MutableList<PluginApi.MenuActionInfo>

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        myInfos = ArrayList()

        init()

        try {
            startActivityForResult(Intent(action, intent.data), 0)
        } catch (e: ActivityNotFoundException) {
            if (finishInitialization()) {
                return
            }
        }

        listAdapter = ActionListAdapter()
        listView.onItemClickListener = this
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        runItem(myInfos[position])
        finish()
    }

    private fun finishInitialization(): Boolean {
        return when (myInfos.size) {
            0 -> {
                finish()
                true
            }
            1 -> {
                runItem(myInfos[0])
                finish()
                true
            }
            else -> false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (intent != null) {
            val actions = intent.getParcelableArrayListExtra<PluginApi.MenuActionInfo>(
                PluginApi.PluginInfo.KEY
            )
            if (actions != null) {
                myInfos.addAll(actions)
            }
            if (finishInitialization()) {
                return
            }
            myInfos.sort()
            (listAdapter as ActionListAdapter).notifyDataSetChanged()
            listView.invalidateViews()
        }
    }

    protected abstract fun init()

    protected abstract val action: String

    protected abstract fun runItem(info: PluginApi.MenuActionInfo)

    private inner class ActionListAdapter : BaseAdapter() {
        override fun getCount(): Int = myInfos.size

        override fun getItem(position: Int): PluginApi.MenuActionInfo = myInfos[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(parent.context).inflate(R.layout.menu_item, parent, false)
            val titleView = view.findViewById<TextView>(R.id.menu_item_title)
            titleView.text = getItem(position).menuItemName
            return view
        }
    }
}
