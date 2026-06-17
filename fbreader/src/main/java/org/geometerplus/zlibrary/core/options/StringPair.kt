package org.geometerplus.zlibrary.core.options

class StringPair internal constructor(
    @JvmField val group: String,
    @JvmField val name: String
) {
    init {
        group.intern()
        name.intern()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        return try {
            val pair = other as StringPair
            group == pair.group && name == pair.name
        } catch (e: ClassCastException) {
            false
        }
    }

    override fun hashCode(): Int = group.hashCode() + 37 * name.hashCode()
}
