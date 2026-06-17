package org.geometerplus.zlibrary.text.view.style

import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import org.geometerplus.zlibrary.core.util.XmlUtil
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

class ZLTextStyleCollection(val screen: String) {
    private val descriptionList: List<ZLTextNGStyleDescription>
    private val descriptionMap = arrayOfNulls<ZLTextNGStyleDescription>(256)
    private var _baseStyle: ZLTextBaseStyle? = null

    init {
        val descriptions = SimpleCSSReader().read(ZLResourceFile.createResourceFile("default/styles.css"))
        descriptionList = ArrayList(descriptions.values).toList()
        for ((key, value) in descriptions) {
            descriptionMap[key.toInt() and 0xFF] = value
        }
        XmlUtil.parseQuietly(
            ZLResourceFile.createResourceFile("default/styles.xml"),
            TextStyleReader()
        )
    }

    fun getBaseStyle(): ZLTextBaseStyle = _baseStyle!!

    fun getDescriptionList(): List<ZLTextNGStyleDescription> = descriptionList

    fun getDescription(kind: Byte): ZLTextNGStyleDescription? = descriptionMap[kind.toInt() and 0xFF]

    private inner class TextStyleReader : DefaultHandler() {
        private fun intValue(attributes: Attributes, name: String, defaultValue: Int): Int {
            val value = attributes.getValue(name)
            return try {
                value?.toInt() ?: defaultValue
            } catch (e: NumberFormatException) {
                defaultValue
            }
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            if ("base" == localName && screen == attributes.getValue("screen")) {
                _baseStyle = ZLTextBaseStyle(
                    screen,
                    attributes.getValue("family"),
                    intValue(attributes, "fontSize", 0)
                )
            }
        }
    }
}
