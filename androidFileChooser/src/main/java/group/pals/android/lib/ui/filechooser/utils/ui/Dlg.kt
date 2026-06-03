/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils.ui

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.widget.Toast
import group.pals.android.R
import group.pals.android.lib.ui.filechooser.utils.ui.Dlg.LENGTH_LONG
import group.pals.android.lib.ui.filechooser.utils.ui.Dlg.LENGTH_SHORT

/**
 * Утилиты для диалоговых окон.
 *
 * @author Hai Bison
 * @since v2.1 alpha
 */
object Dlg {

    /** @see Toast.LENGTH_SHORT */
    const val LENGTH_SHORT = Toast.LENGTH_SHORT

    /** @see Toast.LENGTH_LONG */
    const val LENGTH_LONG = Toast.LENGTH_LONG

    private var toast: Toast? = null

    /**
     * Показывает всплывающее сообщение.
     *
     * @param context [Context]
     * @param msg сообщение.
     * @param duration может быть [LENGTH_LONG] или [LENGTH_SHORT].
     */
    fun toast(context: Context, msg: CharSequence, duration: Int) {
        toast?.cancel()
        toast = Toast.makeText(context, msg, duration)
        toast?.show()
    }

    /**
     * Показывает всплывающее сообщение.
     *
     * @param context [Context]
     * @param msgId ID ресурса сообщения.
     * @param duration может быть [LENGTH_LONG] или [LENGTH_SHORT].
     */
    fun toast(context: Context, msgId: Int, duration: Int) {
        toast(context, context.getString(msgId), duration)
    }

    /**
     * Показывает информационное сообщение.
     *
     * @param context [Context]
     * @param msg сообщение.
     */
    fun showInfo(context: Context, msg: CharSequence) {
        val dlg = newDlg(context)
        dlg.setIcon(android.R.drawable.ic_dialog_info)
        dlg.setTitle(R.string.afc_title_info)
        dlg.setMessage(msg)
        dlg.show()
    }

    /**
     * Показывает информационное сообщение.
     *
     * @param context [Context]
     * @param msgId ID ресурса сообщения.
     */
    fun showInfo(context: Context, msgId: Int) {
        showInfo(context, context.getString(msgId))
    }

    /**
     * Показывает сообщение об ошибке.
     *
     * @param context [Context]
     * @param msg сообщение.
     * @param listener будет вызван после закрытия диалога пользователем.
     */
    fun showError(context: Context, msg: CharSequence, listener: DialogInterface.OnCancelListener?) {
        val dlg = newDlg(context)
        dlg.setIcon(android.R.drawable.ic_dialog_alert)
        dlg.setTitle(R.string.afc_title_error)
        dlg.setMessage(msg)
        dlg.setOnCancelListener(listener)
        dlg.show()
    }

    /**
     * Показывает сообщение об ошибке.
     *
     * @param context [Context]
     * @param msgId ID ресурса сообщения.
     * @param listener будет вызван после закрытия диалога пользователем.
     */
    fun showError(context: Context, msgId: Int, listener: DialogInterface.OnCancelListener?) {
        showError(context, context.getString(msgId), listener)
    }

    /**
     * Показывает неизвестную ошибку.
     *
     * @param context [Context]
     * @param t [Throwable]
     * @param listener будет вызван после закрытия диалога пользователем.
     */
    fun showUnknownError(context: Context, t: Throwable, listener: DialogInterface.OnCancelListener?) {
        showError(context, context.getString(R.string.afc_pmsg_unknown_error, t), listener)
    }

    /**
     * Показывает диалог подтверждения.
     *
     * @param context [Context]
     * @param msg сообщение.
     * @param onYes будет вызван, если пользователь выберет положительный ответ.
     * @param onNo будет вызван после закрытия диалога пользователем.
     */
    fun confirmYesno(
        context: Context,
        msg: CharSequence,
        onYes: DialogInterface.OnClickListener?,
        onNo: DialogInterface.OnCancelListener?
    ) {
        val dlg = newDlg(context)
        dlg.setIcon(android.R.drawable.ic_dialog_alert)
        dlg.setTitle(R.string.afc_title_confirmation)
        dlg.setMessage(msg)
        dlg.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.yes), onYes)
        dlg.setOnCancelListener(onNo)
        dlg.show()
    }

    /**
     * Показывает диалог подтверждения.
     *
     * @param context [Context]
     * @param msg сообщение.
     * @param onYes будет вызван, если пользователь выберет положительный ответ.
     */
    fun confirmYesno(context: Context, msg: CharSequence, onYes: DialogInterface.OnClickListener?) {
        confirmYesno(context, msg, onYes, null)
    }

    /**
     * Создаёт новый [AlertDialog]. Устанавливает отмену при касании снаружи в `true`.
     *
     * @param context [Context]
     * @return [AlertDialog]
     * @since v4.3 beta
     */
    fun newDlg(context: Context): AlertDialog {
        val res = newDlgBuilder(context).create()
        res.setCanceledOnTouchOutside(true)
        return res
    }

    /**
     * Создаёт новый [AlertDialog.Builder].
     *
     * @param context [Context]
     * @return [AlertDialog.Builder]
     * @since v4.3 beta
     */
    fun newDlgBuilder(context: Context): AlertDialog.Builder {
        return AlertDialog.Builder(context)
    }
}
