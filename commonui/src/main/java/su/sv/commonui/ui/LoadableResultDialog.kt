package su.sv.commonui.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import su.sv.commonui.R

/**
 * Прогресс диалог, который сам управляет своей видимостью
 */
class LoadableResultDialog(
    context: Context,
    message: String? = null,
) {
    private val dialog: AlertDialog

    init {
        /**, R.style.ProgressDialogTheme**/
        val builder = AlertDialog.Builder(context)
        builder.setView(R.layout.dialog_progress)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null, false)
        val textViewTitle = view.findViewById<TextView>(R.id.textViewMessage)
        if (message != null) textViewTitle.text = message
        builder.setView(view)
        builder.setCancelable(false)
        dialog = builder.create()
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }
}
