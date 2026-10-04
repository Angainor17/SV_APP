package org.amse.ys.zip

import org.geometerplus.zlibrary.core.util.InputStreamHolder
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.LinkedList
import java.util.TreeMap

class ZipFile(private val myStreamHolder: InputStreamHolder) {

    constructor(fileName: String) : this(object : InputStreamHolder {
        override fun getInputStream(): InputStream? = FileInputStream(fileName)
    })

    constructor(file: File) : this(object : InputStreamHolder {
        override fun getInputStream(): InputStream? = FileInputStream(file)
    })

    private val myFileHeaders = TreeMap<String, LocalFileHeader>(String.CASE_INSENSITIVE_ORDER)
    private val myStoredStreams = LinkedList<MyBufferedInputStream>()
    private var myAllFilesAreRead = false

    fun headers(): Collection<LocalFileHeader> {
        try {
            readAllHeaders()
        } catch (e: IOException) {
        }
        return myFileHeaders.values
    }

    private fun readFileHeader(baseStream: MyBufferedInputStream, fileToFind: String?): Boolean {
        val header = LocalFileHeader()
        header.readFrom(baseStream)
        if (header.Signature != LocalFileHeader.FILE_HEADER_SIGNATURE) {
            return false
        }
        val fileName = header.FileName
        if (fileName != null) {
            myFileHeaders[fileName] = header
            if (fileName.equals(fileToFind, ignoreCase = true)) {
                return true
            }
        }
        if ((header.Flags and 0x08) == 0) {
            baseStream.skip(header.CompressedSize.toLong())
        } else {
            findAndReadDescriptor(baseStream, header)
        }
        return false
    }

    private fun readAllHeaders() {
        if (myAllFilesAreRead) {
            return
        }
        myAllFilesAreRead = true
        val baseStream = getBaseStream()
        baseStream.setPosition(0)
        myFileHeaders.clear()
        try {
            while (baseStream.available() > 0) {
                readFileHeader(baseStream, null)
            }
        } finally {
            storeBaseStream(baseStream)
        }
    }

    private fun findAndReadDescriptor(baseStream: MyBufferedInputStream, header: LocalFileHeader) {
        val decompressor = Decompressor.init(baseStream, header)
        var uncompressedSize = 0
        while (true) {
            val blockSize = decompressor.read(null, 0, 2048)
            if (blockSize <= 0) {
                break
            }
            uncompressedSize += blockSize
        }
        header.UncompressedSize = uncompressedSize
        Decompressor.storeDecompressor(decompressor)
    }

    @Synchronized
    internal fun storeBaseStream(baseStream: MyBufferedInputStream) {
        myStoredStreams.add(baseStream)
    }

    @Synchronized
    internal fun getBaseStream(): MyBufferedInputStream {
        val stored = myStoredStreams.poll()
        if (stored != null) {
            return stored
        }
        return MyBufferedInputStream(myStreamHolder)
    }

    private fun createZipInputStream(header: LocalFileHeader): ZipInputStream =
        ZipInputStream(this, header)

    fun entryExists(entryName: String): Boolean =
        try {
            getHeader(entryName)
            true
        } catch (e: IOException) {
            false
        }

    fun getEntrySize(entryName: String): Int = getHeader(entryName).UncompressedSize

    fun getInputStream(entryName: String): InputStream = createZipInputStream(getHeader(entryName))

    fun getHeader(entryName: String): LocalFileHeader {
        if (myFileHeaders.isNotEmpty()) {
            val header = myFileHeaders[entryName]
            if (header != null) {
                return header
            }
            if (myAllFilesAreRead) {
                throw ZipException("Entry $entryName is not found")
            }
        }
        val baseStream = getBaseStream()
        baseStream.setPosition(0)
        try {
            while (baseStream.available() > 0 && !readFileHeader(baseStream, entryName)) {
            }
            val header = myFileHeaders[entryName]
            if (header != null) {
                return header
            }
        } finally {
            storeBaseStream(baseStream)
        }
        throw ZipException("Entry $entryName is not found")
    }
}
