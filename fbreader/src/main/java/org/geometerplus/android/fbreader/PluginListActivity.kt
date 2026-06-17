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
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import org.geometerplus.R
import org.geometerplus.android.util.PackageUtil
import org.geometerplus.android.util.ViewUtil
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.util.XmlUtil
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.lang.reflect.Field

class PluginListActivity : ListActivity() {
    private val myResource = ZLResource.resource("plugins")

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        title = myResource.value
        val adapter = PluginListAdapter()
        listAdapter = adapter
        listView.onItemClickListener = adapter
    }

    private class Plugin(val id: String, val packageName: String)

    private inner class Reader(private val myPlugins: MutableList<Plugin>) : DefaultHandler() {
        private val myPackageManager = packageManager

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            if ("plugin" == localName) {
                try {
                    if (attributes.getValue("min-api").toInt() > Build.VERSION.SDK_INT) {
                        return
                    }
                } catch (t: Throwable) {
                    // ignore
                }
                val id = attributes.getValue("id")
                val packageName = attributes.getValue("package")
                try {
                    myPackageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                } catch (e: PackageManager.NameNotFoundException) {
                    myPlugins.add(Plugin(id, packageName))
                }
            }
        }
    }

    private inner class PluginListAdapter : BaseAdapter(), AdapterView.OnItemClickListener {
        private val myPlugins = mutableListOf<Plugin>()

        init {
            XmlUtil.parseQuietly(
                ZLFile.createFileByPath("default/plugins.xml"),
                Reader(myPlugins)
            )
        }

        override fun getCount(): Int = if (myPlugins.isEmpty()) 1 else myPlugins.size

        override fun getItem(position: Int): Plugin? = if (myPlugins.isEmpty()) null else myPlugins[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(parent.context).inflate(R.layout.plugin_item, parent, false)
            val iconView: ImageView = view.findViewById(R.id.plugin_item_icon)
            val titleView = ViewUtil.findTextView(view, R.id.plugin_item_title)
            val summaryView = ViewUtil.findTextView(view, R.id.plugin_item_summary)
            val plugin = getItem(position)
            if (plugin != null) {
                val resource = myResource.getResource(plugin.id)
                titleView.text = resource.value
                summaryView.text = resource.getResource("summary").value
                var iconId = R.drawable.fbreader
                try {
                    val f: Field = R.drawable::class.java.getField("plugin_" + plugin.id)
                    iconId = f.getInt(R.drawable::class.java)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
                iconView.setImageResource(iconId)
            } else {
                val resource = myResource.getResource("noMorePlugins")
                titleView.text = resource.value
                summaryView.visibility = View.GONE
                iconView.visibility = View.GONE
            }
            return view
        }

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val plugin = getItem(position)
            if (plugin != null) {
                runOnUiThread {
                    finish()
                    PackageUtil.installFromMarket(this@PluginListActivity, plugin.packageName)
                }
            }
        }
    }
}
