package com.github.axet.bookreader.widgets

import android.content.Context
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.net.Uri
import android.util.AttributeSet
import android.widget.Toast
import com.github.axet.androidlibrary.preferences.StoragePathPreferenceCompat
import com.github.axet.androidlibrary.widgets.OpenFileDialog
import com.github.axet.androidlibrary.widgets.OpenStorageChoicer
import com.github.axet.bookreader.app.Storage

/**
 * Preference для выбора пути хранения книг.
 */
class StoragePathPreferenceCompat @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : StoragePathPreferenceCompat(context, attrs, defStyleAttr) {

    private var defSummary: CharSequence? = null

    override fun create() {
        defSummary = summary
        choicer = object : OpenStorageChoicer(storage, OpenFileDialog.DIALOG_TYPE.FOLDER_DIALOG, false) {
            var reset: Uri? = null

            override fun onResult(uri: Uri) {
                if (uri == reset) {
                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
                    editor.remove(key)
                    editor.apply()
                    setSummary(defSummary)
                } else {
                    if (callChangeListener(uri.toString())) {
                        setText(uri.toString())
                    }
                }
            }

            override fun fileDialogBuild(): OpenFileDialog {
                val d = super.fileDialogBuild()

                d.setNeutralButton(com.github.axet.androidlibrary.R.string.default_button) { _, _ ->
                    val path = storage.localStorage
                    d.setCurrentPath(path)
                    reset = Uri.fromFile(path)
                    Toast.makeText(context, path.toString(), Toast.LENGTH_SHORT).show()
                }

                return d
            }
        }
        choicer.setTitle(title.toString())
        choicer.setContext(context)
    }

    override fun onClick() {
        super.onClick()
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) { // позволяем показывать null
        val v = if (restoreValue) getPersistedString(text) else defaultValue as String?
        val u = storage.getStoragePath(v)
        if (u != null) {
            setText(u.toString())
            setSummary(Storage.getDisplayName(context, u))
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        super.onGetDefaultValue(a, index)
        return null // нет значения по умолчанию для читалки книг
    }
}
