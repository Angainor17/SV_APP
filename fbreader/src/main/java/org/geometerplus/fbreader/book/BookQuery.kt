package org.geometerplus.fbreader.book

class BookQuery(
    @JvmField val filter: Filter,
    @JvmField val limit: Int,
    @JvmField val page: Int
) {

    constructor(filter: Filter, limit: Int) : this(filter, limit, 0)

    fun next(): BookQuery = BookQuery(filter, limit, page + 1)
}
