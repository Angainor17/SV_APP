package org.geometerplus.zlibrary.core.options

abstract class ZLOption protected constructor(
    group: String,
    optionName: String,
    defaultStringValue: String?
) {
    @JvmField val myId: StringPair = StringPair(group, optionName)
    @JvmField var config: ConfigInstance = ConfigInstance()
    protected var myDefaultStringValue: String = defaultStringValue ?: ""
    protected var mySpecialName: String? = null

    fun setSpecialName(specialName: String?) {
        mySpecialName = specialName
    }

    open fun saveSpecialValue() {}

    protected fun getConfigValue(): String {
        val config = config.instance() ?: return myDefaultStringValue
        return config.getValue(myId, myDefaultStringValue) ?: myDefaultStringValue
    }

    protected fun setConfigValue(value: String) {
        val config = config.instance() ?: return
        if (myDefaultStringValue != value) {
            config.setValue(myId, value)
        } else {
            config.unsetValue(myId)
        }
    }

    open class ConfigInstance {
        open fun instance(): Config? = org.geometerplus.zlibrary.core.options.Config.instance
    }
}
