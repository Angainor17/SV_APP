package org.geometerplus.android.fbreader.crash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.geometerplus.R
import org.geometerplus.android.fbreader.FBReader
import org.geometerplus.android.util.FileChooserUtil
import org.geometerplus.fbreader.Paths
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.resources.ZLResource

class FixBooksDirectoryActivity : Activity() {

    private lateinit var myDirectoryView: TextView

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.books_directory_fix)

        val resource = ZLResource.resource("crash").getResource("fixBooksDirectory")
        val buttonResource = ZLResource.resource("dialog").getResource("button")

        val title = resource.getResource("title").value

        val textView: TextView = findViewById(R.id.books_directory_fix_text)
        textView.text = resource.getResource("text").value

        myDirectoryView = findViewById(R.id.books_directory_fix_directory)

        val buttonsView: View = findViewById(R.id.books_directory_fix_buttons)
        val okButton: Button = buttonsView.findViewById(R.id.ok_button)
        okButton.text = buttonResource.getResource("ok").value

        val selectButton: View = findViewById(R.id.books_directory_fix_select_button)

        Config.Instance()?.runOnConnect {
            val tempDirectoryOption = Paths.tempDirectoryOption(this@FixBooksDirectoryActivity)
            myDirectoryView.text = tempDirectoryOption.value
            selectButton.setOnClickListener {
                FileChooserUtil.runDirectoryChooser(
                    this@FixBooksDirectoryActivity,
                    1,
                    title,
                    tempDirectoryOption.value,
                    true
                )
            }
            okButton.setOnClickListener {
                val newDirectory = myDirectoryView.text.toString()
                tempDirectoryOption.value = newDirectory
                startActivity(Intent(this@FixBooksDirectoryActivity, FBReader::class.java))
                finish()
            }
        }

        val cancelButton: Button = buttonsView.findViewById(R.id.cancel_button)
        cancelButton.text = buttonResource.getResource("cancel").value
        cancelButton.setOnClickListener {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            myDirectoryView.text = FileChooserUtil.folderPathFromData(data!!)
        }
    }
}
