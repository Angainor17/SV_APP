package com.github.axet.bookreader.domain

/**
 * Очищает текст закладки от лишних символов
 * Удаляет управляющие символы, символы переноса и специальные маркеры FBReader
 */
fun cleanBookmarkText(text: String): String {
    return text
        // Удаляем маркер переноса слов FBReader (U+FFFE = 65534, U+FFFF = 65535)
        .replace(Regex("[\\uFFFE\\uFFFF]"), "")
        // Удаляем управляющие символы (0x00-0x1F кроме пробелов)
        .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
        // Удаляем символы переноса строки и возврата каретки
        .replace("\r\n", " ")
        .replace("\n", " ")
        .replace("\r", " ")
        // Удаляем специальные маркеры FBReader (в квадратных скобках)
        .replace(Regex("\\[image]"), "")
        .replace(Regex("\\[\\d+]"), "") // числовые маркеры типа [1], [2]
        // Удаляем лишние пробелы
        .trim()
        .replace(Regex("  +"), " ") // два и более пробелов → один
}