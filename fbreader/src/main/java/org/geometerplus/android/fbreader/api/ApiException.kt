/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.api

class ApiException : Exception {
    constructor(message: String?) : super(message)
    constructor(parent: Exception?) : super(parent)
}
