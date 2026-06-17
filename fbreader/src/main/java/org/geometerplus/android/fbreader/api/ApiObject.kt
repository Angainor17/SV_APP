/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.api

import android.os.Parcel
import android.os.Parcelable

abstract class ApiObject : Parcelable {

    protected abstract fun type(): Int

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type())
    }

    class Void private constructor() : ApiObject() {
        override fun type(): Int = Type.VOID

        companion object {
            val Instance = Void()
        }
    }

    class Integer(val value: Int) : ApiObject() {
        override fun type(): Int = Type.INT

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeInt(value)
        }
    }

    class Long(val value: kotlin.Long) : ApiObject() {
        override fun type(): Int = Type.LONG

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeLong(value)
        }
    }

    class Float(val value: kotlin.Float) : ApiObject() {
        override fun type(): Int = Type.FLOAT

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeFloat(value)
        }
    }

    class Boolean(val value: kotlin.Boolean) : ApiObject() {
        override fun type(): Int = Type.BOOLEAN

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeByte(if (value) 1.toByte() else 0.toByte())
        }
    }

    class Date(val value: java.util.Date) : ApiObject() {
        override fun type(): Int = Type.DATE

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeLong(value.time)
        }
    }

    class String(val value: kotlin.String) : ApiObject() {
        override fun type(): Int = Type.STRING

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeString(value)
        }
    }

    class Serializable(val value: java.io.Serializable) : ApiObject() {
        override fun type(): Int = Type.SERIALIZABLE

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeSerializable(value)
        }
    }

    class Parcelable(val value: android.os.Parcelable) : ApiObject() {
        override fun type(): Int = Type.PARCELABLE

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeParcelable(value, 0)
        }
    }

    class Error(val message: kotlin.String) : ApiObject() {
        override fun type(): Int = Type.ERROR

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeString(message)
        }
    }

    internal object Type {
        const val ERROR = -1
        const val VOID = 0
        const val INT = 1
        const val STRING = 2
        const val BOOLEAN = 3
        const val DATE = 4
        const val LONG = 5
        const val FLOAT = 6
        const val TEXT_POSITION = 10
        const val SERIALIZABLE = 20
        const val PARCELABLE = 21
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<ApiObject> {
            override fun createFromParcel(parcel: Parcel): ApiObject {
                val code = parcel.readInt()
                return when (code) {
                    Type.ERROR -> Error(parcel.readString() ?: "Unknown error")
                    Type.VOID -> Void.Instance
                    Type.INT -> Integer(parcel.readInt())
                    Type.LONG -> Long(parcel.readLong())
                    Type.FLOAT -> Float(parcel.readFloat())
                    Type.BOOLEAN -> Boolean(parcel.readByte() == 1.toByte())
                    Type.DATE -> Date(java.util.Date(parcel.readLong()))
                    Type.STRING -> String(parcel.readString() ?: "")
                    Type.TEXT_POSITION -> TextPosition(parcel.readInt(), parcel.readInt(), parcel.readInt())
                    Type.SERIALIZABLE -> Serializable(parcel.readSerializable()!!)
                    Type.PARCELABLE -> Parcelable(parcel.readParcelable(null)!!)
                    else -> Error("Unknown object code: $code")
                }
            }

            override fun newArray(size: Int): Array<ApiObject?> = arrayOfNulls(size)
        }

        internal fun envelope(value: Int): ApiObject = Integer(value)
        internal fun envelope(value: kotlin.Long): ApiObject = Long(value)
        internal fun envelope(value: kotlin.Float): ApiObject = Float(value)
        internal fun envelope(value: kotlin.Boolean): ApiObject = Boolean(value)
        internal fun envelope(value: kotlin.String): ApiObject = String(value)
        internal fun envelope(value: java.util.Date): ApiObject = Date(value)
        internal fun envelope(value: android.os.Parcelable): ApiObject = Parcelable(value)

        internal fun envelopeStringList(values: List<kotlin.String>): List<ApiObject> {
            val objects = ArrayList<ApiObject>(values.size)
            for (v in values) {
                objects.add(String(v))
            }
            return objects
        }

        internal fun envelopeSerializableList(values: List<java.io.Serializable>): List<ApiObject> {
            val objects = ArrayList<ApiObject>(values.size)
            for (v in values) {
                objects.add(Serializable(v))
            }
            return objects
        }

        internal fun envelopeIntegerList(values: List<kotlin.Int>): List<ApiObject> {
            val objects = ArrayList<ApiObject>(values.size)
            for (v in values) {
                objects.add(Integer(v))
            }
            return objects
        }
    }
}
