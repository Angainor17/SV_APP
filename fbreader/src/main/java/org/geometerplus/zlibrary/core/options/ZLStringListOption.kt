package org.geometerplus.zlibrary.core.options

import org.geometerplus.zlibrary.core.util.MiscUtil
import java.util.Collections

class ZLStringListOption : ZLOption {
    private val myDelimiter: String
    private var myValue: List<String>? = null
    private var myStringValue: String? = null

    constructor(group: String, optionName: String, defaultValue: List<String>?, delimiter: String) : super(
        group, optionName, MiscUtil.join(defaultValue, delimiter)
    ) {
        myDelimiter = delimiter
    }

    constructor(group: String, optionName: String, defaultValue: String?, delimiter: String) : this(
        group, optionName,
        if (defaultValue != null)
            Collections.singletonList(defaultValue)
        else
            emptyList(),
        delimiter
    )

    var value: List<String>
        get() {
            val stringValue = getConfigValue()
            if (stringValue != myStringValue) {
                myStringValue = stringValue
                myValue = MiscUtil.split(stringValue, myDelimiter)
            }
            return myValue ?: emptyList()
        }
        set(newValue) {
            if (newValue == myValue) {
                return
            }
            myValue = ArrayList(newValue)
            myStringValue = MiscUtil.join(newValue, myDelimiter)
            setConfigValue(myStringValue!!)
        }
}
