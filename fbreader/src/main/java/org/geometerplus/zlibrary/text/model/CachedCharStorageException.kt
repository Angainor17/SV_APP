package org.geometerplus.zlibrary.text.model

class CachedCharStorageException : RuntimeException {
    companion object {
        private const val serialVersionUID: Long = -6373408730045821053L
    }

    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
