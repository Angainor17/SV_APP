package com.github.axet.bookreader.widgets

import android.os.Parcel
import android.os.Parcelable
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition

open class ZLTextIndexPosition : ZLTextFixedPosition, Parcelable {
    var end: ZLTextPosition

    constructor(p: ZLTextPosition, e: ZLTextPosition) : super(p) {
        end = ZLTextFixedPosition(e)
    }

    constructor(inParcel: Parcel) : super(read(inParcel)) {
        end = read(inParcel)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(paragraphIndex)
        dest.writeInt(elementIndex)
        dest.writeInt(charIndex)
        dest.writeInt(end.paragraphIndex)
        dest.writeInt(end.elementIndex)
        dest.writeInt(end.charIndex)
    }

    companion object CREATOR : Parcelable.Creator<ZLTextIndexPosition> {
        private fun read(inParcel: Parcel): ZLTextPosition {
            val p = inParcel.readInt()
            val e = inParcel.readInt()
            val c = inParcel.readInt()
            return ZLTextFixedPosition(p, e, c)
        }

        override fun createFromParcel(source: Parcel): ZLTextIndexPosition {
            return ZLTextIndexPosition(source)
        }

        override fun newArray(size: Int): Array<ZLTextIndexPosition?> {
            return arrayOfNulls(size)
        }
    }
}
