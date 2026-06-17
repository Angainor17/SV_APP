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
class EditAuthorsDialogActivity : EditListDialogActivity() {
    private val authorNameFilter = "[\\p{L}0-9_\\-& ]*"
    private var myInputField: AutoCompleteTextView? = null
    private var myEditPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_authors_dialog)

        myResource = ZLResource.resource("dialog").getResource("editAuthors")

        val intent = intent
        val allAuthorList =
            intent.getStringArrayListExtra(EditListDialogActivity.Key.ALL_ITEMS_LIST)

        myInputField = findViewById(R.id.edit_authors_input_field)
        myInputField?.let { inputField ->
            inputField.hint = myResource?.getResource("addAuthor")?.value
            inputField.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addAuthor(inputField.text.toString().trim(), myEditPosition)
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
                    allAuthorList ?: ArrayList()
                )
            )
        }

        parseUIElements()

        val adapter = AuthorsAdapter()
        listAdapter = adapter
        listView.onItemClickListener = adapter
        listView.onItemLongClickListener = adapter

        setResult(RESULT_CANCELED)
    }

    private fun addAuthor(author: String, position: Int) {
        if (author.isNotEmpty() && author.matches(Regex(authorNameFilter))) {
            if (position < 0) {
                if (!myEditList.contains(author)) {
                    myEditList.add(author)
                }
            } else {
                myEditList[position] = author
            }
            (listAdapter as BaseAdapter).notifyDataSetChanged()
        }
    }

    override fun onChooseContextMenu(index: Int, itemPosition: Int) {
        when (index) {
            0 -> editAuthor(itemPosition)
            1 -> deleteItem(itemPosition)
        }
    }

    private fun editAuthor(position: Int) {
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

    private inner class AuthorsAdapter : EditListAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)

            val deleteButton = view.findViewById<View>(R.id.edit_item_remove)

            if (myEditList.size > 1) {
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener {
                    deleteItem(position)
                }
            } else {
                deleteButton.visibility = View.INVISIBLE
            }

            return view
        }
    }

    companion object {

        const val REQ_CODE: Int = 2
    }
}
