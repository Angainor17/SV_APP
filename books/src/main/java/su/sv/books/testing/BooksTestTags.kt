package su.sv.books.testing

/**
 * TestTags для UI элементов модуля books.
 *
 * Значения совпадают с test-side TestTags (app/src/androidTest/.../TestTags.kt),
 * чтобы UI тесты могли находить элементы через Modifier.testTag().
 */
object BooksTestTags {

    /** Каталог книг */
    object BooksCatalog {
        const val ROOT = "books_catalog_root"
        const val LIST = "books_list"
        const val ITEM = "book_item"
        const val ITEM_TITLE = "book_title"
        const val ITEM_AUTHOR = "book_author"
        const val ITEM_COVER = "book_cover"
        const val SEARCH_FIELD = "books_search_field"
        const val CATEGORY_FILTER = "category_filter"
        const val CATEGORY_CHIP = "category_chip"
        const val LOADING = "books_loading"
        const val ERROR = "books_error"
        const val BOOKMARKS_BUTTON = "books_bookmarks_button"
        const val DOWNLOADED_BUTTON = "books_downloaded_button"
    }

    /** Детали книги */
    object BookDetail {
        const val ROOT = "book_detail_root"
        const val COVER = "book_detail_cover"
        const val TITLE = "book_detail_title"
        const val AUTHOR = "book_detail_author"
        const val DESCRIPTION = "book_detail_description"
        const val READ_BUTTON = "book_read_button"
        const val DOWNLOAD_PROGRESS = "book_download_progress"
        const val LOADING = "book_detail_loading"
    }

    /** Скачанные книги */
    object DownloadedBooks {
        const val ROOT = "downloaded_books_root"
        const val LIST = "downloaded_books_list"
        const val ITEM = "downloaded_book_item"
        const val ITEM_TITLE = "downloaded_book_title"
        const val DELETE_BUTTON = "delete_book_button"
        const val EMPTY_STATE = "downloaded_empty_state"
    }

    /** Заметки (закладки) */
    object Bookmarks {
        const val ROOT = "bookmarks_root"
        const val LIST = "bookmarks_list"
        const val ITEM = "bookmark_item"
        const val ITEM_TITLE = "bookmark_title"
        const val ITEM_PREVIEW = "bookmark_preview"
        const val MODE_TOGGLE = "bookmarks_mode_toggle"
        const val MODE_LIST = "mode_list"
        const val MODE_BY_BOOK = "mode_by_book"
        const val EMPTY_STATE = "bookmarks_empty_state"
    }
}
