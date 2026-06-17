/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.libraryService

import android.os.Parcel
import android.os.Parcelable

import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition

data class PositionWithTimestamp(
    val paragraphIndex: Int,
    val elementIndex: Int,
    val charIndex: Int,
    val timestamp: Long
) : Parcelable {

    constructor(pos: ZLTextPosition) : this(
        pos.paragraphIndex,
        pos.elementIndex,
        pos.charIndex,
        if (pos is ZLTextFixedPosition.WithTimestamp) pos.timestamp else -1
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(paragraphIndex)
        parcel.writeInt(elementIndex)
        parcel.writeInt(charIndex)
        parcel.writeLong(timestamp)
    }

    companion object CREATOR : Parcelable.Creator<PositionWithTimestamp> {
        override fun createFromParcel(parcel: Parcel): PositionWithTimestamp {
            return PositionWithTimestamp(
                parcel.readInt(),
                parcel.readInt(),
                parcel.readInt(),
                parcel.readLong()
            )
        }

        override fun newArray(size: Int): Array<PositionWithTimestamp?> = arrayOfNulls(size)
    }
}
