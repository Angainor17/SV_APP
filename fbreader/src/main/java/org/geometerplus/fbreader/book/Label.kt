package org.geometerplus.fbreader.book

import java.util.UUID

class Label(@JvmField val uid: String, @JvmField val name: String) {

    init {
        if (uid.isEmpty() || name.isEmpty()) {
            throw IllegalArgumentException("Label($uid,$name)")
        }
    }

    internal constructor(name: String) : this(UUID.randomUUID().toString(), name)

    override fun toString(): String = "$name[$uid]"

    override fun equals(other: Any?): Boolean {
        if (other !is Label) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}
