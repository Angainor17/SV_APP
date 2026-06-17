package org.geometerplus.zlibrary.core.encodings

abstract class EncodingCollection {
    abstract fun encodings(): List<Encoding>
    abstract fun getEncoding(alias: String): Encoding?
    abstract fun getEncoding(code: Int): Encoding?
}
