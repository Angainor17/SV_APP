package org.geometerplus.zlibrary.core.options

import java.util.Collections

abstract class Config protected constructor() {
    private val myNullString = "__NULL__"
    private val myCache: MutableMap<StringPair, String> = Collections.synchronizedMap(HashMap())
    private val myCachedGroups = HashSet<String>()

    companion object {
        private var ourInstance: Config? = null

        @JvmStatic
        val instance: Config?
            get() = ourInstance

        @JvmStatic
        fun Instance(): Config? = ourInstance
    }

    init {
        ourInstance = this
    }

    open fun getValue(id: StringPair, defaultValue: String?): String? {
        var value = myCache[id]
        if (value == null) {
            if (myCachedGroups.contains(id.group)) {
                value = myNullString
            } else {
                try {
                    value = getValueInternal(id.group, id.name)
                } catch (e: NotAvailableException) {
                    return defaultValue
                }
                if (value == null) {
                    value = myNullString
                }
            }
            myCache[id] = value
        }
        return if (value != myNullString) value else defaultValue
    }

    open fun setValue(id: StringPair, value: String?) {
        val oldValue = myCache[id]
        if (oldValue != null && oldValue == value) {
            return
        }
        myCache[id] = value ?: myNullString
        setValueInternal(id.group, id.name, value)
    }

    fun requestAllValuesForGroup(group: String) {
        synchronized(myCachedGroups) {
            if (myCachedGroups.contains(group)) {
                return
            }
            val values: Map<String, String>
            try {
                values = requestAllValuesForGroupInternal(group)
            } catch (e: NotAvailableException) {
                return
            }
            for ((key, value) in values) {
                setToCache(group, key, value)
            }
            myCachedGroups.add(group)
        }
    }

    open fun unsetValue(id: StringPair) {
        myCache[id] = myNullString
        unsetValueInternal(id.group, id.name)
    }

    fun setToCache(group: String, name: String, value: String?) {
        myCache[StringPair(group, name)] = value ?: myNullString
    }

    abstract fun isInitialized(): Boolean

    abstract fun runOnConnect(runnable: Runnable)

    abstract fun listGroups(): List<String>

    abstract fun listNames(group: String): List<String>

    abstract fun removeGroup(name: String)

    abstract fun getSpecialBooleanValue(name: String, defaultValue: Boolean): Boolean

    abstract fun setSpecialBooleanValue(name: String, value: Boolean)

    abstract fun getSpecialStringValue(name: String, defaultValue: String?): String?

    abstract fun setSpecialStringValue(name: String, value: String?)

    @Throws(NotAvailableException::class)
    protected abstract fun getValueInternal(group: String, name: String): String?

    protected abstract fun setValueInternal(group: String, name: String, value: String?)

    protected abstract fun unsetValueInternal(group: String, name: String)

    @Throws(NotAvailableException::class)
    protected abstract fun requestAllValuesForGroupInternal(group: String): Map<String, String>

    protected class NotAvailableException(message: String) : Exception(message)
}
