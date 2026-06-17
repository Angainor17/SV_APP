package org.geometerplus.zlibrary.core.options

class ZLIntegerOption(
    group: String,
    optionName: String,
    defaultValue: Int
) : ZLOption(group, optionName, defaultValue.toString()) {

    private var myValue = 0
    private var myStringValue: String? = null

    var value: Int
        get() {
            val stringValue = getConfigValue()
            if (stringValue != myStringValue) {
                myStringValue = stringValue
                try {
                    myValue = stringValue.toInt()
                } catch (e: NumberFormatException) {
                }
            }
            return myValue
        }
        set(value) {
            myValue = value
            myStringValue = value.toString()
            setConfigValue(myStringValue!!)
        }
}
