package org.amse.ys.zip

import java.io.InputStream

internal class ZipInputStream(
    private val myParent: ZipFile,
    header: LocalFileHeader
) : InputStream() {
    private val myBaseStream: MyBufferedInputStream = myParent.getBaseStream()
    private val myDecompressor: Decompressor
    private var myIsClosed = false

    init {
        myBaseStream.setPosition(header.DataOffset)
        myDecompressor = Decompressor.init(myBaseStream, header)
    }

    override fun available(): Int = myDecompressor.available()

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        if (b == null) {
            throw NullPointerException()
        } else if (off < 0 || len < 0 || off + len > b.size) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return 0
        }

        return myDecompressor.read(b, off, len)
    }

    override fun read(): Int = myDecompressor.read()

    override fun close() {
        if (!myIsClosed) {
            myIsClosed = true
            myParent.storeBaseStream(myBaseStream)
            Decompressor.storeDecompressor(myDecompressor)
        }
    }

    @Suppress("DEPRECATION")
    @Throws(Throwable::class)
    protected fun finalize() {
        close()
    }
}
