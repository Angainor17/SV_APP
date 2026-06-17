package org.geometerplus.android.util

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.BaseAdapter
import org.geometerplus.R
import org.geometerplus.zlibrary.core.resources.ZLResource

@Suppress("DEPRECATION")
class EditTagsDialogActivity : EditListDialogActivity() {
    private val tagNameFilter = "[\\p{L}0-9_\\-& ]*"
    private var myInputField: AutoCompleteTextView? = null
    private var myEditPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_tags_dialog)

        myResource = ZLResource.resource("dialog").getResource("editTags")

        val intent = intent
        val allTagsList = intent.getStringArrayListExtra(Key.ALL_ITEMS_LIST)

        myInputField = findViewById(R.id.edit_tags_input_field)
        myInputField?.let { inputField ->
            inputField.hint = myResource?.getResource("addTag")?.value
            inputField.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addTag(inputField.text.toString(), myEditPosition)
                    inputField.setText("")
                    myEditPosition = -1
                    return@setOnEditorActionListener false
                }
                true
            }
            inputField.setAdapter(
                ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    allTagsList ?: ArrayList()
                )
            )
        }

        parseUIElements()

        val adapter = TagsAdapter()
        listAdapter = adapter
        listView.onItemClickListener = adapter
        listView.onItemLongClickListener = adapter

        setResult(RESULT_CANCELED)
    }

    private fun addTag(tag: String, position: Int) {
        if (tag.isNotEmpty()) {
            val tags = tag.split(",")
            if (position < 0) {
                for (s in tags) {
                    val trimmed = s.trim()
                    if (!myEditList.contains(trimmed) && trimmed.matches(Regex(tagNameFilter))) {
                        myEditList.add(trimmed)
                    }
                }
            } else {
                val trimmed = tags[0].trim()
                if (trimmed.matches(Regex(tagNameFilter))) {
                    myEditList[position] = trimmed
                }
            }
            (listAdapter as BaseAdapter).notifyDataSetChanged()
        }
    }

    override fun onChooseContextMenu(index: Int, itemPosition: Int) {
        when (index) {
            0 -> editTag(itemPosition)
            1 -> deleteItem(itemPosition)
        }
    }

    private fun editTag(position: Int) {
        myEditPosition = position
        val s = listAdapter?.getItem(position) as String
        myInputField?.let { inputField ->
            inputField.setText(s)
            inputField.setSelection(inputField.text.length)
            inputField.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private inner class TagsAdapter : EditListAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)

            val deleteButton = view.findViewById<View>(R.id.edit_item_remove)
            deleteButton.setOnClickListener {
                deleteItem(position)
            }

            return view
        }
    }

    companion object {

        const val REQ_CODE = 1
    }
}
