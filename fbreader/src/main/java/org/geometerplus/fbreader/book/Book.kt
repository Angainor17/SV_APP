package org.geometerplus.fbreader.book

class Book internal constructor(
    id: Long,
    private val myPath: String,
    title: String?,
    encoding: String?,
    language: String?
) : AbstractBook(id, title, encoding, language) {

    init {
        require(myPath.isNotEmpty()) { "Creating book with no file" }
    }

    override fun getPath(): String = myPath

    override fun hashCode(): Int = myPath.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Book) return false
        return myPath == other.myPath
    }
}
