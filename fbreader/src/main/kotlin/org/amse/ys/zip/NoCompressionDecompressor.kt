package org.amse.ys.zip

class NoCompressionDecompressor(
    inputStream: MyBufferedInputStream,
    header: LocalFileHeader
) : Decompressor() {
    private val myHeader: LocalFileHeader = header
    private val myStream: MyBufferedInputStream = inputStream
    private var myCurrentPosition = 0

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        val left = available()
        if (left <= 0) {
            return -1
        }
        val r = myStream.read(b, off, minOf(len, left))
        myCurrentPosition += r
        return r
    }

    override fun read(): Int {
        return if (myCurrentPosition < myHeader.CompressedSize) {
            myCurrentPosition++
            myStream.read()
        } else {
            -1
        }
    }

    override fun available(): Int = myHeader.UncompressedSize - myCurrentPosition
}
