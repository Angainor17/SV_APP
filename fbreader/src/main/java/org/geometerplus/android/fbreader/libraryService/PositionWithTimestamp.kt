/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.libraryService

import android.os.Parcel
import android.os.Parcelable

import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition

class PositionWithTimestamp private constructor(
    @JvmField val ParagraphIndex: Int,
    @JvmField val ElementIndex: Int,
    @JvmField val CharIndex: Int,
    @JvmField val Timestamp: Long
) : Parcelable {

    constructor(pos: ZLTextPosition) : this(
        pos.getParagraphIndex(),
        pos.getElementIndex(),
        pos.getCharIndex(),
        if (pos is ZLTextFixedPosition.WithTimestamp) pos.Timestamp else -1L
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ParagraphIndex)
        parcel.writeInt(ElementIndex)
        parcel.writeInt(CharIndex)
        parcel.writeLong(Timestamp)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PositionWithTimestamp> =
            object : Parcelable.Creator<PositionWithTimestamp> {
                override fun createFromParcel(parcel: Parcel): PositionWithTimestamp =
                    PositionWithTimestamp(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readLong())

                override fun newArray(size: Int): Array<PositionWithTimestamp?> = arrayOfNulls(size)
            }
    }
}
