package org.amse.ys.zip

/**
 * Class consists of constants, describing a compressed file. Contains only
 * constructor, all fields are final.
 */
class LocalFileHeader {
    @JvmField
    var FileName: String? = null
    @JvmField
    var Signature: Int = 0
    @JvmField
    var Version: Int = 0
    @JvmField
    var Flags: Int = 0
    @JvmField
    var CompressionMethod: Int = 0
    @JvmField
    var ModificationTime: Int = 0
    @JvmField
    var ModificationDate: Int = 0
    @JvmField
    var CRC32: Int = 0
    @JvmField
    var CompressedSize: Int = 0
    @JvmField
    var UncompressedSize: Int = 0
    @JvmField
    var NameLength: Int = 0
    @JvmField
    var ExtraLength: Int = 0
    @JvmField
    var DataOffset: Int = 0

    fun readFrom(stream: MyBufferedInputStream) {
        Signature = stream.read4Bytes()
        when (Signature) {
            END_OF_CENTRAL_DIRECTORY_SIGNATURE -> {
                stream.skip(16L)
                val comment = stream.read2Bytes()
                stream.skip(comment.toLong())
            }
            FOLDER_HEADER_SIGNATURE -> {
                Version = stream.read4Bytes()
                Flags = stream.read2Bytes()
                CompressionMethod = stream.read2Bytes()
                ModificationTime = stream.read2Bytes()
                ModificationDate = stream.read2Bytes()
                CRC32 = stream.read4Bytes()
                CompressedSize = stream.read4Bytes()
                UncompressedSize = stream.read4Bytes()
                if (CompressionMethod == 0 && CompressedSize != UncompressedSize) {
                    CompressedSize = UncompressedSize
                }
                NameLength = stream.read2Bytes()
                ExtraLength = stream.read2Bytes()
                val comment = stream.read2Bytes()
                stream.skip(12L)
                FileName = stream.readString(NameLength)
                stream.skip(ExtraLength.toLong())
                stream.skip(comment.toLong())
            }
            FILE_HEADER_SIGNATURE -> {
                Version = stream.read2Bytes()
                Flags = stream.read2Bytes()
                CompressionMethod = stream.read2Bytes()
                ModificationTime = stream.read2Bytes()
                ModificationDate = stream.read2Bytes()
                CRC32 = stream.read4Bytes()
                CompressedSize = stream.read4Bytes()
                UncompressedSize = stream.read4Bytes()
                if (CompressionMethod == 0 && CompressedSize != UncompressedSize) {
                    CompressedSize = UncompressedSize
                }
                NameLength = stream.read2Bytes()
                ExtraLength = stream.read2Bytes()
                FileName = stream.readString(NameLength)
                stream.skip(ExtraLength.toLong())
            }
            DATA_DESCRIPTOR_SIGNATURE -> {
                CRC32 = stream.read4Bytes()
                CompressedSize = stream.read4Bytes()
                UncompressedSize = stream.read4Bytes()
            }
        }
        DataOffset = stream.offset()
    }

    companion object {
        const val FILE_HEADER_SIGNATURE = 0x04034b50
        const val FOLDER_HEADER_SIGNATURE = 0x02014b50
        const val END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50
        const val DATA_DESCRIPTOR_SIGNATURE = 0x08074b50
    }
}
