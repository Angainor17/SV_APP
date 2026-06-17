package org.geometerplus.android.util

import android.app.AlertDialog
import android.app.ListActivity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import org.geometerplus.R
import org.geometerplus.zlibrary.core.resources.ZLResource

@Suppress("DEPRECATION")
abstract class EditListDialogActivity : ListActivity() {
    @Suppress("UNCHECKED_CAST")
    protected var myEditList: ArrayList<String> = intent?.getStringArrayListExtra(Key.LIST) as ArrayList<String>
    protected var myResource: ZLResource? = null
    private val myContextMenuItems = ArrayList<String>()

    interface Key {
        companion object {
            const val LIST = "edit_list.list"
            const val ALL_ITEMS_LIST = "edit_list.all_items_list"
            const val ACTIVITY_TITLE = "edit_list.title"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        @Suppress("UNCHECKED_CAST")
        myEditList = intent.getStringArrayListExtra(Key.LIST) as ArrayList<String>
        title = intent.getStringExtra(Key.ACTIVITY_TITLE)
        setResult(RESULT_CANCELED)

        val editListResource = ZLResource.resource("dialog").getResource("editList")
        myContextMenuItems.add(editListResource.getResource("edit").value)
        myContextMenuItems.add(editListResource.getResource("remove").value)
    }

    protected fun parseUIElements() {
        val buttonResource = ZLResource.resource("dialog").getResource("button")
        val okButton = findViewById<Button>(R.id.edit_dialog_button_ok)
        okButton?.let {
            it.text = buttonResource.getResource("ok").value
            it.setOnClickListener {
                setResult(RESULT_OK, Intent().putExtra(Key.LIST, myEditList))
                finish()
            }
        }
        val cancelButton = findViewById<Button>(R.id.edit_dialog_button_cancel)
        cancelButton?.let {
            it.text = buttonResource.getResource("cancel").value
            it.setOnClickListener {
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    protected fun showItemRemoveDialog(index: Int) {
        if (index < 0 || myResource == null)
            return

        val resource = myResource!!.getResource("removeDialog")
        val buttonResource = ZLResource.resource("dialog").getResource("button")
        AlertDialog.Builder(this@EditListDialogActivity)
            .setCancelable(false)
            .setTitle(resource.value)
            .setMessage(resource.getResource("message").value.replace("%s", myEditList[index]))
            .setPositiveButton(buttonResource.getResource("yes").value) { _, _ ->
                myEditList.removeAt(index)
                listAdapter?.let { (it as BaseAdapter).notifyDataSetChanged() }
            }
            .setNegativeButton(buttonResource.getResource("cancel").value, null)
            .create().show()
    }

    protected fun showItemContextMenuDialog(position: Int) {
        AlertDialog.Builder(this@EditListDialogActivity)
            .setTitle(myEditList[position])
            .setItems(myContextMenuItems.toTypedArray()) { _, which ->
                onChooseContextMenu(which, position)
            }.create().show()
    }

    protected abstract fun onChooseContextMenu(index: Int, itemPosition: Int)

    protected open fun onClick(position: Int) {
        showItemContextMenuDialog(position)
    }

    protected open fun onLongClick(position: Int) {
        //can be overriden in children
    }

    protected fun deleteItem(position: Int) {
        showItemRemoveDialog(position)
    }

    protected open inner class EditListAdapter : BaseAdapter(), AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
        override fun getCount(): Int = myEditList.size

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getItem(position: Int): String = myEditList[position]

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(this@EditListDialogActivity).inflate(R.layout.edit_list_dialog_item, parent, false)

            (view.findViewById<View>(R.id.edit_item_title) as TextView).text = getItem(position)
            return view
        }

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            onClick(position)
        }

        override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
            this@EditListDialogActivity.onLongClick(position)
            return true
        }
    }
}
