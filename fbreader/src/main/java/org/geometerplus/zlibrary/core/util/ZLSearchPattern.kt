package org.geometerplus.zlibrary.core.util

class ZLSearchPattern(pattern: String, @JvmField val ignoreCase: Boolean) {

    @JvmField
    val lowerCasePattern: CharArray

    @JvmField
    val upperCasePattern: CharArray?

    init {
        val cleanedPattern = pattern.replace("​", "")
        if (ignoreCase) {
            lowerCasePattern = cleanedPattern.lowercase().toCharArray()
            upperCasePattern = cleanedPattern.uppercase().toCharArray()
        } else {
            lowerCasePattern = cleanedPattern.toCharArray()
            upperCasePattern = null
        }
    }

    fun getLength(): Int = lowerCasePattern.size
}
