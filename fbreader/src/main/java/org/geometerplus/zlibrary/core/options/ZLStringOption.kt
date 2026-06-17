package org.geometerplus.zlibrary.core.options

class ZLStringOption(
    group: String,
    optionName: String,
    defaultValue: String?
) : ZLOption(group, optionName, defaultValue) {

    var value: String
        get() {
            return if (mySpecialName != null && Config.instance?.isInitialized() == false) {
                Config.instance?.getSpecialStringValue(mySpecialName!!, myDefaultStringValue) ?: myDefaultStringValue
            } else {
                getConfigValue()
            }
        }
        set(newValue) {
            if (mySpecialName != null) {
                Config.instance?.setSpecialStringValue(mySpecialName!!, newValue)
            }
            setConfigValue(newValue)
        }

    override fun saveSpecialValue() {
        if (mySpecialName != null && Config.instance?.isInitialized() == true) {
            Config.instance?.setSpecialStringValue(mySpecialName!!, value)
        }
    }
}
