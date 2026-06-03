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
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ListView
import group.pals.android.R

/**
 * Утилиты для контекстного меню.
 *
 * @author Hai Bison
 * @since v4.3 beta
 */
object ContextMenuUtils {

    /**
     * Показывает контекстное меню.
     *
     * @param context [Context]
     * @param iconId ID ресурса иконки диалога.
     * @param title заголовок диалога.
     * @param itemIds массив ID ресурсов строк.
     * @param listener [OnMenuItemClickListener]
     */
    fun showContextMenu(
        context: Context,
        iconId: Int,
        title: String?,
        itemIds: Array<Int>,
        listener: OnMenuItemClickListener?
    ) {
        val adapter = MenuItemAdapter(context, itemIds)

        val view = LayoutInflater.from(context).inflate(R.layout.afc_context_menu_view, null)
        val listView = view.findViewById<ListView>(R.id.afc_context_menu_view_listview_menu)
        listView.adapter = adapter

        val dialog = Dlg.newDlg(context)

        // Не используем кнопку Cancel
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, null, null as DialogInterface.OnClickListener?)
        dialog.setCanceledOnTouchOutside(true)

        if (iconId > 0) {
            dialog.setIcon(iconId)
        }
        dialog.setTitle(title)
        dialog.setView(view)

        if (listener != null) {
            listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                dialog.dismiss()
                listener.onClick(itemIds[position])
            }
        }

        dialog.show()
    }

    /**
     * Показывает контекстное меню.
     *
     * @param context [Context]
     * @param iconId ID ресурса иконки диалога.
     * @param titleId ID ресурса заголовка диалога. `0` будет проигнорировано.
     * @param itemIds массив ID ресурсов строк.
     * @param listener [OnMenuItemClickListener]
     */
    fun showContextMenu(
        context: Context,
        iconId: Int,
        titleId: Int,
        itemIds: Array<Int>,
        listener: OnMenuItemClickListener?
    ) {
        showContextMenu(context, iconId, if (titleId > 0) context.getString(titleId) else null, itemIds, listener)
    }

    /**
     * Слушатель клика по пункту меню.
     *
     * @author Hai Bison
     * @since v4.3 beta
     */
    interface OnMenuItemClickListener {
        /**
         * Этот метод будет вызван после закрытия меню.
         *
         * @param resId ID ресурса заголовка пункта меню.
         */
        fun onClick(resId: Int)
    }
}
