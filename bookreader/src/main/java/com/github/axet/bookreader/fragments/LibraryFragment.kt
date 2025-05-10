package com.github.axet.bookreader.fragments

import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.view.get
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.axet.androidlibrary.net.HttpClient
import com.github.axet.androidlibrary.services.StorageProvider
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter.DownloadImageTask
import com.github.axet.androidlibrary.widgets.CacheImagesRecyclerAdapter
import com.github.axet.androidlibrary.widgets.InvalidateOptionsMenuCompat
import com.github.axet.androidlibrary.widgets.OpenFileDialog.EditTextDialog
import com.github.axet.androidlibrary.widgets.SearchView
import com.github.axet.androidlibrary.widgets.TextMax
import com.github.axet.bookreader.R
import com.github.axet.bookreader.activities.MainActivity
import com.github.axet.bookreader.activities.MainActivity.SearchListener
import com.github.axet.bookreader.app.BookApplication
import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.app.Storage.FBook
import com.github.axet.bookreader.fragments.LibraryFragment.BooksAdapter.BookHolder
import com.github.axet.bookreader.widgets.BookmarksDialog
import com.github.axet.bookreader.widgets.FBReaderView.ZLTextIndexPosition
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections.reverseOrder
import org.geometerplus.zlibrary.ui.android.R as ZlibraryR

class LibraryFragment : Fragment(), SearchListener {

    private lateinit var books: LibraryAdapter
    private lateinit var storage: Storage
    private lateinit var holder: FragmentHolder

    private var lastSearch: String? = ""
    private var invalidateOptionsMenu: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = Storage(requireContext())
        holder = FragmentHolder(requireContext())
        books = LibraryAdapter(holder)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        books.load()
        books.refresh()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_library, container, false)

        holder.create(v)
        holder.footer?.visibility = View.GONE

        val main = activity as MainActivity?
        main?.toolbar?.setTitle(R.string.app_name)
        holder.grid?.setAdapter(books)
        holder.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
            val b = books.getItem(position)
            main?.loadBookFromUri(b)
        })
        holder.setOnItemLongClickListener(OnItemLongClickListener { parent, view, position, id ->
            val b = books.getItem(position)
            val popup = PopupMenu(context, view)
            popup.inflate(R.menu.bookitem_menu)
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->
                if (item?.itemId == R.id.action_rename) {
                    val e = EditTextDialog(context)
                    e.setTitle(R.string.book_rename)
                    e.setText(b.info.title)
                    e.setPositiveButton(DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        val name = e.text
                        b.info.title = name
                        storage.save(b)
                        books.notifyDataSetChanged()
                    })
                    val d = e.create()
                    d.show()
                }
                if (item?.itemId == R.id.action_open) {
                    val ext = Storage.getExt(context, b.url)
                    val n = Storage.getTitle(b.info) + "." + ext
                    val open = StorageProvider.getProvider().openIntent(b.url, n)
                    startActivity(open)
                }
                if (item?.itemId == R.id.action_share) {
                    val ext = Storage.getExt(context, b.url)
                    val t = Storage.getTitle(b.info) + "." + ext
                    val name = Storage.getName(context, b.url)
                    val type = Storage.getTypeByName(name)
                    val share = StorageProvider.getProvider().shareIntent(b.url, t, type, t)
                    startActivity(share)
                }
                if (item?.itemId == R.id.action_delete) {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle(R.string.book_delete)
                    builder.setMessage(com.github.axet.androidlibrary.R.string.are_you_sure)
                    builder.setNegativeButton(
                        android.R.string.cancel,
                        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> })
                    builder.setPositiveButton(
                        android.R.string.ok,
                        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                            storage.delete(b)
                            books.delete(b)
                        })
                    builder.show()
                }
                true
            })
            popup.show()
            true
        })
        return v
    }

    override fun onStart() {
        super.onStart()
        val main = (requireActivity() as MainActivity?)
        main?.setFullscreen(false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onDestroy() {
        books.clearTasks()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        invalidateOptionsMenu =
            InvalidateOptionsMenuCompat.onCreateOptionsMenu(this, menu, inflater)

        val homeMenu = menu.findItem(R.id.action_home)
        val tocMenu = menu.findItem(R.id.action_toc)
        val bookmarksMenu = menu.findItem(R.id.action_bm)
        val searchMenu = menu.findItem(R.id.action_search)
        val reflow = menu.findItem(R.id.action_reflow)
        val fontsize = menu.findItem(R.id.action_fontsize)
        val debug = menu.findItem(R.id.action_debug)
        val rtl = menu.findItem(R.id.action_rtl)
        val mode = menu.findItem(R.id.action_mode)
        val sort = menu.findItem(R.id.action_sort)
        val tts = menu.findItem(R.id.action_tts)

        val shared = PreferenceManager.getDefaultSharedPreferences(context)
        val selected = requireContext().resources.getIdentifier(
            shared.getString(
                BookApplication.PREFERENCE_SORT,
                requireContext().resources.getResourceEntryName(R.id.sort_add_ask)
            ), "id", requireContext().packageName
        )
        val sorts = sort.subMenu
        for (i in 0..<sorts!!.size) {
            val m = sorts[i]
            if (m.itemId == selected) m.isChecked = true
            m.setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                false
            })
        }

        reflow.isVisible = false
        searchMenu.isVisible = true
        homeMenu.isVisible = false
        tocMenu.isVisible = false
        bookmarksMenu.isVisible = books.hasBookmarks()
        fontsize.isVisible = false
        debug.isVisible = false
        rtl.isVisible = false
        mode.isVisible = false
        tts.isVisible = false

        holder.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (holder.onOptionsItemSelected(item)) {
            invalidateOptionsMenu!!.run()
            return true
        }
        val shared = PreferenceManager.getDefaultSharedPreferences(context)
        val id = item.itemId
        if (id == R.id.sort_add_ask || id == R.id.sort_add_desc || id == R.id.sort_name_ask
            || id == R.id.sort_name_desc || id == R.id.sort_open_ask || id == R.id.sort_open_desc
        ) {
            shared.edit(commit = true) {
                putString(
                    BookApplication.PREFERENCE_SORT,
                    requireContext().resources.getResourceEntryName(item.itemId)
                )
            }
            books.sort()
            invalidateOptionsMenu?.run()
            return true
        } else if (id == R.id.action_bm) {
            val dialog: BookmarksDialog = object : BookmarksDialog(context) {
                override fun onSelected(b: Storage.Book, bm: Storage.Bookmark) {
                    val main = (activity as MainActivity?)
                    main?.openBook(b.url, ZLTextIndexPosition(bm.start, bm.end))
                }

                override fun onSave(book: Storage.Book, bm: Storage.Bookmark?) {
                    storage.save(book)
                }

                override fun onDelete(book: Storage.Book, bm: Storage.Bookmark?) {
                    book.info.bookmarks.remove(bm)
                    storage.save(book)
                }
            }
            dialog.load(books.all)
            dialog.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun search(s: String?) {
        books.filter = s
        books.refresh()
        lastSearch = books.filter
    }

    override fun searchClose() {
        search("")
    }

    override val hint: String?
        get() = getString(R.string.search_local)

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    class FragmentHolder(val context: Context) {

        private val inflater: LayoutInflater = LayoutInflater.from(context)

        var layout: Int = 0
        var grid: RecyclerView? = null
        var toolbar: View? = null
        var searchpanel: View? = null
        var searchtoolbar: LinearLayout? = null
        var footer: View? = null
        var footerButtons: View? = null
        var footerNext: View? = null
        var footerProgress: View? = null
        var footerStop: View? = null

        var clickListener: OnItemClickListener? = null
        var longClickListener: OnItemLongClickListener? = null

        fun create(v: View) {
            grid = v.findViewById<View?>(R.id.grid) as RecyclerView

            toolbar = v.findViewById<View>(R.id.search_header_toolbar_parent)
            searchpanel = v.findViewById<View?>(ZlibraryR.id.search_panel)
            searchtoolbar = v.findViewById<View?>(R.id.search_header_toolbar) as LinearLayout?

            toolbar?.visibility = View.GONE

            footer = inflater.inflate(R.layout.library_footer, null)
            footerButtons = footer!!.findViewById<View?>(R.id.search_footer_buttons)
            footerNext = footer!!.findViewById<View>(R.id.search_footer_next)
            footerProgress = footer!!.findViewById<View?>(R.id.search_footer_progress)
            footerStop = footer!!.findViewById<View?>(R.id.search_footer_stop)

            footerNext?.setOnClickListener(View.OnClickListener { v1: View? ->
                Log.d(TAG, "footer next")
            })

            addFooterView(footer)

            updateGrid()
        }

        fun getLayout(): String {
            return "library"
        }

        fun updateGrid() {
            val shared = PreferenceManager.getDefaultSharedPreferences(context)
            if (shared.getString(
                    BookApplication.PREFERENCE_LIBRARY_LAYOUT + getLayout(),
                    ""
                ) == "book_list_item"
            ) {
                setNumColumns(1)
                layout = R.layout.book_list_item
            } else {
                setNumColumns(4)
                layout = R.layout.book_item
            }
        }

        fun onCreateOptionsMenu(menu: Menu) {
            val grid = menu.findItem(R.id.action_grid)

            updateGrid()

            if (layout == R.layout.book_item) grid.setIcon(R.drawable.ic_view_module_black_24dp)
            else grid.setIcon(R.drawable.ic_view_list_black_24dp)
        }

        fun onOptionsItemSelected(item: MenuItem): Boolean {
            val shared = PreferenceManager.getDefaultSharedPreferences(context)
            val id = item.itemId
            if (id == R.id.action_grid) {
                shared.edit(commit = true) {
                    if (layout == R.layout.book_list_item) {
                        putString(
                            BookApplication.PREFERENCE_LIBRARY_LAYOUT + getLayout(),
                            "book_item"
                        )
                    } else {
                        putString(
                            BookApplication.PREFERENCE_LIBRARY_LAYOUT + getLayout(),
                            "book_list_item"
                        )
                    }
                }
                updateGrid()
                return true
            }
            return false
        }

        fun addFooterView(v: View?) {

        }

        fun setNumColumns(i: Int) {
            var reset: LinearLayoutManager? = null
            if (i == 1) {
                val lm = LinearLayoutManager(context)
                val l = grid!!.layoutManager
                if (l == null || l is GridLayoutManager) reset = lm
            } else {
                val lm = GridLayoutManager(context, i)
                lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return this@FragmentHolder.getSpanSize(position)
                    }
                }
                val l = grid!!.layoutManager
                if (l == null || (l !is GridLayoutManager) || l.spanCount != i) reset = lm
            }
            if (reset != null) grid!!.setLayoutManager(reset)
        }

        fun getSpanSize(position: Int): Int {
            return 1
        }

        fun setOnItemClickListener(l: OnItemClickListener?) {
            clickListener = l
        }

        fun setOnItemLongClickListener(l: OnItemLongClickListener?) {
            longClickListener = l
        }
    }

    class ByRecent : Comparator<Storage.Book> {
        override fun compare(o1: Storage.Book, o2: Storage.Book): Int {
            return o1.info.last.compareTo(o2.info.last)
        }
    }

    class ByCreated : Comparator<Storage.Book> {
        override fun compare(o1: Storage.Book, o2: Storage.Book): Int {
            return o1.info.created.compareTo(o2.info.created)
        }
    }

    class ByName : Comparator<Storage.Book> {
        override fun compare(o1: Storage.Book, o2: Storage.Book): Int {
            return Storage.getTitle(o1.info).compareTo(Storage.getTitle(o2.info))
        }
    }

    abstract class BooksAdapter(context: Context?, var holder: FragmentHolder) :
        CacheImagesRecyclerAdapter<BookHolder?>(context) {
        var filter: String? = null
        var client: HttpClient = HttpClient() // images client

        fun getCover(position: Int): Uri? {
            return null
        }

        open fun getAuthors(position: Int): String? {
            return ""
        }

        open fun getTitle(position: Int): String? {
            return ""
        }

        open fun refresh() {
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItemViewType(position: Int): Int {
            return -1
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookHolder {
            val inflater = LayoutInflater.from(context)
            val convertView = inflater.inflate(viewType, parent, false)
            return BookHolder(convertView)
        }

        override fun onBindViewHolder(h: BookHolder, position: Int) {
            h.itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (holder.clickListener != null) holder.clickListener!!.onItemClick(
                    null,
                    v,
                    h.getAdapterPosition(),
                    -1
                )
            })
            h.itemView.setOnLongClickListener(OnLongClickListener { v: View? ->
                if (holder.longClickListener != null) holder.longClickListener!!.onItemLongClick(
                    null,
                    v,
                    h.getAdapterPosition(),
                    -1
                )
                true
            })
            setText(h.aa, getAuthors(position))
            setText(h.tt, getTitle(position))
        }

        @Throws(IOException::class)
        override fun downloadImage(cover: Uri, f: File?): Bitmap {
            val w = client.getResponse(null, cover.toString())
            val out = FileOutputStream(f)
            IOUtils.copy(w.inputStream, out)
            w.inputStream.close()
            out.close()
            val bm = CacheImagesAdapter.createScaled(FileInputStream(f))
            val os = FileOutputStream(f)
            bm.compress(Bitmap.CompressFormat.PNG, 100, os)
            os.close()
            return bm
        }

        override fun downloadTaskUpdate(task: DownloadImageTask?, item: Any?, view: Any?) {
            val h = BookHolder((view as View?)!!)
            updateView(task, h.image, h.progress)
        }

        override fun downloadImageTask(task: DownloadImageTask): Bitmap? {
            val u = task.item as Uri?
            return downloadImage(u)
        }

        fun setText(t: TextView?, s: String?) {
            if (t == null) return
            var m: TextMax? = null
            if (t.parent is TextMax) m = t.parent as TextMax?
            t.parent
            if (s == null || s.isEmpty()) {
                t.visibility = View.GONE
                if (m != null) m.visibility = View.GONE
                return
            }
            t.visibility = View.VISIBLE
            t.text = s
            if (m != null) m.visibility = View.VISIBLE
        }

        class BookHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var aa: TextView = itemView.findViewById<TextView>(R.id.book_authors)
            var tt: TextView = itemView.findViewById<TextView>(R.id.book_title)
            var image: ImageView = itemView.findViewById<ImageView>(R.id.book_cover)
            var progress: ProgressBar = itemView.findViewById<ProgressBar>(R.id.book_progress)
        }
    }

    inner class LibraryAdapter(holder: FragmentHolder) :
        BooksAdapter(this@LibraryFragment.context, holder) {
        var all: ArrayList<Storage.Book> = ArrayList<Storage.Book>()
        var list: ArrayList<Storage.Book> = ArrayList<Storage.Book>()

        override fun getItemViewType(position: Int): Int {
            return holder.layout
        }

        override fun getAuthors(position: Int): String? {
            val b = list[position]
            return b.info.authors
        }

        override fun getTitle(position: Int): String? {
            val b = list[position]
            return b.info.title
        }

        override fun getItemCount(): Int {
            return list.size
        }

        fun getItem(position: Int): Storage.Book {
            return list[position]
        }

        fun load() {
            all = storage.list()
        }

        fun hasBookmarks(): Boolean {
            for (b in all) {
                if (b.info.bookmarks != null) return true
            }
            return false
        }

        fun delete(b: Storage.Book?) {
            all.remove(b)
            val i = list.indexOf(b)
            list.removeAt(i)
            notifyItemRemoved(i)
        }

        override fun refresh() {
            list.clear()
            if (filter == null || filter!!.isEmpty()) {
                list = ArrayList<Storage.Book>(all)
                clearTasks()
            } else {
                for (b in all) {
                    if (SearchView.filter(filter, Storage.getTitle(b.info))) list.add(b)
                }
            }
            sort()
        }

        fun sort() {
            val shared = PreferenceManager.getDefaultSharedPreferences(context)
            val selected = context.resources.getIdentifier(
                shared.getString(
                    BookApplication.PREFERENCE_SORT,
                    context.resources.getResourceEntryName(R.id.sort_add_ask)
                ), "id", context.packageName
            )

            when (selected) {
                R.id.sort_name_ask -> list.sortWith(ByName())
                R.id.sort_name_desc -> list.sortWith(reverseOrder<Storage.Book>(ByName()))
                R.id.sort_add_ask -> list.sortWith(ByCreated())
                R.id.sort_add_desc -> list.sortWith(reverseOrder<Storage.Book>(ByCreated()))
                R.id.sort_open_ask -> list.sortWith(ByRecent())
                R.id.sort_open_desc -> list.sortWith(reverseOrder<Storage.Book>(ByRecent()))
                else -> list.sortWith(ByCreated())
            }

            notifyDataSetChanged()
        }

        override fun onBindViewHolder(h: BookHolder, position: Int) {
            super.onBindViewHolder(h, position)

            val b = list[position]

            val convertView = h.itemView

            if (b.cover == null || !b.cover.exists()) {
                downloadTask(b, convertView)
            } else {
                downloadTaskClean(convertView)
                downloadTaskUpdate(null, b, convertView)
            }
        }

        override fun downloadImageTask(task: DownloadImageTask): Bitmap? {
            Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST)
            val book = task.item as Storage.Book
            var fbook: FBook? = null
            try {
                fbook = storage.read(book)
                val cover = Storage.coverFile(context, book)
                if (!cover.exists() || cover.length() == 0L) storage.createCover(fbook, cover)
                book.cover = cover
                try {
                    val bm = BitmapFactory.decodeStream(FileInputStream(cover))
                    return bm
                } catch (e: IOException) {
                    cover.delete()
                    throw RuntimeException(e)
                }
            } catch (e: RuntimeException) {
                Log.e(CacheImagesRecyclerAdapter.TAG, "Unable to load cover", e)
            } finally {
                fbook?.close()
            }
            return null
        }

        override fun downloadTaskUpdate(task: DownloadImageTask?, item: Any?, view: Any?) {
            super.downloadTaskUpdate(task, item, view)
            val h = BookHolder((view as View?)!!)
            val b = item as Storage.Book
            if (b.cover != null && b.cover.exists()) {
                try {
                    val bm = BitmapFactory.decodeStream(FileInputStream(b.cover))
                    h.image.setImageBitmap(bm)
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
        }
    }

    companion object {
        @JvmField
        val TAG: String = LibraryFragment::class.java.getSimpleName()

        @JvmStatic
        fun newInstance(): LibraryFragment {
            val fragment = LibraryFragment()
            val args = Bundle()
            fragment.setArguments(args)
            return fragment
        }
    }
}
