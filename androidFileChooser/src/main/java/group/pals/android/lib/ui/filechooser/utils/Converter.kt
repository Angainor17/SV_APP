/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils

/**
 * Утилиты преобразования.
 */
object Converter {

    /**
     * Преобразует `size` (в байтах) в строку.
     * Совет из: http://stackoverflow.com/a/5599842/942821
     *
     * @param size размер в байтах.
     * @return например:
     * - 128 B
     * - 1.5 KB
     * - 10 MB
     * - ...
     */
    fun sizeToStr(size: Double): String {
        if (size <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val blockSize = 1024

        var digitGroups = (Math.log10(size) / Math.log10(blockSize.toDouble())).toInt()
        if (digitGroups >= units.size) digitGroups = units.size - 1

        val adjustedSize = size / Math.pow(blockSize.toDouble(), digitGroups.toDouble())

        return String.format(
            if (digitGroups == 0) "%,.0f %s" else "%,.2f %s",
            adjustedSize,
            units[digitGroups]
        )
    }
}
