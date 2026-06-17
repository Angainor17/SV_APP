package org.geometerplus.fbreader.book

class UID(
    @JvmField val type: String,
    id: String
) {

    @JvmField
    val id: String = id.trim()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UID) return false
        return type == other.type && id == other.id
    }

    override fun hashCode(): Int = type.hashCode() + id.hashCode()
}
