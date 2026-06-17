package org.geometerplus.zlibrary.core.options

class ZLIntegerRangeOption(
    group: String,
    optionName: String,
    @JvmField val MinValue: Int,
    @JvmField val MaxValue: Int,
    defaultValue: Int
) : ZLOption(group, optionName, valueInRange(defaultValue, MinValue, MaxValue).toString()) {

    private var myValue = 0
    private var myStringValue: String? = null

    var value: Int
        get() {
            val stringValue = getConfigValue()
            if (stringValue != myStringValue) {
                myStringValue = stringValue
                try {
                    myValue = valueInRange(stringValue.toInt(), MinValue, MaxValue)
                } catch (e: NumberFormatException) {
                }
            }
            return myValue
        }
        set(newValue) {
            val valueInRange = valueInRange(newValue, MinValue, MaxValue)
            myValue = valueInRange
            myStringValue = valueInRange.toString()
            setConfigValue(myStringValue!!)
        }

    companion object {
        private fun valueInRange(value: Int, min: Int, max: Int): Int = Math.min(max, Math.max(min, value))
    }
}
