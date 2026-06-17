package org.geometerplus.zlibrary.core.options

class ZLEnumOption<T : Enum<T>>(
    group: String,
    optionName: String,
    defaultValue: T
) : ZLOption(group, optionName, defaultValue.toString()) {

    private var myValue: T? = null
    private var myStringValue: String? = null
    private val myEnumClass: Class<T> = defaultValue.javaClass.declaringClass as Class<T>

    var value: T?
        get() {
            val stringValue = getConfigValue()
            if (stringValue != myStringValue) {
                myStringValue = stringValue
                try {
                    @Suppress("UNCHECKED_CAST")
                    myValue = java.lang.Enum.valueOf(myEnumClass, stringValue) as T
                } catch (t: Throwable) {
                }
            }
            return myValue
        }
        set(newValue) {
            if (newValue == null) {
                return
            }
            myValue = newValue
            myStringValue = newValue.toString()
            setConfigValue(myStringValue!!)
        }
}
