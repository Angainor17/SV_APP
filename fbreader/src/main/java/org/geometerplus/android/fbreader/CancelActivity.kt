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

import android.app.ListActivity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.LinearLayout
import org.geometerplus.R
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow
import org.geometerplus.android.util.ViewUtil
import org.geometerplus.fbreader.fbreader.options.CancelMenuHelper

class CancelActivity : ListActivity() {
    private var myCollection: BookCollectionShadow? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    override fun onStart() {
        super.onStart()
        // we use this local variable to be sure collection is not null inside the runnable
        val collection = BookCollectionShadow()
        myCollection = collection
        collection.bindToService(this) {
            val adapter = ActionListAdapter(
                CancelMenuHelper().getActionsList(collection)
            )
            listAdapter = adapter
            listView.onItemClickListener = adapter
        }
    }

    override fun onStop() {
        myCollection?.unbind()
        myCollection = null
        super.onStop()
    }

    private inner class ActionListAdapter(
        private val myActions: List<CancelMenuHelper.ActionDescription>
    ) : BaseAdapter(), AdapterView.OnItemClickListener {

        override fun getCount(): Int = myActions.size

        override fun getItem(position: Int): CancelMenuHelper.ActionDescription = myActions[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(parent.context).inflate(R.layout.cancel_item, parent, false)
            val item = getItem(position)
            val titleView = ViewUtil.findTextView(view, R.id.cancel_item_title)
            val summaryView = ViewUtil.findTextView(view, R.id.cancel_item_summary)
            val title = item.title
            val summary = item.summary
            titleView.text = title
            if (summary != null) {
                summaryView.visibility = View.VISIBLE
                summaryView.text = summary
                titleView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
            } else {
                summaryView.visibility = View.GONE
                titleView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
                )
            }
            return view
        }

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val data = Intent()
            val item = getItem(position)
            data.putExtra(FBReaderIntents.Key.TYPE, item.type.name)
            if (item is CancelMenuHelper.BookmarkDescription) {
                FBReaderIntents.putBookmarkExtra(
                    data, item.bookmark
                )
            }
            setResult(RESULT_FIRST_USER, data)
            finish()
        }
    }
}
