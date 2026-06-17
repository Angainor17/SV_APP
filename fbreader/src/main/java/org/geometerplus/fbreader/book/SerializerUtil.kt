package org.geometerplus.fbreader.book

object SerializerUtil {
    private val defaultSerializer: AbstractSerializer = XMLSerializer()

    @JvmStatic
    fun serialize(query: BookQuery?): String? =
        query?.let { defaultSerializer.serialize(it) }

    @JvmStatic
    fun deserializeBookQuery(xml: String?): BookQuery? =
        xml?.let { defaultSerializer.deserializeBookQuery(it) }

    @JvmStatic
    fun serialize(query: BookmarkQuery?): String? =
        query?.let { defaultSerializer.serialize(it) }

    @JvmStatic
    fun deserializeBookmarkQuery(xml: String?, creator: AbstractSerializer.BookCreator<out AbstractBook>): BookmarkQuery? =
        xml?.let { defaultSerializer.deserializeBookmarkQuery(it, creator) }

    @JvmStatic
    fun serialize(book: AbstractBook?): String? =
        book?.let { defaultSerializer.serialize(it) }

    @JvmStatic
    fun <B : AbstractBook> deserializeBook(xml: String?, creator: AbstractSerializer.BookCreator<B>): B? =
        xml?.let { defaultSerializer.deserializeBook(it, creator) }

    @JvmStatic
    fun serializeBookList(books: List<AbstractBook>): List<String> {
        val serialized = ArrayList<String>(books.size)
        for (b in books) {
            serialized.add(defaultSerializer.serialize(b))
        }
        return serialized
    }

    @JvmStatic
    fun <B : AbstractBook> deserializeBookList(xmlList: List<String>, creator: AbstractSerializer.BookCreator<B>): List<B> {
        val books = ArrayList<B>(xmlList.size)
        for (xml in xmlList) {
            val b = defaultSerializer.deserializeBook(xml, creator)
            if (b != null) {
                books.add(b)
            }
        }
        return books
    }

    @JvmStatic
    fun serialize(bookmark: Bookmark?): String? =
        bookmark?.let { defaultSerializer.serialize(it) }

    @JvmStatic
    fun deserializeBookmark(xml: String?): Bookmark? =
        xml?.let { defaultSerializer.deserializeBookmark(it) }

    @JvmStatic
    fun serializeBookmarkList(bookmarks: List<Bookmark>): List<String> {
        val serialized = ArrayList<String>(bookmarks.size)
        for (b in bookmarks) {
            serialized.add(defaultSerializer.serialize(b))
        }
        return serialized
    }

    @JvmStatic
    fun deserializeBookmarkList(xmlList: List<String>): List<Bookmark> {
        val bookmarks = ArrayList<Bookmark>(xmlList.size)
        for (xml in xmlList) {
            val b = defaultSerializer.deserializeBookmark(xml)
            if (b != null) {
                bookmarks.add(b)
            }
        }
        return bookmarks
    }

    @JvmStatic
    fun serialize(style: HighlightingStyle?): String? =
        style?.let { defaultSerializer.serialize(it) }

    @JvmStatic
    fun deserializeStyle(xml: String?): HighlightingStyle? =
        xml?.let { defaultSerializer.deserializeStyle(it) }

    @JvmStatic
    fun serializeStyleList(styles: List<HighlightingStyle>): List<String> {
        val serialized = ArrayList<String>(styles.size)
        for (s in styles) {
            serialized.add(defaultSerializer.serialize(s))
        }
        return serialized
    }

    @JvmStatic
    fun deserializeStyleList(xmlList: List<String>): List<HighlightingStyle> {
        val styles = ArrayList<HighlightingStyle>(xmlList.size)
        for (xml in xmlList) {
            val s = defaultSerializer.deserializeStyle(xml)
            if (s != null) {
                styles.add(s)
            }
        }
        return styles
    }
}
