/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.library

import android.app.AlertDialog
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import org.geometerplus.R
import org.geometerplus.android.fbreader.FBReader
import org.geometerplus.android.fbreader.FBUtil
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow
import org.geometerplus.android.fbreader.tree.TreeActivity
import org.geometerplus.android.util.DeviceType
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.android.util.PackageUtil
import org.geometerplus.android.util.SearchDialogUtil
import org.geometerplus.android.util.UIMessageUtil
import org.geometerplus.fbreader.Paths
import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookEvent
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.book.Filter
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.book.IBookCollection.Status
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.fbreader.library.BookTree
import org.geometerplus.fbreader.library.ExternalViewTree
import org.geometerplus.fbreader.library.FileTree
import org.geometerplus.fbreader.library.LibraryTree
import org.geometerplus.fbreader.library.RootTree
import org.geometerplus.fbreader.library.SyncLabelTree
import org.geometerplus.fbreader.tree.FBTree
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.resources.ZLResource
import java.util.LinkedList

class LibraryActivity : TreeActivity<LibraryTree>(), MenuItem.OnMenuItemClickListener, View.OnCreateContextMenuListener, IBookCollection.Listener<Book> {

    private val myCollection = BookCollectionShadow()
    private val bookSearchPatternOption = ZLStringOption("BookSearch", "Pattern", "")
    @Volatile private var myRootTree: RootTree? = null
    private var mySelectedBook: Book? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        mySelectedBook = FBReaderIntents.getBookExtra(intent, myCollection)

        LibraryTreeAdapter(this)

        listView.isTextFilterEnabled = true
        listView.setOnCreateContextMenuListener(this)

        deleteRootTree()

        myCollection.bindToService(this) {
            setProgressBarIndeterminateVisibility(myCollection.status() != Status.Succeeded && myCollection.status() != Status.Failed)
            myRootTree = RootTree(myCollection, PluginCollection.Instance(Paths.systemInfo(this@LibraryActivity)))
            myCollection.addListener(this@LibraryActivity)
            init(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        if (START_SEARCH_ACTION == intent.action) {
            val pattern = intent.getStringExtra(SearchManager.QUERY)
            if (!pattern.isNullOrEmpty()) {
                startBookSearch(pattern)
            }
        } else {
            super.onNewIntent(intent)
        }
    }

    override fun getTreeByKey(key: FBTree.Key): LibraryTree? {
        return myRootTree?.getLibraryTree(key) ?: myRootTree
    }

    @Synchronized
    private fun deleteRootTree() {
        if (myRootTree != null) {
            myCollection.removeListener(this)
            myCollection.unbind()
            myRootTree = null
        }
    }

    override fun onDestroy() {
        deleteRootTree()
        super.onDestroy()
    }

    override fun isTreeSelected(tree: FBTree): Boolean {
        val lTree = tree as LibraryTree
        val selectedBook = mySelectedBook
        return lTree.isSelectable() && selectedBook != null && lTree.containsBook(selectedBook)
    }

    override fun onListItemClick(listView: ListView, view: View, position: Int, rowId: Long) {
        val tree = getListAdapter().getItem(position) as LibraryTree
        if (tree is ExternalViewTree) {
            runOrInstallExternalView(true)
        } else {
            val book = tree.getBook()
            if (book != null) {
                showBookInfo(book)
            } else {
                openTree(tree)
            }
        }
    }

    private fun showBookInfo(book: Book) {
        val intent = Intent(applicationContext, BookInfoActivity::class.java)
        FBReaderIntents.putBookExtra(intent, book)
        OrientationUtil.startActivity(this, intent)
    }

    private fun openSearchResults() {
        val tree = myRootTree?.getSearchResultsTree()
        if (tree != null) {
            openTree(tree)
        }
    }

    override fun onSearchRequested(): Boolean {
        if (DeviceType.Instance().hasStandardSearchDialog()) {
            startSearch(bookSearchPatternOption.value, true, null, false)
        } else {
            SearchDialogUtil.showDialog(this, LibrarySearchActivity::class.java, bookSearchPatternOption.value, null)
        }
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo) {
        val position = (menuInfo as AdapterView.AdapterContextMenuInfo).position
        val book = (getListAdapter().getItem(position) as LibraryTree).getBook()
        if (book != null) {
            createBookContextMenu(menu, book)
        }
    }

    private fun createBookContextMenu(menu: ContextMenu, book: Book) {
        val resource = LibraryTree.resource()
        menu.setHeaderTitle(book.title)
        menu.add(0, CONTEXT_ITEM_OPEN_BOOK, 0, resource.getResource("openBook").value)
        menu.add(0, CONTEXT_ITEM_SHOW_BOOK_INFO, 0, resource.getResource("showBookInfo").value)
        if (BookUtil.fileByBook(book).physicalFile != null) {
            menu.add(0, CONTEXT_ITEM_SHARE_BOOK, 0, resource.getResource("shareBook").value)
        }
        if (book.hasLabel(AbstractBook.FAVORITE_LABEL)) {
            menu.add(0, CONTEXT_ITEM_REMOVE_FROM_FAVORITES, 0, resource.getResource("removeFromFavorites").value)
        } else {
            menu.add(0, CONTEXT_ITEM_ADD_TO_FAVORITES, 0, resource.getResource("addToFavorites").value)
        }
        if (book.hasLabel(AbstractBook.READ_LABEL)) {
            menu.add(0, CONTEXT_ITEM_MARK_AS_UNREAD, 0, resource.getResource("markAsUnread").value)
        } else {
            menu.add(0, CONTEXT_ITEM_MARK_AS_READ, 0, resource.getResource("markAsRead").value)
        }
        if (myCollection.canRemoveBook(book, true)) {
            menu.add(0, CONTEXT_ITEM_DELETE_BOOK, 0, resource.getResource("deleteBook").value)
        }
        if (book.hasLabel(AbstractBook.SYNC_DELETED_LABEL)) {
            menu.add(0, CONTEXT_ITEM_UPLOAD_AGAIN, 0, resource.getResource("uploadAgain").value)
        }
        if (book.hasLabel(AbstractBook.SYNC_FAILURE_LABEL)) {
            menu.add(0, CONTEXT_ITEM_TRY_AGAIN, 0, resource.getResource("tryAgain").value)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = (item.menuInfo as AdapterView.AdapterContextMenuInfo).position
        val book = (getListAdapter().getItem(position) as LibraryTree).getBook()
        if (book != null) {
            return onContextItemSelected(item.itemId, book)
        }
        return super.onContextItemSelected(item)
    }

    private fun syncAgain(book: Book) {
        book.removeLabel(AbstractBook.SYNC_FAILURE_LABEL)
        book.removeLabel(AbstractBook.SYNC_DELETED_LABEL)
        book.addNewLabel(AbstractBook.SYNC_TOSYNC_LABEL)
        myCollection.saveBook(book)
    }

    private fun onContextItemSelected(itemId: Int, book: Book): Boolean {
        when (itemId) {
            CONTEXT_ITEM_OPEN_BOOK -> {
                FBReader.openBookActivity(this, book, null)
                return true
            }
            CONTEXT_ITEM_SHOW_BOOK_INFO -> {
                showBookInfo(book)
                return true
            }
            CONTEXT_ITEM_SHARE_BOOK -> {
                FBUtil.shareBook(this, book)
                return true
            }
            CONTEXT_ITEM_ADD_TO_FAVORITES -> {
                book.addNewLabel(AbstractBook.FAVORITE_LABEL)
                myCollection.saveBook(book)
                return true
            }
            CONTEXT_ITEM_REMOVE_FROM_FAVORITES -> {
                book.removeLabel(AbstractBook.FAVORITE_LABEL)
                myCollection.saveBook(book)
                val tree = getCurrentTree()
                if (tree?.onBookEvent(BookEvent.Updated, book) == true) {
                    getListAdapter().replaceAll(tree.subtrees(), true)
                }
                return true
            }
            CONTEXT_ITEM_MARK_AS_READ -> {
                book.addNewLabel(AbstractBook.READ_LABEL)
                myCollection.saveBook(book)
                listView.invalidateViews()
                return true
            }
            CONTEXT_ITEM_MARK_AS_UNREAD -> {
                book.removeLabel(AbstractBook.READ_LABEL)
                myCollection.saveBook(book)
                listView.invalidateViews()
                return true
            }
            CONTEXT_ITEM_DELETE_BOOK -> {
                tryToDeleteBook(book)
                return true
            }
            CONTEXT_ITEM_UPLOAD_AGAIN, CONTEXT_ITEM_TRY_AGAIN -> {
                syncAgain(book)
                val tree = getCurrentTree()
                if (tree?.onBookEvent(BookEvent.Updated, book) == true) {
                    getListAdapter().replaceAll(tree.subtrees(), true)
                }
                return true
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        addMenuItem(menu, OPTIONS_ITEM_SEARCH, "localSearch", R.drawable.ic_menu_search)
        addMenuItem(menu, OPTIONS_ITEM_RESCAN, "rescan", R.drawable.ic_menu_refresh)
        addMenuItem(menu, OPTIONS_ITEM_UPLOAD_AGAIN, "uploadAgain", -1)
        addMenuItem(menu, OPTIONS_ITEM_TRY_AGAIN, "tryAgain", -1)
        addMenuItem(menu, OPTIONS_ITEM_DELETE_ALL, "deleteAll", -1)
        if (Build.VERSION.SDK_INT >= 9) {
            addMenuItem(menu, OPTIONS_ITEM_EXTERNAL_VIEW, "bookshelfView", -1)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        var enableUploadAgain = false
        var enableTryAgain = false
        var enableDeleteAll = false
        val tree = getCurrentTree()
        if (tree is SyncLabelTree) {
            val label = tree.label
            if (AbstractBook.SYNC_DELETED_LABEL == label) {
                enableUploadAgain = true
                enableDeleteAll = true
            } else if (AbstractBook.SYNC_FAILURE_LABEL == label) {
                enableTryAgain = true
            }
        }

        val rescanItem = menu.findItem(OPTIONS_ITEM_RESCAN)
        myCollection.bindToService(this) {
            val status = myCollection.status()
            rescanItem.isEnabled = status == Status.Succeeded || status == Status.Failed
        }
        rescanItem.isVisible = tree == myRootTree
        menu.findItem(OPTIONS_ITEM_UPLOAD_AGAIN).isVisible = enableUploadAgain
        menu.findItem(OPTIONS_ITEM_TRY_AGAIN).isVisible = enableTryAgain
        menu.findItem(OPTIONS_ITEM_DELETE_ALL).isVisible = enableDeleteAll

        return true
    }

    private fun addMenuItem(menu: Menu, id: Int, resourceKey: String, iconId: Int): MenuItem {
        val label = LibraryTree.resource().getResource(resourceKey).value
        val item = menu.add(0, id, Menu.NONE, label)
        item.setOnMenuItemClickListener(this)
        if (iconId != -1) {
            item.setIcon(iconId)
        }
        return item
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            OPTIONS_ITEM_SEARCH -> return onSearchRequested()
            OPTIONS_ITEM_RESCAN -> {
                val status = myCollection.status()
                if (status == Status.Succeeded || status == Status.Failed) {
                    myCollection.reset(true)
                    openTree(myRootTree!!)
                }
                return true
            }
            OPTIONS_ITEM_UPLOAD_AGAIN, OPTIONS_ITEM_TRY_AGAIN -> {
                val tree = getCurrentTree()
                for (subtree in tree?.subtrees() ?: emptyList()) {
                    if (subtree is BookTree) {
                        syncAgain(subtree.book)
                    }
                }
                tree?.let { getListAdapter().replaceAll(it.subtrees(), true) }
                return true
            }
            OPTIONS_ITEM_DELETE_ALL -> {
                val books = LinkedList<Book>()
                val tree = getCurrentTree()
                for (subtree in tree?.subtrees() ?: emptyList()) {
                    if (subtree is BookTree) {
                        books.add(subtree.book)
                    }
                }
                tryToDeleteBooks(books)
                return true
            }
            OPTIONS_ITEM_EXTERNAL_VIEW -> {
                runOrInstallExternalView(true)
                return true
            }
            else -> return true
        }
    }

    private fun runOrInstallExternalView(install: Boolean) {
        try {
            startActivity(Intent(FBReaderIntents.Action.EXTERNAL_LIBRARY))
            finish()
        } catch (e: ActivityNotFoundException) {
            if (install) {
                PackageUtil.installFromMarket(this, "org.fbreader.plugin.library")
            }
        }
    }

    private fun tryToDeleteBooks(books: List<Book>) {
        val size = books.size
        if (size == 0) {
            return
        }
        val dialogResource = ZLResource.resource("dialog")
        val buttonResource = dialogResource.getResource("button")
        val boxResource = dialogResource.getResource(
            if (size == 1) "deleteBookBox" else "deleteMultipleBookBox"
        )
        val title = if (size == 1) books[0].title else boxResource.getResource("title").value
        val message = boxResource.getResource("message").getValue(size).replace("%s", size.toString())
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setIcon(0)
            .setPositiveButton(buttonResource.getResource("yes").value, BookDeleter(books))
            .setNegativeButton(buttonResource.getResource("no").value, null)
            .create().show()
    }

    private fun tryToDeleteBook(book: Book) {
        tryToDeleteBooks(listOf(book))
    }

    private fun startBookSearch(pattern: String) {
        bookSearchPatternOption.value = pattern

        val searcher = Thread(Runnable {
            val oldSearchResults = myRootTree?.getSearchResultsTree()

            if (oldSearchResults != null && pattern == oldSearchResults.pattern) {
                onSearchEvent(true)
            } else if (myCollection.hasBooks(Filter.ByPattern(pattern))) {
                oldSearchResults?.removeSelf()
                myRootTree?.createSearchResultsTree(pattern)
                onSearchEvent(true)
            } else {
                onSearchEvent(false)
            }
        }, "Library.searchBooks")
        searcher.priority = (Thread.MIN_PRIORITY + Thread.NORM_PRIORITY) / 2
        searcher.start()
    }

    private fun onSearchEvent(found: Boolean) {
        runOnUiThread {
            if (found) {
                openSearchResults()
            } else {
                UIMessageUtil.showErrorMessage(this@LibraryActivity, "bookNotFound")
            }
        }
    }

    override fun onBookEvent(event: BookEvent, book: Book?) {
        val tree = getCurrentTree()
        if (book != null && tree?.onBookEvent(event, book) == true) {
            getListAdapter().replaceAll(tree.subtrees(), true)
        }
    }

    override fun onBuildEvent(status: IBookCollection.Status) {
        setProgressBarIndeterminateVisibility(status != Status.Succeeded && status != Status.Failed)
    }

    private inner class BookDeleter(private val myBooks: List<Book>) : DialogInterface.OnClickListener {

        override fun onClick(dialog: DialogInterface, which: Int) {
            val currentTree = getCurrentTree()
            if (currentTree is FileTree) {
                for (book in myBooks) {
                    getListAdapter().remove(FileTree(currentTree, BookUtil.fileByBook(book)))
                    myCollection.removeBook(book, true)
                }
                listView.invalidateViews()
            } else if (currentTree != null) {
                var doReplace = false
                for (book in myBooks) {
                    doReplace = doReplace || currentTree.onBookEvent(BookEvent.Removed, book)
                    myCollection.removeBook(book, true)
                }
                if (doReplace) {
                    getListAdapter().replaceAll(currentTree.subtrees(), true)
                }
            }
        }
    }

    companion object {
        const val START_SEARCH_ACTION = "action.fbreader.library.start-search"

        private const val CONTEXT_ITEM_OPEN_BOOK = 0
        private const val CONTEXT_ITEM_SHOW_BOOK_INFO = 1
        private const val CONTEXT_ITEM_SHARE_BOOK = 2
        private const val CONTEXT_ITEM_ADD_TO_FAVORITES = 3
        private const val CONTEXT_ITEM_REMOVE_FROM_FAVORITES = 4
        private const val CONTEXT_ITEM_MARK_AS_READ = 5
        private const val CONTEXT_ITEM_MARK_AS_UNREAD = 6
        private const val CONTEXT_ITEM_DELETE_BOOK = 7
        private const val CONTEXT_ITEM_UPLOAD_AGAIN = 8
        private const val CONTEXT_ITEM_TRY_AGAIN = 9

        private const val OPTIONS_ITEM_SEARCH = 0
        private const val OPTIONS_ITEM_RESCAN = 1
        private const val OPTIONS_ITEM_UPLOAD_AGAIN = 2
        private const val OPTIONS_ITEM_TRY_AGAIN = 3
        private const val OPTIONS_ITEM_DELETE_ALL = 4
        private const val OPTIONS_ITEM_EXTERNAL_VIEW = 5
    }
}
