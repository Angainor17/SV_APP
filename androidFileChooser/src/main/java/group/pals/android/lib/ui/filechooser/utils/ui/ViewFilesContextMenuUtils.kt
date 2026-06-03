/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils.ui

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ListView
import group.pals.android.R
import group.pals.android.lib.ui.filechooser.IFileAdapter
import group.pals.android.lib.ui.filechooser.IFileDataModel
import group.pals.android.lib.ui.filechooser.io.IFile
import group.pals.android.lib.ui.filechooser.services.IFileProvider
import group.pals.android.lib.ui.filechooser.services.IFileProvider.FilterMode
import group.pals.android.lib.ui.filechooser.utils.history.History

/**
 * Утилиты для контекстного меню редактора.
 *
 * @author Hai Bison
 * @since v4.3 beta
 */
object ViewFilesContextMenuUtils {

    /**
     * Показывает содержимое истории пользователю. Он может очистить все элементы.
     *
     * @param context [Context]
     * @param fileProvider [IFileProvider]
     * @param history [History] из [IFile].
     * @param currentLocation текущее местоположение, не будет показано.
     * @param listener будет уведомлён после закрытия диалога или когда пользователь выберет элемент.
     */
    fun doShowHistoryContents(
        context: Context,
        fileProvider: IFileProvider,
        history: History<IFile>,
        currentLocation: IFile?,
        listener: TaskListener?
    ) {
        if (history.isEmpty()) return

        val dialog = Dlg.newDlg(context)
        // Не используем кнопку Cancel
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, null, null as DialogInterface.OnClickListener?)
        dialog.setIcon(android.R.drawable.ic_dialog_info)
        dialog.setTitle(R.string.afc_title_history)

        val data = mutableListOf<IFileDataModel>()
        val items = history.items()
        for (i in items.size - 1 downTo 0) {
            val f = items[i]
            if (f == currentLocation) continue

            // Проверяем дубликаты
            var duplicated = false
            for (j in data.indices) {
                if (f.equalsToPath(data[j].file)) {
                    duplicated = true
                    break
                }
            }
            if (!duplicated) {
                data.add(IFileDataModel(f))
            }
        }

        val adapter = IFileAdapter(context, data, FilterMode.DirectoriesOnly, null, false)

        val listView = LayoutInflater.from(context).inflate(R.layout.afc_listview_files, null) as ListView
        listView.setBackgroundResource(0)
        listView.isFastScrollEnabled = true
        listView.adapter = adapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (listener != null) {
                dialog.dismiss()
                listener.onFinish(true, adapter.getItem(position).file)
            }
        }

        dialog.setView(listView)
        dialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            context.getString(R.string.afc_cmd_clear)
        ) { dialog, _ ->
            dialog.cancel()
            history.clear()
        }
        dialog.setOnCancelListener {
            listener?.onFinish(true, null)
        }
        dialog.show()
    }
}
