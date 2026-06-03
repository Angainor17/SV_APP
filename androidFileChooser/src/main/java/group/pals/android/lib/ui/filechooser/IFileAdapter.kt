/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import group.pals.android.R
import group.pals.android.lib.ui.filechooser.io.IFile
import group.pals.android.lib.ui.filechooser.io.IFileFilter
import group.pals.android.lib.ui.filechooser.prefs.DisplayPrefs
import group.pals.android.lib.ui.filechooser.prefs.DisplayPrefs.FileTimeDisplay
import group.pals.android.lib.ui.filechooser.services.IFileProvider
import group.pals.android.lib.ui.filechooser.utils.Converter
import group.pals.android.lib.ui.filechooser.utils.DateUtils
import group.pals.android.lib.ui.filechooser.utils.FileUtils
import group.pals.android.lib.ui.filechooser.utils.ui.ContextMenuUtils
import group.pals.android.lib.ui.filechooser.utils.ui.LoadingDialog

/**
 * Адаптер для использования в [android.widget.ListView].
 *
 * @author Hai Bison
 */
class IFileAdapter(
    private val context: Context,
    private var data: MutableList<IFileDataModel>?,
    private val filterMode: IFileProvider.FilterMode,
    private val filenameRegexp: String?,
    private var multiSelection: Boolean
) : BaseAdapter() {

    companion object {
        /** Используется для логирования... */
        const val CLASS_NAME = "IFileAdapter"
    }

    private val advancedSelectionOptions: Array<Int>
    private val fileTimeDisplay: FileTimeDisplay
    private val inflater = LayoutInflater.from(context)

    private val checkboxSelectionOnLongClickListener = View.OnLongClickListener { view ->
        ContextMenuUtils.showContextMenu(
            view.context,
            0,
            R.string.afc_title_advanced_selection,
            advancedSelectionOptions,
            object : ContextMenuUtils.OnMenuItemClickListener {
                override fun onClick(resId: Int) {
                    object : LoadingDialog(view.context, R.string.afc_msg_loading, false) {
                        override fun doInBackground(vararg params: Void): Any? {
                            when (resId) {
                                R.string.afc_cmd_advanced_selection_all -> selectAll(false, null)
                                R.string.afc_cmd_advanced_selection_none -> selectNone(false)
                                R.string.afc_cmd_advanced_selection_invert -> invertSelection(false)
                                R.string.afc_cmd_select_all_files -> selectAll(false, object : IFileFilter {
                                    override fun accept(pathname: IFile): Boolean = pathname.isFile()
                                })
                                R.string.afc_cmd_select_all_folders -> selectAll(false, object : IFileFilter {
                                    override fun accept(pathname: IFile): Boolean = pathname.isDirectory()
                                })
                            }
                            return null
                        }

                        override fun onPostExecute(result: Any?) {
                            super.onPostExecute(result)
                            notifyDataSetChanged()
                        }
                    }.execute()
                }
            })
        true
    }

    init {
        advancedSelectionOptions = when (filterMode) {
            IFileProvider.FilterMode.DirectoriesOnly,
            IFileProvider.FilterMode.FilesOnly -> arrayOf(
                R.string.afc_cmd_advanced_selection_all,
                R.string.afc_cmd_advanced_selection_none,
                R.string.afc_cmd_advanced_selection_invert
            )
            else -> arrayOf(
                R.string.afc_cmd_advanced_selection_all,
                R.string.afc_cmd_advanced_selection_none,
                R.string.afc_cmd_advanced_selection_invert,
                R.string.afc_cmd_select_all_files,
                R.string.afc_cmd_select_all_folders
            )
        }

        fileTimeDisplay = FileTimeDisplay(
            DisplayPrefs.isShowTimeForOldDaysThisYear(context),
            DisplayPrefs.isShowTimeForOldDays(context)
        )
    }

    override fun notifyDataSetChanged() {
        updateEnvironments()
        super.notifyDataSetChanged()
    }

    /**
     * Обновляет окружение, например, отображение времени файла.
     * Этот метод полезен, если у вас есть PreferenceActivity или PreferenceFragment
     * для изменения настроек пользователем.
     */
    fun updateEnvironments() {
        fileTimeDisplay.isShowTimeForOldDaysThisYear = DisplayPrefs.isShowTimeForOldDaysThisYear(context)
        fileTimeDisplay.isShowTimeForOldDays = DisplayPrefs.isShowTimeForOldDays(context)
    }

    override fun getCount(): Int = data?.size ?: 0

    override fun getItem(position: Int): IFileDataModel = data?.get(position) ?: throw IndexOutOfBoundsException()

    override fun getItemId(position: Int): Long = position.toLong()

    fun isMultiSelection(): Boolean = multiSelection

    /**
     * Устанавливает режим множественного выбора.
     *
     * **Примечание:**
     * - Если `v = true`, этот метод также обновит адаптер.
     * - Если `v = false`, этот метод переберёт все элементы и установит их выбор в `false`.
     *   Поэтому вам следует рассмотреть использование [LoadingDialog]. Это не обновит адаптер, вы должны сделать это сами.
     *
     * @param v `true`, если множественный выбор включён.
     */
    fun setMultiSelection(v: Boolean) {
        if (multiSelection != v) {
            multiSelection = v
            if (multiSelection) {
                notifyDataSetChanged()
            } else {
                if (count > 0) {
                    for (i in data?.indices ?: emptyList()) {
                        data?.get(i)?.isSelected = false
                    }
                }
            }
        }
    }

    /**
     * Получает выбранные элементы.
     *
     * @return список выбранных элементов, может быть пустым, но никогда `null`.
     */
    fun getSelectedItems(): ArrayList<IFileDataModel> {
        val result = ArrayList<IFileDataModel>()
        for (i in 0 until count) {
            if (getItem(i).isSelected) {
                result.add(getItem(i))
            }
        }
        return result
    }

    /**
     * Добавляет элемент. **Примечание:** Это не уведомляет адаптер об изменении данных.
     *
     * @param item [IFileDataModel]
     */
    fun add(item: IFileDataModel) {
        data?.add(item)
    }

    /**
     * Удаляет элемент. **Примечание:** Это не уведомляет адаптер об изменении данных.
     *
     * @param item [IFileDataModel]
     */
    fun remove(item: IFileDataModel) {
        data?.remove(item)
    }

    /**
     * Удаляет все элементы. **Примечание:** Это не уведомляет адаптер об изменении данных.
     *
     * @param items элементы, которые вы хотите удалить.
     */
    fun removeAll(items: Collection<IFileDataModel>) {
        data?.removeAll(items)
    }

    /**
     * Очищает все элементы.
     * **Примечание:** Это не уведомляет адаптер об изменении данных.
     */
    fun clear() {
        data?.clear()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val data = getItem(position)
        var view = convertView
        val bag: Bag

        if (view == null) {
            view = inflater.inflate(R.layout.afc_file_item, null)

            bag = Bag()
            bag.imageIcon = view.findViewById(R.id.afc_file_item_imageview_icon)
            bag.txtFileName = view.findViewById(R.id.afc_file_item_textview_filename)
            bag.txtFileInfo = view.findViewById(R.id.afc_file_item_textview_file_info)
            bag.checkboxSelection = view.findViewById(R.id.afc_file_item_checkbox_selection)

            view.tag = bag
        } else {
            bag = view.tag as Bag
        }

        // Обновляем представление
        updateView(parent, view, bag, data, data.file)

        return view!!
    }

    /**
     * Обновляет представление.
     *
     * @param parent родительское представление.
     * @param childView дочернее представление.
     * @param bag "view holder", см. [Bag].
     * @param data [IFileDataModel]
     * @param file [IFile]
     * @since v2.0 alpha
     */
    private fun updateView(
        parent: ViewGroup?,
        childView: View,
        bag: Bag,
        data: IFileDataModel,
        file: IFile
    ) {
        // Используем одну строку для GridView, несколько строк для ListView
        bag.txtFileName.isSingleLine = parent is GridView

        // Иконка файла
        bag.imageIcon.setImageResource(FileUtils.getResIcon(file, filterMode))

        // Имя файла
        bag.txtFileName.text = file.getSecondName()
        // Проверяем, помечен ли файл для удаления
        if (data.isTobeDeleted) {
            bag.txtFileName.paintFlags = bag.txtFileName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            bag.txtFileName.paintFlags = bag.txtFileName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        // Информация о файле
        val time = DateUtils.formatDate(context, file.lastModified(), fileTimeDisplay)
        if (file.isDirectory()) {
            bag.txtFileInfo.text = time
        } else {
            bag.txtFileInfo.text = "${Converter.sizeToStr(file.length().toDouble())}, $time"
        }

        bag.isAccessible = FileUtils.isAccessible(file, filenameRegexp)
        bag.imageIcon.isEnabled = bag.isAccessible
        bag.txtFileName.isEnabled = bag.isAccessible
        bag.txtFileInfo.isEnabled = bag.isAccessible

        // Чекбокс
        if (multiSelection) {
            if (IFileProvider.FilterMode.FilesOnly == filterMode && file.isDirectory()) {
                bag.checkboxSelection.visibility = View.GONE
            } else {
                bag.checkboxSelection.visibility = View.VISIBLE
                bag.checkboxSelection.isFocusable = false
                bag.checkboxSelection.setOnCheckedChangeListener { _, isChecked ->
                    data.isSelected = isChecked
                }

                bag.checkboxSelection.setOnLongClickListener(checkboxSelectionOnLongClickListener)
                bag.checkboxSelection.isChecked = data.isSelected
            }
        } else {
            bag.checkboxSelection.visibility = View.GONE
        }
    }

    // =========
    // УТИЛИТЫ

    /**
     * Выбирает все элементы.
     *
     * @param notifyDataSetChanged `true`, если нужно уведомить об изменении данных.
     * @param filter [IFileFilter]
     */
    fun selectAll(notifyDataSetChanged: Boolean, filter: IFileFilter?) {
        for (i in 0 until count) {
            val item = getItem(i)
            item.isSelected = filter?.accept(item.file) ?: true
        }
        if (notifyDataSetChanged) {
            this.notifyDataSetChanged()
        }
    }

    /**
     * Снимает выбор со всех элементов.
     *
     * @param notifyDataSetChanged `true`, если нужно уведомить об изменении данных.
     */
    fun selectNone(notifyDataSetChanged: Boolean) {
        for (i in 0 until count) {
            getItem(i).isSelected = false
        }
        if (notifyDataSetChanged) {
            notifyDataSetChanged()
        }
    }

    /**
     * Инвертирует выбор.
     *
     * @param notifyDataSetChanged `true`, если нужно уведомить об изменении данных.
     */
    fun invertSelection(notifyDataSetChanged: Boolean) {
        for (i in 0 until count) {
            val item = getItem(i)
            item.isSelected = !item.isSelected
        }
        if (notifyDataSetChanged) {
            notifyDataSetChanged()
        }
    }

    // =========
    // ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ

    /**
     * "View holder"
     *
     * @author Hai Bison
     */
    internal class Bag {
        var isAccessible = false
        lateinit var imageIcon: ImageView
        lateinit var txtFileName: TextView
        lateinit var txtFileInfo: TextView
        lateinit var checkboxSelection: CheckBox
    }
}
