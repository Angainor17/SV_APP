package org.geometerplus.zlibrary.core.options

class ZLBooleanOption @JvmOverloads constructor(
    group: String,
    optionName: String,
    private val myDefaultValue: Boolean
) : ZLOption(group, optionName, if (myDefaultValue) "true" else "false") {

    var value: Boolean
        get() {
            return if (mySpecialName != null && Config.instance?.isInitialized() == false) {
                Config.instance?.getSpecialBooleanValue(mySpecialName!!, myDefaultValue) ?: myDefaultValue
            } else {
                "true" == getConfigValue()
            }
        }
        set(newValue) {
            if (mySpecialName != null) {
                Config.instance?.setSpecialBooleanValue(mySpecialName!!, newValue)
            }
            setConfigValue(if (newValue) "true" else "false")
        }

    override fun saveSpecialValue() {
        if (mySpecialName != null && Config.instance?.isInitialized() == true) {
            Config.instance?.setSpecialBooleanValue(mySpecialName!!, value)
        }
    }
}
