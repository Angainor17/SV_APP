package org.amse.ys.zip

internal class DeflatingDecompressor(
    inputStream: MyBufferedInputStream,
    header: LocalFileHeader
) : Decompressor() {
    private val myInBuffer = ByteArray(IN_BUFFER_SIZE)
    private val myOutBuffer = ByteArray(OUT_BUFFER_SIZE)

    private lateinit var myStream: MyBufferedInputStream
    private var myCompressedAvailable = 0
    private var myAvailable = 0
    private var myInBufferOffset = 0
    private var myInBufferLength = 0
    private var myOutBufferOffset = 0
    private var myOutBufferLength = 0

    @Volatile
    private var myInflatorId = -1

    init {
        reset(inputStream, header)
    }

    internal fun reset(inputStream: MyBufferedInputStream, header: LocalFileHeader) {
        if (myInflatorId != -1) {
            endInflating(myInflatorId)
            myInflatorId = -1
        }
        myStream = inputStream
        myCompressedAvailable = header.CompressedSize
        if (myCompressedAvailable <= 0) {
            myCompressedAvailable = Integer.MAX_VALUE
        }
        myAvailable = header.UncompressedSize
        if (myAvailable <= 0) {
            myAvailable = Integer.MAX_VALUE
        }
        myInBufferOffset = IN_BUFFER_SIZE
        myInBufferLength = 0
        myOutBufferOffset = OUT_BUFFER_SIZE
        myOutBufferLength = 0
        myInflatorId = startInflating()
        if (myInflatorId == -1) {
            throw ZipException("cannot start inflating")
        }
    }

    override fun available(): Int = myAvailable

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        if (myAvailable <= 0) {
            return -1
        }
        var readLen = if (len > myAvailable) myAvailable else len
        var offset = off
        var toFill = readLen
        while (toFill > 0) {
            if (myOutBufferLength == 0) {
                fillOutBuffer()
            }
            if (myOutBufferLength == 0) {
                readLen -= toFill
                break
            }
            val ready = if (toFill < myOutBufferLength) toFill else myOutBufferLength
            if (b != null) {
                System.arraycopy(myOutBuffer, myOutBufferOffset, b, offset, ready)
            }
            offset += ready
            myOutBufferOffset += ready
            toFill -= ready
            myOutBufferLength -= ready
        }
        if (readLen > 0) {
            myAvailable -= readLen
        } else {
            myAvailable = 0
        }
        return readLen
    }

    override fun read(): Int {
        if (myAvailable <= 0) {
            return -1
        }
        if (myOutBufferLength == 0) {
            fillOutBuffer()
        }
        if (myOutBufferLength == 0) {
            myAvailable = 0
            return -1
        }
        --myAvailable
        --myOutBufferLength
        return myOutBuffer[myOutBufferOffset++].toInt()
    }

    private fun fillOutBuffer() {
        if (myInflatorId == -1) {
            return
        }
        while (myOutBufferLength == 0) {
            if (myInBufferLength == 0) {
                myInBufferOffset = 0
                val toRead = if (myCompressedAvailable < IN_BUFFER_SIZE) myCompressedAvailable else IN_BUFFER_SIZE
                myInBufferLength = myStream.read(myInBuffer, 0, toRead)
                if (myInBufferLength < toRead) {
                    myCompressedAvailable = 0
                } else {
                    myCompressedAvailable -= toRead
                }
            }
            if (myInBufferLength <= 0) {
                break
            }
            val result = inflate(myInflatorId, myInBuffer, myInBufferOffset, myInBufferLength, myOutBuffer)
            if (result <= 0) {
                val extraInfo = StringBuilder()
                    .append(myStream.offset()).append(":")
                    .append(myInBufferOffset).append(":")
                    .append(myInBufferLength).append(":")
                    .append(myOutBuffer.size).append(":")
                for (i in 0 until minOf(10, myInBufferLength)) {
                    extraInfo.append(myInBuffer[myInBufferOffset + i]).append(",")
                }
                throw ZipException("Cannot inflate zip-compressed block, code = " + result + ";extra info = " + extraInfo)
            }
            val inCount = (result shr 16).toInt() and 0xFFFF
            if (inCount > myInBufferLength) {
                throw ZipException("Invalid inflating result, code = " + result + "; buffer length = " + myInBufferLength)
            }
            val outCount = result.toInt() and 0xFFFF
            myInBufferOffset += inCount
            myInBufferLength -= inCount
            myOutBufferOffset = 0
            myOutBufferLength = outCount
            if ((result and (1L shl 32)) != 0L) {
                endInflating(myInflatorId)
                myInflatorId = -1
                myStream.backSkip(myInBufferLength)
                break
            }
        }
    }

    private external fun startInflating(): Int

    private external fun endInflating(inflatorId: Int)

    private external fun inflate(inflatorId: Int, bytes: ByteArray, inOffset: Int, inLength: Int, out: ByteArray): Long

    companion object {
        private const val IN_BUFFER_SIZE = 2048
        private const val OUT_BUFFER_SIZE = 32768

        init {
            System.loadLibrary("DeflatingDecompressor-v3")
        }
    }
}
