package org.geometerplus.zlibrary.core.util

/**
 * class Color. Color is presented as the triple of short's (Red, Green, Blue components)
 * Each component should be in the range 0..255
 */
class ZLColor {
    @JvmField
    val Red: Short
    @JvmField
    val Green: Short
    @JvmField
    val Blue: Short

    constructor(r: Int, g: Int, b: Int) {
        Red = (r and 0xFF).toShort()
        Green = (g and 0xFF).toShort()
        Blue = (b and 0xFF).toShort()
    }

    constructor(intValue: Int) {
        Red = ((intValue shr 16) and 0xFF).toShort()
        Green = ((intValue shr 8) and 0xFF).toShort()
        Blue = (intValue and 0xFF).toShort()
    }

    fun intValue(): Int = (Red.toInt() shl 16) + (Green.toInt() shl 8) + Blue.toInt()

    override fun equals(o: Any?): Boolean {
        if (o === this) {
            return true
        }

        if (o !is ZLColor) {
            return false
        }

        return o.Red == Red && o.Green == Green && o.Blue == Blue
    }

    override fun hashCode(): Int = intValue()

    override fun toString(): String = "ZLColor($Red, $Green, $Blue)"
}
