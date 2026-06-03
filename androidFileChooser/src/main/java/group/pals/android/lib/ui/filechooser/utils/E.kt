/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils

import android.app.AlertDialog
import android.content.Context
import group.pals.android.lib.ui.filechooser.utils.ui.Dlg

/**
 * Что-то забавное :-)
 *
 * @author Hai Bison
 */
object E {

    /**
     * Показывает!
     *
     * @param context [Context]
     */
    fun show(context: Context) {
        val msg = try {
            String.format(
                "Привет :-)\n\n%s v%s\n…от Hai Bison Apps\n\nhttp://www.haibison.com\n\nНадеемся, вам понравится эта библиотека.",
                "android-filechooser",
                "5.0"
            )
        } catch (e: Exception) {
            "Ой… Вы нашли сломанное пасхальное яйцо, попробуйте позже :-("
        }

        val dlg = Dlg.newDlg(context)
        dlg.setButton(AlertDialog.BUTTON_NEGATIVE, null as CharSequence?, null as android.content.DialogInterface.OnClickListener?)
        dlg.setTitle("…")
        dlg.setMessage(msg)
        dlg.show()
    }
}
