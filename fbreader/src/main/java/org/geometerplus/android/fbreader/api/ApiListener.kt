/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.api

interface ApiListener {
    companion object {
        const val EVENT_READ_MODE_OPENED = "startReading"
        const val EVENT_READ_MODE_CLOSED = "stopReading"
    }

    fun onEvent(event: Int)
}
