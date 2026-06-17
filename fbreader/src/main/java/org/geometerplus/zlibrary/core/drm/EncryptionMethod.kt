package org.geometerplus.zlibrary.core.drm

abstract class EncryptionMethod {
    companion object {
        const val UNSUPPORTED = "unsupported"
        const val EMBEDDING = "embedding"
        const val MARLIN = "marlin"
        const val KINDLE = "kindle"

        @JvmStatic
        fun isSupported(method: String?): Boolean = EMBEDDING == method
    }
}
