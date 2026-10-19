package org.amse.ys.zip

import java.io.IOException

class ZipException @JvmOverloads constructor(
    message: String? = null
) : IOException(message)
