package org.geometerplus.zlibrary.core.encodings

class AutoEncodingCollection : EncodingCollection() {
    private val myEncoding = Encoding(null, "auto", "auto")

    override fun encodings(): List<Encoding> = listOf(myEncoding)

    override fun getEncoding(alias: String): Encoding? = myEncoding

    override fun getEncoding(code: Int): Encoding? = myEncoding
}
