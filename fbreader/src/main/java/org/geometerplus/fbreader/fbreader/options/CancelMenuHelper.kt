package org.geometerplus.fbreader.fbreader.options

import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.BookmarkQuery
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.resources.ZLResource

class CancelMenuHelper {

    companion object {
        private const val GROUP_NAME = "CancelMenu"
    }

    val showLibraryItemOption = ZLBooleanOption(GROUP_NAME, "library", true)
    val showNetworkLibraryItemOption = ZLBooleanOption(GROUP_NAME, "networkLibrary", true)
    val showPreviousBookItemOption = ZLBooleanOption(GROUP_NAME, "previousBook", false)
    val showPositionItemsOption = ZLBooleanOption(GROUP_NAME, "positions", true)

    init {
        Config.Instance()?.requestAllValuesForGroup(GROUP_NAME)
    }

    fun getActionsList(collection: IBookCollection<Book>): List<ActionDescription> {
        val list = mutableListOf<ActionDescription>()

        if (showLibraryItemOption.value) {
            list.add(ActionDescription(ActionType.library, null))
        }
        if (showNetworkLibraryItemOption.value) {
            list.add(ActionDescription(ActionType.networkLibrary, null))
        }
        if (showPreviousBookItemOption.value) {
            val previousBook = collection.getRecentBook(1)
            if (previousBook != null) {
                list.add(ActionDescription(ActionType.previousBook, previousBook.title))
            }
        }
        if (showPositionItemsOption.value) {
            val currentBook = collection.getRecentBook(0)
            if (currentBook != null) {
                val bookmarks = collection.bookmarks(
                    BookmarkQuery(currentBook, 3, false)
                )
                for (b in bookmarks.sortedWith(Bookmark.ByTimeComparator())) {
                    list.add(BookmarkDescription(b))
                }
            }
        }
        list.add(ActionDescription(ActionType.close, null))

        return list
    }

    enum class ActionType {
        library,
        networkLibrary,
        previousBook,
        returnTo,
        close
    }

    open class ActionDescription(val type: ActionType, val summary: String?) {
        val title: String

        init {
            val resource = ZLResource.resource("cancelMenu")
            title = resource.getResource(type.toString()).value
        }
    }

    class BookmarkDescription(val bookmark: Bookmark) : ActionDescription(ActionType.returnTo, bookmark.text)
}
