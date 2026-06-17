package org.geometerplus.zlibrary.core.util

import java.io.IOException
import java.io.InputStream

interface InputStreamHolder {
    @Throws(IOException::class)
    fun getInputStream(): InputStream?
}
