package org.geometerplus.zlibrary.core.util

interface SystemInfo {
    fun tempDirectory(): String?
    fun networkCacheDirectory(): String?
}
