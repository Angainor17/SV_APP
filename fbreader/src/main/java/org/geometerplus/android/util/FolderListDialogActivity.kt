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
class FolderListDialogActivity : ListActivity() {
    private lateinit var myFolderList: ArrayList<String>
    private var myChooserTitle: String? = null
    private var myChooseWritableDirectoriesOnly = false
    private lateinit var myResource: ZLResource

    interface Key {
        companion object {
            const val FOLDER_LIST = "folder_list.folder_list"
            const val ACTIVITY_TITLE = "folder_list.title"
            const val CHOOSER_TITLE = "folder_list.chooser_title"
            const val WRITABLE_FOLDERS_ONLY = "folder_list.writable_folders_only"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.folder_list_dialog)

        val intent = intent
        @Suppress("UNCHECKED_CAST")
        myFolderList = intent.getStringArrayListExtra(Key.FOLDER_LIST) as ArrayList<String>
        title = intent.getStringExtra(Key.ACTIVITY_TITLE)
        myChooserTitle = intent.getStringExtra(Key.CHOOSER_TITLE)
        myChooseWritableDirectoriesOnly = intent.getBooleanExtra(Key.WRITABLE_FOLDERS_ONLY, true)
        myResource = ZLResource.resource("dialog").getResource("folderList")

        val buttonResource = ZLResource.resource("dialog").getResource("button")
        val okButton = findViewById<Button>(R.id.folder_list_dialog_button_ok)
        okButton.text = buttonResource.getResource("ok").value
        okButton.setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra(Key.FOLDER_LIST, myFolderList))
            finish()
        }
        val cancelButton = findViewById<Button>(R.id.folder_list_dialog_button_cancel)
        cancelButton.text = buttonResource.getResource("cancel").value
        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        val adapter = DirectoriesAdapter()
        listAdapter = adapter
        listView.onItemClickListener = adapter

        setResult(RESULT_CANCELED)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(index: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val path = FileChooserUtil.folderPathFromData(data) ?: return
            val existing = myFolderList.indexOf(path)
            if (existing == -1) {
                if (index == 0) {
                    myFolderList.add(path)
                } else {
                    myFolderList[index - 1] = path
                }
                (listAdapter as DirectoriesAdapter).notifyDataSetChanged()
            } else if (existing != index - 1) {
                UIMessageUtil.showMessageText(
                    this, myResource.getResource("duplicate").value.replace("%s", path)
                )
            }
        }
    }

    private fun showItemRemoveDialog(index: Int) {
        val resource = myResource.getResource("removeDialog")
        val buttonResource = ZLResource.resource("dialog").getResource("button")
        AlertDialog.Builder(this@FolderListDialogActivity)
            .setCancelable(false)
            .setTitle(resource.value)
            .setMessage(resource.getResource("message").value.replace("%s", myFolderList[index]))
            .setPositiveButton(buttonResource.getResource("yes").value) { _, _ ->
                myFolderList.removeAt(index)
                (listAdapter as DirectoriesAdapter).notifyDataSetChanged()
            }
            .setNegativeButton(buttonResource.getResource("cancel").value, null)
            .create().show()
    }

    private inner class DirectoriesAdapter : BaseAdapter(), AdapterView.OnItemClickListener {
        override fun getCount(): Int = myFolderList.size + 1

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getItem(position: Int): String {
            return if (position != 0)
                myFolderList[position - 1]
            else
                myResource.getResource("addFolder").value
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(this@FolderListDialogActivity).inflate(R.layout.folder_list_item, parent, false)

            (view.findViewById<View>(R.id.folder_list_item_title) as TextView).text = getItem(position)

            val deleteButton = view.findViewById<View>(R.id.folder_list_item_remove)

            if (position > 0 && myFolderList.size > 1) {
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener {
                    showItemRemoveDialog(position - 1)
                }
            } else {
                deleteButton.visibility = View.INVISIBLE
            }

            return view
        }

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            FileChooserUtil.runDirectoryChooser(
                this@FolderListDialogActivity,
                position,
                myChooserTitle ?: "",
                if (position == 0) "/" else myFolderList[position - 1],
                myChooseWritableDirectoriesOnly
            )
        }
    }
}
