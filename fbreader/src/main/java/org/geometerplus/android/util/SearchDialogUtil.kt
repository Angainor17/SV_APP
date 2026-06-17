package org.geometerplus.android.util

import android.app.Activity
import android.app.AlertDialog
import android.app.SearchManager
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import org.geometerplus.zlibrary.core.resources.ZLResource

object SearchDialogUtil {
    @JvmStatic
    @JvmOverloads
    @Suppress("DEPRECATION")
    fun showDialog(
        activity: Activity,
        clazz: Class<out Activity>,
        initialPattern: String,
        listener: DialogInterface.OnCancelListener?,
        bundle: Bundle? = null
    ) {
        val builder = AlertDialog.Builder(activity)

        builder.setTitle(ZLResource.resource("menu").getResource("search").value)

        val input = EditText(activity)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(initialPattern)
        builder.setView(input)

        val dialogResource = ZLResource.resource("dialog").getResource("button")
        builder.setPositiveButton(dialogResource.getResource("ok").value) { _, _ ->
            activity.startActivity(
                Intent(Intent.ACTION_SEARCH)
                    .setClass(activity, clazz)
                    .putExtra(SearchManager.QUERY, input.text.toString())
                    .putExtra(SearchManager.APP_DATA, bundle)
            )
        }
        builder.setNegativeButton(dialogResource.getResource("cancel").value) { dialog, _ ->
            dialog.cancel()
        }
        if (listener != null) {
            builder.setOnCancelListener(listener)
        }
        val dialog = builder.create()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }
}
