package org.geometerplus.zlibrary.core.encodings

class Encoding internal constructor(
    @JvmField val family: String?,
    @JvmField val name: String,
    @JvmField val displayName: String
) {

    override fun equals(other: Any?): Boolean = other is Encoding && name == other.name

    override fun hashCode(): Int = name.hashCode()

    fun createConverter(): EncodingConverter = EncodingConverter(name)
}
