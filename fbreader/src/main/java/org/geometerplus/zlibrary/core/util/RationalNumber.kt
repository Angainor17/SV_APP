package org.geometerplus.zlibrary.core.util

class RationalNumber private constructor(
    numerator: Long,
    denominator: Long
) {
    val numerator: Long
    val denominator: Long

    init {
        var numerator = numerator
        var denominator = denominator
        val gcd = gcd(numerator, denominator)
        if (gcd > 1) {
            numerator /= gcd
            denominator /= gcd
        }
        if (denominator < 0) {
            numerator = -numerator
            denominator = -denominator
        }
        this.numerator = numerator
        this.denominator = denominator
    }

    fun toFloat(): Float = 1.0f * numerator / denominator

    private fun gcd(a: Long, b: Long): Long {
        var a = a
        var b = b
        if (a < 0) {
            a = -a
        }
        if (b < 0) {
            b = -b
        }
        while (a != 0L && b != 0L) {
            if (a > b) {
                a = a % b
            } else {
                b = b % a
            }
        }
        return a + b
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is RationalNumber) {
            return false
        }
        return other.numerator == numerator && other.denominator == denominator
    }

    override fun hashCode(): Int = (37 * numerator + denominator).toInt()

    companion object {
        @JvmStatic
        fun create(numerator: Long, denominator: Long): RationalNumber? {
            if (denominator == 0L) {
                return null
            }
            return RationalNumber(numerator, denominator)
        }
    }
}
