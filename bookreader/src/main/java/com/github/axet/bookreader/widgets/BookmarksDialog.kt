package com.github.axet.bookreader.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.github.axet.androidlibrary.net.HttpClient
import com.github.axet.androidlibrary.preferences.OptimizationPreferenceCompat
import com.github.axet.androidlibrary.widgets.TextMax
import com.github.axet.androidlibrary.widgets.TreeListView
import com.github.axet.androidlibrary.widgets.TreeRecyclerView
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.Storage

/**
 * Диалог для отображения списка закладок.
 */
open class BookmarksDialog(context: Context) : AlertDialog.Builder(context) {

    private var a: BMAdapter? = null
    private var tree: TreeRecyclerView? = null
    private lateinit var dialog: AlertDialog

    /**
     * Загружает список книг с закладками.
     */
    fun load(all: ArrayList<Storage.Book>) {
        a = BMAdapterBooks(all)
        tree = TreeRecyclerView(context)
        tree!!.adapter = a
        setView(tree)
        setPositiveButton(android.R.string.ok) { _, _ -> }
    }

    /**
     * Загружает список закладок.
     */
    fun load(bm: Storage.Bookmarks) {
        a = BMAdapter(bm)
        tree = TreeRecyclerView(context)
        tree!!.adapter = a
        setView(tree)
        setPositiveButton(android.R.string.ok) { _, _ -> }
    }

    override fun create(): AlertDialog {
        dialog = super.create()
        dialog.setOnShowListener { }
        return dialog
    }

    override fun show(): AlertDialog = super.show()

    open fun onSelected(b: Storage.Bookmark) {}
    open fun onSelected(book: Storage.Book, bm: Storage.Bookmark) {}
    open fun onSave(book: Storage.Book, bm: Storage.Bookmark) {}
    open fun onSave(bm: Storage.Bookmark) {}
    open fun onDelete(book: Storage.Book, bm: Storage.Bookmark) {}
    open fun onDelete(bm: Storage.Bookmark) {}

    /**
     * ViewHolder для элемента закладки.
     */
    class BMHolder(itemView: View) : TreeRecyclerView.TreeHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image)
        val text: TextView = itemView.findViewById(R.id.text)
        val name: TextView = itemView.findViewById(R.id.name)
    }

    /**
     * Адаптер для списка закладок.
     */
    open inner class BMAdapter : TreeRecyclerView.TreeAdapter<BMHolder> {

        constructor()

        constructor(tree: List<Storage.Bookmark>) {
            load(root, tree)
            load()
        }

        protected fun load(r: TreeListView.TreeNode, tree: List<Storage.Bookmark>) {
            for (t in tree) {
                val n = TreeListView.TreeNode(r, t)
                r.nodes.add(n)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BMHolder {
            val inflater = LayoutInflater.from(context)
            val convertView = inflater.inflate(R.layout.bm_item, parent, false)
            return BMHolder(convertView)
        }

        override fun onBindViewHolder(h: BMHolder, position: Int) {
            val t = getItem(h.getAdapterPosition(this))
            val tt = t.tag as Storage.Bookmark
            val ex = h.itemView.findViewById<ImageView>(R.id.expand)
            if (t.nodes.isEmpty())
                ex.visibility = View.INVISIBLE
            else
                ex.visibility = View.VISIBLE
            ex.setImageResource(if (t.expanded) R.drawable.ic_expand_less_black_24dp else R.drawable.ic_expand_more_black_24dp)
            h.itemView.setPadding(20 * t.level, 0, 0, 0)
            if (t.selected) {
                h.text.typeface = null
                h.text.setTypeface(null, Typeface.BOLD)
                h.image.colorFilter = null
            } else {
                h.image.setColorFilter(Color.GRAY)
                h.text.typeface = null
                h.text.setTypeface(null, Typeface.NORMAL)
            }
            h.text.text = tt.text.replace("\n".toRegex(), " ")
            if (tt.name == null || tt.name.isEmpty()) {
                (h.name.parent as TextMax).visibility = View.GONE
            } else {
                h.name.text = tt.name.replace("\n".toRegex(), " ")
            }
            h.itemView.setOnClickListener {
                val n = getItem(h.getAdapterPosition(this@BMAdapter)).tag as Storage.Bookmark
                onSelected(n)
                dialog.dismiss()
            }
            h.itemView.setOnLongClickListener {
                val menu = PopupMenu(context, h.itemView)
                val inflater = MenuInflater(context)
                inflater.inflate(R.menu.bookmark_menu, menu.menu)
                menu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit -> {
                            val popup = object : BookmarkPopup(h.itemView, tt, ArrayList()) {
                                override fun onDismiss() {
                                    if (t.parent == root) {
                                        onSave(tt)
                                    } else {
                                        val b = t.parent.tag as Storage.Book
                                        onSave(b, tt)
                                    }
                                    notifyDataSetChanged()
                                }
                            }
                            popup.show()
                        }
                        R.id.action_open -> {
                            if (t.parent == root) {
                                onSave(tt)
                            } else {
                                val b = t.parent.tag as Storage.Book
                                onSave(b, tt)
                            }
                        }
                        R.id.action_share -> {
                            val subject: String = if (t.parent == root) {
                                tt.name ?: ""
                            } else {
                                val b = t.parent.tag as Storage.Book
                                Storage.getTitle(b.info)
                            }
                            val text = "${tt.text}\n\n${tt.name}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = HttpClient.CONTENTTYPE_TEXT
                                putExtra(Intent.EXTRA_EMAIL, "")
                                putExtra(Intent.EXTRA_SUBJECT, subject)
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            if (OptimizationPreferenceCompat.isCallable(context, intent))
                                context.startActivity(intent)
                        }
                        R.id.action_delete -> {
                            AlertDialog.Builder(context)
                                .setTitle(R.string.delete_bookmark)
                                .setMessage(com.github.axet.androidlibrary.R.string.are_you_sure)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    if (t.parent == root) {
                                        onDelete(tt)
                                    } else {
                                        val b = t.parent.tag as Storage.Book
                                        onDelete(b, tt)
                                    }
                                    items.remove(t)
                                    notifyDataSetChanged()
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                        }
                    }
                    true
                }
                menu.show()
                false
            }
        }
    }

    /**
     * Адаптер для списка книг с закладками.
     */
    inner class BMAdapterBooks(books: ArrayList<Storage.Book>) : BMAdapter() {

        init {
            loadBooks(root, books)
            load()
        }

        private fun loadBooks(r: TreeListView.TreeNode, books: List<Storage.Book>) {
            for (b in books) {
                if (b.info.bookmarks != null) {
                    val n = TreeListView.TreeNode(r, b)
                    r.nodes.add(n)
                    load(n, b.info.bookmarks!!)
                }
            }
        }

        override fun onBindViewHolder(h: BMHolder, position: Int) {
            val t = getItem(h.getAdapterPosition(this))
            if (t.tag is Storage.Bookmark) {
                super.onBindViewHolder(h, position)
                h.itemView.setOnClickListener {
                    val tt = t.parent.tag as Storage.Book
                    val n = getItem(h.getAdapterPosition(this@BMAdapterBooks)).tag as Storage.Bookmark
                    onSelected(tt, n)
                    dialog.dismiss()
                }
            } else {
                val tt = t.tag as Storage.Book
                val ex = h.itemView.findViewById<ImageView>(R.id.expand)
                if (t.nodes.isEmpty())
                    ex.visibility = View.INVISIBLE
                else
                    ex.visibility = View.VISIBLE
                ex.setImageResource(if (t.expanded) R.drawable.ic_expand_less_black_24dp else R.drawable.ic_expand_more_black_24dp)
                h.itemView.setPadding(20 * t.level, 0, 0, 0)
                if (t.selected) {
                    h.text.typeface = null
                    h.text.setTypeface(null, Typeface.BOLD)
                    h.image.colorFilter = null
                } else {
                    h.image.setColorFilter(Color.GRAY)
                    h.text.typeface = null
                    h.text.setTypeface(null, Typeface.NORMAL)
                }
                h.text.text = Storage.getTitle(tt.info)
                (h.name.parent as TextMax).visibility = View.GONE
            }
        }
    }
}
