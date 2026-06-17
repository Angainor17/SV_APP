package org.geometerplus.fbreader.book

class Author internal constructor(
    @JvmField val displayName: String,
    @JvmField val sortKey: String
) : Comparable<Author> {

    companion object {
        @JvmField
        val NULL = Author("", "")

        @JvmStatic
        fun create(name: String?, sortKey: String?): Author? {
            if (name == null) {
                return null
            }
            var strippedName = name.trim()
            if (strippedName.isEmpty()) {
                return null
            }

            var strippedKey = sortKey?.trim() ?: ""
            if (strippedKey.isEmpty()) {
                var index = strippedName.lastIndexOf(' ')
                if (index == -1) {
                    strippedKey = strippedName
                } else {
                    strippedKey = strippedName.substring(index + 1)
                    while (index >= 0 && strippedName[index] == ' ') {
                        --index
                    }
                    strippedName = strippedName.substring(0, index + 1) + ' ' + strippedKey
                }
            }

            return Author(strippedName, strippedKey)
        }

        @JvmStatic
        fun hashCode(author: Author?): Int = author?.hashCode() ?: 0
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Author) return false
        return sortKey == other.sortKey && displayName == other.displayName
    }

    override fun hashCode(): Int = sortKey.hashCode() + displayName.hashCode()

    override fun compareTo(other: Author): Int {
        val byKeys = sortKey.compareTo(other.sortKey)
        return if (byKeys != 0) byKeys else displayName.compareTo(other.displayName)
    }

    override fun toString(): String = "$displayName ($sortKey)"
}
