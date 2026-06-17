package org.geometerplus.fbreader.book

import org.geometerplus.fbreader.sort.TitledEntity

class Series(title: String?) : TitledEntity<Series>(title) {

    override val language: String?
        get() = "en" // TODO: return real language

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Series) return false
        return title == other.title
    }

    override fun hashCode(): Int = title.hashCode()
}
