package org.amse.ys.zip

import java.io.IOException
import java.util.LinkedList
import java.util.Queue

abstract class Decompressor {
    constructor(inputStream: MyBufferedInputStream, header: LocalFileHeader)

    protected constructor()

    @Throws(IOException::class)
    abstract fun read(b: ByteArray?, off: Int, len: Int): Int

    @Throws(IOException::class)
    abstract fun read(): Int

    open fun available(): Int = -1

    companion object {
        private val ourDeflators: Queue<DeflatingDecompressor> = LinkedList()

        @JvmStatic
        fun storeDecompressor(decompressor: Decompressor) {
            if (decompressor is DeflatingDecompressor) {
                synchronized(ourDeflators) {
                    ourDeflators.add(decompressor)
                }
            }
        }

        @JvmStatic
        @Throws(IOException::class)
        fun init(inputStream: MyBufferedInputStream, header: LocalFileHeader): Decompressor {
            when (header.CompressionMethod) {
                0 -> return NoCompressionDecompressor(inputStream, header)
                8 -> {
                    synchronized(ourDeflators) {
                        if (!ourDeflators.isEmpty()) {
                            val decompressor = ourDeflators.poll()!!
                            decompressor.reset(inputStream, header)
                            return decompressor
                        }
                    }
                    return DeflatingDecompressor(inputStream, header)
                }
                else -> throw ZipException("Unsupported method of compression")
            }
        }
    }
}
