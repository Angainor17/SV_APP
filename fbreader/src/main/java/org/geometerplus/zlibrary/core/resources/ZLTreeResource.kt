package org.geometerplus.zlibrary.core.resources

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import org.geometerplus.zlibrary.core.language.Language
import org.geometerplus.zlibrary.core.util.XmlUtil
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.Locale

internal class ZLTreeResource private constructor(name: String, value: String?) : ZLResource(name) {
    private var myHasValue = false
    private var myValue: String? = null
    private var myChildren: HashMap<String, ZLTreeResource>? = null
    private var myConditionalValues: LinkedHashMap<Condition, String>? = null

    init {
        setValue(value)
    }

    override fun hasValue(): Boolean = myHasValue

    override fun getValue(): String {
        updateLanguage()
        return if (myHasValue) myValue!! else ZLMissingResource.Value
    }

    private fun setValue(value: String?) {
        myHasValue = value != null
        myValue = value
    }

    override fun getValue(number: Int): String {
        updateLanguage()
        val conditionalValues = myConditionalValues
        if (conditionalValues != null) {
            for (entry in conditionalValues.entries) {
                if (entry.key.accepts(number)) {
                    return entry.value
                }
            }
        }
        return if (myHasValue) myValue!! else ZLMissingResource.Value
    }

    override fun getResource(key: String): ZLResource {
        val children = myChildren
        if (children != null) {
            val child = children[key]
            if (child != null) {
                return child
            }
        }
        return ZLMissingResource.Instance
    }

    private interface Condition {
        fun accepts(number: Int): Boolean
    }

    private class ValueCondition(private val myValue: Int) : Condition {
        override fun accepts(number: Int): Boolean = myValue == number
    }

    private class RangeCondition(private val myMin: Int, private val myMax: Int) : Condition {
        override fun accepts(number: Int): Boolean = myMin <= number && number <= myMax
    }

    private class ModRangeCondition(private val myMin: Int, private val myMax: Int, private val myBase: Int) : Condition {
        override fun accepts(number: Int): Boolean {
            val n = number % myBase
            return myMin <= n && n <= myMax
        }
    }

    private class ModCondition(private val myMod: Int, private val myBase: Int) : Condition {
        override fun accepts(number: Int): Boolean = number % myBase == myMod
    }

    private class ResourceTreeReader : DefaultHandler() {
        private val myStack = ArrayList<ZLTreeResource>()

        fun readDocument(root: ZLTreeResource, file: ZLFile) {
            myStack.clear()
            myStack.add(root)
            XmlUtil.parseQuietly(file, this)
        }

        @Throws(SAXException::class)
        override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
            val stack = myStack
            if (stack.isNotEmpty() && NODE == localName) {
                val name = attributes.getValue("name")
                val condition = attributes.getValue("condition")
                val value = attributes.getValue("value")
                val peek = stack[stack.size - 1]
                if (name != null) {
                    var node: ZLTreeResource?
                    var children = peek.myChildren
                    if (children == null) {
                        node = null
                        children = HashMap()
                        peek.myChildren = children
                    } else {
                        node = children[name]
                    }
                    if (node == null) {
                        node = ZLTreeResource(name, value)
                        children[name] = node
                    } else {
                        if (value != null) {
                            node.setValue(value)
                            node.myConditionalValues = null
                        }
                    }
                    stack.add(node)
                } else if (condition != null && value != null) {
                    val compiled = parseCondition(condition)
                    if (compiled != null) {
                        if (peek.myConditionalValues == null) {
                            peek.myConditionalValues = LinkedHashMap()
                        }
                        peek.myConditionalValues!![compiled] = value
                    }
                    stack.add(peek)
                }
            }
        }

        @Throws(SAXException::class)
        override fun endElement(uri: String?, localName: String?, qName: String?) {
            val stack = myStack
            if (stack.isNotEmpty() && NODE == localName) {
                stack.removeAt(stack.size - 1)
            }
        }
    }

    companion object {
        private const val NODE = "node"
        private val ourLock = Any()

        @Volatile
        var ourRoot: ZLTreeResource? = null

        private var ourTimeStamp = 0L
        private var ourLanguage: String? = null
        private var ourCountry: String? = null

        fun buildTree() {
            synchronized(ourLock) {
                if (ourRoot == null) {
                    ourRoot = ZLTreeResource("", null)
                    ourLanguage = "en"
                    ourCountry = "UK"
                    loadData()
                }
            }
        }

        private fun setInterfaceLanguage() {
            val custom = ZLResource.getLanguageOption().getValue()
            val language: String?
            val country: String?
            if (Language.SYSTEM_CODE == custom) {
                val locale = Locale.getDefault()
                language = locale.getLanguage()
                country = locale.getCountry()
            } else {
                val index = custom.indexOf('_')
                if (index == -1) {
                    language = custom
                    country = null
                } else {
                    language = custom.substring(0, index)
                    country = custom.substring(index + 1)
                }
            }
            if ((language != null && language != ourLanguage) ||
                (country != null && country != ourCountry)
            ) {
                ourLanguage = language
                ourCountry = country
                loadData()
            }
        }

        private fun updateLanguage() {
            val timeStamp = System.currentTimeMillis()
            if (timeStamp > ourTimeStamp + 1000) {
                synchronized(ourLock) {
                    if (timeStamp > ourTimeStamp + 1000) {
                        ourTimeStamp = timeStamp
                        setInterfaceLanguage()
                    }
                }
            }
        }

        private fun loadData(reader: ResourceTreeReader, fileName: String) {
            reader.readDocument(ourRoot!!, ZLResourceFile.createResourceFile("resources/zlibrary/$fileName"))
            reader.readDocument(ourRoot!!, ZLResourceFile.createResourceFile("resources/application/$fileName"))
            reader.readDocument(ourRoot!!, ZLResourceFile.createResourceFile("resources/lang.xml"))
            reader.readDocument(ourRoot!!, ZLResourceFile.createResourceFile("resources/application/neutral.xml"))
        }

        private fun loadData() {
            val reader = ResourceTreeReader()
            loadData(reader, ourLanguage + ".xml")
            loadData(reader, ourLanguage + "_" + ourCountry + ".xml")
        }

        private fun parseCondition(description: String): Condition? {
            val parts = description.split(" ")
            try {
                if ("range" == parts[0]) {
                    return RangeCondition(parts[1].toInt(), parts[2].toInt())
                } else if ("mod" == parts[0]) {
                    return ModCondition(parts[1].toInt(), parts[2].toInt())
                } else if ("modrange" == parts[0]) {
                    return ModRangeCondition(parts[1].toInt(), parts[2].toInt(), parts[3].toInt())
                } else if ("value" == parts[0]) {
                    return ValueCondition(parts[1].toInt())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }
}
