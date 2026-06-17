package org.geometerplus.zlibrary.core.util

object ZLArrayUtils {
    @JvmStatic
    fun createCopy(array: BooleanArray, dataSize: Int, newLength: Int): BooleanArray {
        val newArray = BooleanArray(newLength)
        if (dataSize > 0) {
            System.arraycopy(array, 0, newArray, 0, dataSize)
        }
        return newArray
    }

    @JvmStatic
    fun createCopy(array: ByteArray, dataSize: Int, newLength: Int): ByteArray {
        val newArray = ByteArray(newLength)
        if (dataSize > 0) {
            System.arraycopy(array, 0, newArray, 0, dataSize)
        }
        return newArray
    }

    @JvmStatic
    fun createCopy(array: CharArray, dataSize: Int, newLength: Int): CharArray {
        val newArray = CharArray(newLength)
        if (dataSize > 0) {
            System.arraycopy(array, 0, newArray, 0, dataSize)
        }
        return newArray
    }

    @JvmStatic
    fun createCopy(array: IntArray, dataSize: Int, newLength: Int): IntArray {
        val newArray = IntArray(newLength)
        if (dataSize > 0) {
            System.arraycopy(array, 0, newArray, 0, dataSize)
        }
        return newArray
    }

    @JvmStatic
    fun createCopy(array: Array<String?>, dataSize: Int, newLength: Int): Array<String?> {
        val newArray = arrayOfNulls<String>(newLength)
        if (dataSize > 0) {
            System.arraycopy(array, 0, newArray, 0, dataSize)
        }
        return newArray
    }
}
