package org.geometerplus.fbreader.book

sealed class Filter {
    abstract fun matches(book: AbstractBook?): Boolean

    class Empty : Filter() {
        override fun matches(book: AbstractBook?): Boolean = true
    }

    class ByAuthor(@JvmField val author: Author) : Filter() {
        override fun matches(book: AbstractBook?): Boolean {
            val bookAuthors = book?.authors() ?: emptyList()
            return if (author == Author.NULL) bookAuthors.isEmpty() else bookAuthors.contains(author)
        }
    }

    class ByTag(@JvmField val tag: Tag) : Filter() {
        override fun matches(book: AbstractBook?): Boolean {
            val bookTags = book?.tags() ?: emptyList()
            return if (tag == Tag.NULL) bookTags.isEmpty() else bookTags.contains(tag)
        }
    }

    class ByLabel(@JvmField val label: String) : Filter() {
        override fun matches(book: AbstractBook?): Boolean = book?.hasLabel(label) ?: false
    }

    class ByPattern(@JvmField val pattern: String) : Filter() {
        override fun matches(book: AbstractBook?): Boolean {
            return book != null && pattern.isNotEmpty() && book.matches(pattern)
        }
    }

    class ByTitlePrefix(@JvmField val prefix: String) : Filter() {
        override fun matches(book: AbstractBook?): Boolean {
            return book != null && prefix.isNotEmpty() && prefix == book.firstTitleLetter()
        }
    }

    class BySeries(@JvmField val series: Series) : Filter() {
        override fun matches(book: AbstractBook?): Boolean {
            val info = book?.getSeriesInfo()
            return info != null && series == info.series
        }
    }

    class HasBookmark : Filter() {
        override fun matches(book: AbstractBook?): Boolean = book != null && book.hasBookmark
    }

    class HasPhysicalFile : Filter() {
        override fun matches(book: AbstractBook?): Boolean = book != null && book.getPath().startsWith("/")
    }

    class And(@JvmField val first: Filter, @JvmField val second: Filter) : Filter() {
        override fun matches(book: AbstractBook?): Boolean = first.matches(book) && second.matches(book)
    }

    class Or(@JvmField val first: Filter, @JvmField val second: Filter) : Filter() {
        override fun matches(book: AbstractBook?): Boolean = first.matches(book) || second.matches(book)
    }

    class Not(@JvmField val base: Filter) : Filter() {
        override fun matches(book: AbstractBook?): Boolean = !base.matches(book)
    }
}
