/*
 NaturalOrderComparator.kt -- Perform 'natural order' comparisons of strings in Kotlin.
 Copyright (C) 2003 by Pierre-Luc Paour <natorder@paour.com>

 Based on the C version by Martin Pool, of which this is more or less a straight conversion.
 Copyright (C) 2000 by Martin Pool <mbp@humbug.org.au>

 This software is provided 'as-is', without any express or implied
 warranty.  In no event will the authors be held liable for any damages
 arising from the use of this software.

 Permission is granted to anyone to use this software for any purpose,
 including commercial applications, and to alter it and redistribute it
 freely, subject to the following restrictions:

 1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
 2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
 3. This notice may not be removed or altered from any source distribution.
 */
package org.fbreader.util

/**
 * Компаратор для "естественной" сортировки строк.
 *
 * Выполняет сравнение строк в порядке, который кажется естественным для человека.
 * Числа в строках сравниваются как числовые значения, а не посимвольно.
 *
 * ## Использование
 * ```kotlin
 * val comparator = NaturalOrderComparator()
 * val strings = listOf("file10.txt", "file2.txt", "file1.txt")
 * strings.sortedWith(comparator) // ["file1.txt", "file2.txt", "file10.txt"]
 * ```
 *
 * ## Примеры сортировки
 * - Обычная сортировка: `["file1.txt", "file10.txt", "file2.txt"]`
 * - Естественная сортировка: `["file1.txt", "file2.txt", "file10.txt"]`
 *
 * ## Особенности
 * - Числа сравниваются по их числовому значению
 * - Пробелы и ведущие нули учитываются при сравнении
 * - Регистронезависимое сравнение для строк разного регистра
 *
 * @see Comparator
 */
class NaturalOrderComparator : Comparator<String> {

    /**
     * Возвращает символ строки по индексу или 0, если индекс выходит за границы.
     *
     * @param s строка
     * @param i индекс символа
     * @return символ по индексу или 0 (null-символ) если индекс за границами строки
     */
    private fun charAt(s: String, i: Int): Char =
        if (i >= s.length) 0.toChar() else s[i]

    /**
     * Сравнивает числовые части строк справа от текущей позиции.
     *
     * Определяет, какое число больше, сравнивая цифры последовательно.
     * Запоминает "перевес" (bias) для случаев, когда числа одинаковой длины.
     *
     * @param a первая строка (подстрока, начинающаяся с цифр)
     * @param b вторая строка (подстрока, начинающаяся с цифр)
     * @return отрицательное число если a < b, положительное если a > b, 0 если равны
     */
    private fun compareRight(a: String, b: String): Int {
        var bias = 0
        var ia = 0
        var ib = 0

        // Самая длинная последовательность цифр побеждает.
        // Кроме того, побеждает наибольшее значение, но мы не можем
        // узнать это заранее, пока не просканируем оба числа.
        // Поэтому запоминаем это в BIAS.
        while (true) {
            val ca = charAt(a, ia)
            val cb = charAt(b, ib)

            when {
                !ca.isDigit() && !cb.isDigit() -> return bias
                !ca.isDigit() -> return -1
                !cb.isDigit() -> return 1
                ca < cb -> if (bias == 0) bias = -1
                ca > cb -> if (bias == 0) bias = 1
                ca == 0.toChar() && cb == 0.toChar() -> return bias
            }
            ia++
            ib++
        }
    }

    /**
     * Сравнивает две строки в естественном порядке.
     *
     * @param a первая строка для сравнения
     * @param b вторая строка для сравнения
     * @return отрицательное число если a < b, положительное если a > b, 0 если равны
     */
    override fun compare(a: String, b: String): Int {
        // Если строки содержат разные регистры и не равны без учёта регистра,
        // сравниваем в нижнем регистре
        if ((!a.lowercase().equals(a, ignoreCase = true) ||
             !b.lowercase().equals(b, ignoreCase = true)) &&
             !a.equals(b, ignoreCase = true)) {
            return compare(a.lowercase(), b.lowercase())
        }

        var ia = 0
        var ib = 0
        var nza = 0
        var nzb = 0

        while (true) {
            // Считаем количество нулей перед последним сравниваемым числом
            nza = 0
            nzb = 0

            var ca = charAt(a, ia)
            var cb = charAt(b, ib)

            // Пропускаем ведущие пробелы и нули
            while (ca.isWhitespace() || ca == '0') {
                if (ca == '0') {
                    nza++
                } else {
                    // Считаем только последовательные нули
                    nza = 0
                }
                ia++
                ca = charAt(a, ia)
            }

            while (cb.isWhitespace() || cb == '0') {
                if (cb == '0') {
                    nzb++
                } else {
                    // Считаем только последовательные нули
                    nzb = 0
                }
                ib++
                cb = charAt(b, ib)
            }

            // Обрабатываем последовательность цифр
            if (ca.isDigit() && cb.isDigit()) {
                val result = compareRight(a.substring(ia), b.substring(ib))
                if (result != 0) {
                    return result
                }
            }

            if (ca == 0.toChar() && cb == 0.toChar()) {
                // Строки сравниваются одинаково. Возможно, вызывающий код
                // захочет использовать strcmp для разрешения конфликта.
                return nza - nzb
            }

            if (ca < cb) {
                return -1
            } else if (ca > cb) {
                return 1
            }

            ia++
            ib++
        }
    }
}
