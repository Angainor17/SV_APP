package org.geometerplus.zlibrary.core.util

import android.util.Xml
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.xml.sax.helpers.DefaultHandler

object XmlUtil {
    @JvmStatic
    fun parseQuietly(file: ZLFile, handler: DefaultHandler): Boolean {
        return try {
            Xml.parse(file.getInputStream(), Xml.Encoding.UTF_8, handler)
            true
        } catch (e: Exception) {
            false
        }
    }
}
