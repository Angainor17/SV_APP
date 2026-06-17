package org.geometerplus.zlibrary.core.options

import org.geometerplus.zlibrary.core.util.ZLColor

class ZLColorOption(
    group: String,
    optionName: String,
    defaultValue: ZLColor?
) : ZLOption(group, optionName, stringColorValue(defaultValue)) {

    private var myValue: ZLColor? = null
    private var myStringValue: String? = null

    val value: ZLColor?
        get() {
            val stringValue = getConfigValue()
            if (stringValue != myStringValue) {
                myStringValue = stringValue
                try {
                    val intValue = stringValue.toInt()
                    myValue = if (intValue != -1) ZLColor(intValue) else null
                } catch (e: NumberFormatException) {
                }
            }
            return myValue
        }

    fun setValue(value: ZLColor?) {
        if (value == null) {
            return
        }
        myValue = value
        myStringValue = stringColorValue(value)
        setConfigValue(myStringValue!!)
    }

    companion object {
        private fun stringColorValue(color: ZLColor?): String = (color?.intValue() ?: -1).toString()
    }
}
