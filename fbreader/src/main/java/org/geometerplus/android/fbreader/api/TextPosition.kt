/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.api

import android.os.Parcel
import android.os.Parcelable

class TextPosition(
    @JvmField val paragraphIndex: Int,
    @JvmField val elementIndex: Int,
    @JvmField val charIndex: Int
) : ApiObject() {

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<TextPosition> {
            override fun createFromParcel(parcel: Parcel): TextPosition {
                parcel.readInt()
                return TextPosition(parcel.readInt(), parcel.readInt(), parcel.readInt())
            }

            override fun newArray(size: Int): Array<TextPosition?> = arrayOfNulls(size)
        }
    }

    override fun type(): Int = Type.TEXT_POSITION

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeInt(paragraphIndex)
        parcel.writeInt(elementIndex)
        parcel.writeInt(charIndex)
    }
}
