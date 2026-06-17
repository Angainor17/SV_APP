package org.geometerplus.fbreader.book

abstract class AbstractSerializer {

    abstract fun serialize(query: BookQuery): String

    abstract fun deserializeBookQuery(data: String): BookQuery?

    abstract fun serialize(query: BookmarkQuery): String

    abstract fun deserializeBookmarkQuery(data: String, creator: BookCreator<out AbstractBook>): BookmarkQuery?

    abstract fun serialize(book: AbstractBook): String

    abstract fun <B : AbstractBook> deserializeBook(data: String, creator: BookCreator<B>): B?

    abstract fun serialize(bookmark: Bookmark): String

    abstract fun deserializeBookmark(data: String): Bookmark?

    abstract fun serialize(style: HighlightingStyle): String

    abstract fun deserializeStyle(data: String): HighlightingStyle?

    interface BookCreator<B : AbstractBook> {
        fun createBook(id: Long, url: String?, title: String?, encoding: String?, language: String?): B?
    }
}
