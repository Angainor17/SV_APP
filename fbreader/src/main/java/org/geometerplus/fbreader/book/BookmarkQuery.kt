package org.geometerplus.fbreader.book

class BookmarkQuery(
    @JvmField val book: AbstractBook?,
    @JvmField val limit: Int,
    @JvmField val visible: Boolean = true,
    @JvmField val page: Int = 0
) {

    constructor(limit: Int) : this(null, limit, true, 0)

    fun next(): BookmarkQuery = BookmarkQuery(book, limit, visible, page + 1)
}
