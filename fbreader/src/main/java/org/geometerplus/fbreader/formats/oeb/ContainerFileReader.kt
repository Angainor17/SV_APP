package org.geometerplus.fbreader.formats.oeb

import org.geometerplus.zlibrary.core.xml.ZLStringMap
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter

internal class ContainerFileReader : ZLXMLReaderAdapter() {
    private var myRootPath: String? = null

    val rootPath: String?
        get() = myRootPath

    override fun startElementHandler(tag: String, xmlattributes: ZLStringMap): Boolean {
        if ("rootfile".equals(tag, ignoreCase = true)) {
            myRootPath = xmlattributes.getValue("full-path")
            if (myRootPath != null) {
                return true
            }
        }
        return false
    }
}
