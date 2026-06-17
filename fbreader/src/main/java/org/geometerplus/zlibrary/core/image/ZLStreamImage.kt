package org.geometerplus.zlibrary.core.image

import java.io.InputStream

interface ZLStreamImage : ZLImage {
    fun inputStream(): InputStream?
}
