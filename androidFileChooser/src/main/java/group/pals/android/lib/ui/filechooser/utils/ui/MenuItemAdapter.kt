/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import group.pals.android.R

/**
 * Адаптер для контекстного меню.
 *
 * @author Hai Bison
 * @since v4.3 beta
 */
class MenuItemAdapter(
    private val context: Context,
    private val items: Array<Int>
) : BaseAdapter() {

    private val padding: Int = context.resources.getDimensionPixelSize(R.dimen.afc_5dp)
    private val itemPaddingLeft: Int = context.resources.getDimensionPixelSize(R.dimen.afc_context_menu_item_padding_left)

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.afc_context_menu_tiem, null)

        (view as TextView).setText(items[position])
        view.setPadding(itemPaddingLeft, padding, padding, padding)

        return view
    }
}
