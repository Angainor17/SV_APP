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

package org.geometerplus.android.fbreader.bookmark

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TabHost
import android.widget.TextView
import org.geometerplus.R
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow
import org.geometerplus.android.util.ViewUtil
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookEvent
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.BookmarkUtil
import org.geometerplus.fbreader.book.HighlightingStyle
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.resources.ZLResource
import yuku.ambilwarna.widget.AmbilWarnaPrefWidgetView

class EditBookmarkActivity : Activity(), IBookCollection.Listener<Book> {

    private val myResource = ZLResource.resource("editBookmark")
    private val myCollection = BookCollectionShadow()
    private var myBookmark: Bookmark? = null
    private var myStylesAdapter: StyleListAdapter? = null

    private fun addTab(host: TabHost, id: String, content: Int) {
        val spec = host.newTabSpec(id)
        spec.setIndicator(myResource.getResource(id).value)
        spec.setContent(content)
        host.addTab(spec)
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.edit_bookmark)

        myBookmark = FBReaderIntents.getBookmarkExtra(intent)
        if (myBookmark == null) {
            finish()
            return
        }

        val dm = resources.displayMetrics
        val width = Math.min(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500f, dm).toInt(),
            dm.widthPixels * 9 / 10
        )
        val height = Math.min(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 350f, dm).toInt(),
            dm.heightPixels * 9 / 10
        )

        val tabHost: TabHost = findViewById(R.id.edit_bookmark_tabhost)
        tabHost.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams(width, height)
        )
        tabHost.setup()

        addTab(tabHost, "text", R.id.edit_bookmark_content_text)
        addTab(tabHost, "style", R.id.edit_bookmark_content_style)
        addTab(tabHost, "delete", R.id.edit_bookmark_content_delete)

        val currentTabOption = ZLStringOption("LookNFeel", "EditBookmark", "text")
        tabHost.setCurrentTabByTag(currentTabOption.value)
        tabHost.setOnTabChangedListener { tag ->
            if (tag != "delete") {
                currentTabOption.value = tag
            }
        }

        val editor: EditText = findViewById(R.id.edit_bookmark_text)
        editor.setText(myBookmark!!.text)
        val len = editor.text.length
        editor.setSelection(len, len)

        val saveTextButton: Button = findViewById(R.id.edit_bookmark_save_text_button)
        saveTextButton.isEnabled = false
        saveTextButton.text = myResource.getResource("saveText").value
        saveTextButton.setOnClickListener {
            myCollection.bindToService(this@EditBookmarkActivity) {
                myBookmark!!.text = editor.text.toString()
                myCollection.saveBookmark(myBookmark!!)
                saveTextButton.isEnabled = false
            }
        }
        editor.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(sequence: CharSequence, start: Int, before: Int, count: Int) {
                val originalText = myBookmark!!.text
                saveTextButton.isEnabled = originalText != editor.text.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        val deleteButton: Button = findViewById(R.id.edit_bookmark_delete_button)
        deleteButton.text = myResource.getResource("deleteBookmark").value
        deleteButton.setOnClickListener {
            myCollection.bindToService(this@EditBookmarkActivity) {
                myCollection.deleteBookmark(myBookmark!!)
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        myCollection.bindToService(this) {
            val styles = myCollection.highlightingStyles()
            if (styles.isEmpty()) {
                finish()
                return@bindToService
            }
            myStylesAdapter = StyleListAdapter(styles)
            val stylesList: ListView = findViewById(R.id.edit_bookmark_content_style)
            stylesList.adapter = myStylesAdapter
            stylesList.onItemClickListener = myStylesAdapter
            myCollection.addListener(this@EditBookmarkActivity)
        }
    }

    override fun onDestroy() {
        myCollection.unbind()
        super.onDestroy()
    }

    override fun onBookEvent(event: BookEvent, book: Book?) {
        if (event == BookEvent.BookmarkStyleChanged) {
            myStylesAdapter?.setStyleList(myCollection.highlightingStyles())
        }
    }

    override fun onBuildEvent(status: IBookCollection.Status) {}

    private inner class StyleListAdapter(styles: List<HighlightingStyle>) : BaseAdapter(), AdapterView.OnItemClickListener {

        private val myStyles = ArrayList(styles)

        @Synchronized
        fun setStyleList(styles: List<HighlightingStyle>) {
            myStyles.clear()
            myStyles.addAll(styles)
            notifyDataSetChanged()
        }

        @Synchronized
        override fun getCount(): Int = myStyles.size

        @Synchronized
        override fun getItem(position: Int): HighlightingStyle = myStyles[position]

        override fun getItemId(position: Int): Long = position.toLong()

        @Synchronized
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(parent.context).inflate(R.layout.style_item, parent, false)
            val style = getItem(position)

            val checkBox: CheckBox = ViewUtil.findView(view, R.id.style_item_checkbox) as CheckBox
            val colorView: AmbilWarnaPrefWidgetView = ViewUtil.findView(view, R.id.style_item_color) as AmbilWarnaPrefWidgetView
            val titleView: TextView = ViewUtil.findTextView(view, R.id.style_item_title)
            val button: Button = ViewUtil.findView(view, R.id.style_item_edit_button) as Button

            checkBox.isChecked = style.id == myBookmark!!.styleId

            colorView.visibility = View.VISIBLE
            BookmarksUtil.setupColorView(colorView, style)

            titleView.text = BookmarkUtil.getStyleName(style)

            button.visibility = View.VISIBLE
            button.text = myResource.getResource("editStyle").value
            button.setOnClickListener {
                startActivity(
                    Intent(this@EditBookmarkActivity, EditStyleActivity::class.java)
                        .putExtra(EditStyleActivity.STYLE_ID_KEY, style.id)
                )
            }

            return view
        }

        @Synchronized
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val style = getItem(position)
            myCollection.bindToService(this@EditBookmarkActivity) {
                myBookmark!!.styleId = style.id
                myCollection.setDefaultHighlightingStyleId(style.id)
                myCollection.saveBookmark(myBookmark!!)
            }
            notifyDataSetChanged()
        }
    }
}
