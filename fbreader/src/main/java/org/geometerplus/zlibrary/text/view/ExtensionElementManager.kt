package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.text.model.ExtensionEntry

abstract class ExtensionElementManager {
    internal fun getElements(entry: ExtensionEntry): List<ExtensionElement> =
        getElements(entry.Type, entry.Data)

    open fun getElements(type: String, data: Map<String, String>): List<ExtensionElement> = emptyList()
}
