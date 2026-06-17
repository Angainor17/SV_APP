package org.geometerplus.zlibrary.core.util

object ZLSearchUtil {
    @JvmStatic
    @JvmOverloads
    fun find(text: CharArray, offset: Int, length: Int, pattern: ZLSearchPattern, pos: Int = 0): Result? {
        var pos = pos
        if (pos < 0) {
            pos = 0
        }
        val lower = pattern.lowerCasePattern ?: return null
        val patternLength = lower.size
        val end = offset + length
        val lastStart = end - patternLength
        if (pattern.ignoreCase) {
            val upper = pattern.upperCasePattern ?: return null
            val firstCharLower = lower[0]
            val firstCharUpper = upper[0]
            for (i in offset + pos..lastStart) {
                val current = text[i]
                if (current == firstCharLower || current == firstCharUpper) {
                    var j = 1
                    var k = i + 1
                    while (j < patternLength) {
                        val symbol = text[k]
                        if (symbol == '​') {
                            if (patternLength - j > end - k) {
                                break
                            } else {
                                k++
                                continue
                            }
                        }
                        if (lower[j] != symbol && upper[j] != symbol) {
                            break
                        }
                        ++j
                        k++
                    }
                    if (j == patternLength) {
                        return Result(i - offset, k - i)
                    }
                }
            }
        } else {
            val firstChar = lower[0]
            for (i in offset + pos..lastStart) {
                if (text[i] == firstChar) {
                    var j = 1
                    var k = i + 1
                    while (j < patternLength) {
                        val symbol = text[k]
                        if (symbol == '​') {
                            if (patternLength - j > end - k) {
                                break
                            } else {
                                k++
                                continue
                            }
                        }
                        if (lower[j] != text[k]) {
                            break
                        }
                        ++j
                        k++
                    }
                    if (j >= patternLength) {
                        return Result(i - offset, k - i)
                    }
                }
            }
        }
        return null
    }

    class Result internal constructor(
        @JvmField val Start: Int,
        @JvmField val Length: Int
    )
}
