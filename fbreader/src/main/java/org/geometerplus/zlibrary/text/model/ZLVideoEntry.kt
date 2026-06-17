package org.geometerplus.zlibrary.text.model

class ZLVideoEntry {
    private val mySources: MutableMap<String, String> = HashMap()

    fun addSource(mime: String, path: String) {
        mySources[mime] = path
    }

    fun sources(): Map<String, String> = mySources.toMap()
}
