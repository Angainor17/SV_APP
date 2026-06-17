/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

object PluginApi {
    const val ACTION_REGISTER = "android.fbreader.action.plugin.REGISTER"
    const val ACTION_RUN = "android.fbreader.action.plugin.RUN"

    abstract class PluginInfo : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val newActions = implementedActions(context)
            if (newActions != null) {
                val bundle = getResultExtras(true)
                var actions = bundle.getParcelableArrayList<ActionInfo>(KEY)
                if (actions == null) {
                    actions = ArrayList()
                }
                actions.addAll(newActions)
                bundle.putParcelableArrayList(KEY, actions)
            }
        }

        protected abstract fun implementedActions(context: Context): List<ActionInfo>?

        companion object {
            const val KEY = "actions"
        }
    }

    abstract class ActionInfo protected constructor(id: Uri) : Parcelable {
        private val myId: String = id.toString()

        protected abstract fun getType(): Int

        fun getId(): Uri = Uri.parse(myId)

        override fun describeContents(): Int = 0

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(getType())
            parcel.writeString(myId)
        }

        companion object {
            protected const val TYPE_MENU_OBSOLETE = 1
            protected const val TYPE_MENU = 2

            @JvmField
            val CREATOR = object : Parcelable.Creator<ActionInfo> {
                override fun createFromParcel(parcel: Parcel): ActionInfo? {
                    return when (parcel.readInt()) {
                        TYPE_MENU_OBSOLETE -> MenuActionInfo(
                            Uri.parse(parcel.readString()),
                            parcel.readString(),
                            Int.MAX_VALUE
                        )
                        TYPE_MENU -> MenuActionInfo(
                            Uri.parse(parcel.readString()),
                            parcel.readString(),
                            parcel.readInt()
                        )
                        else -> null
                    }
                }

                override fun newArray(size: Int): Array<ActionInfo?> = arrayOfNulls(size)
            }
        }
    }

    class MenuActionInfo(
        id: Uri,
        val menuItemName: String?,
        val weight: Int
    ) : ActionInfo(id), Comparable<MenuActionInfo> {

        override fun getType(): Int = TYPE_MENU

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeString(menuItemName)
            parcel.writeInt(weight)
        }

        override fun compareTo(other: MenuActionInfo): Int = weight - other.weight

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<MenuActionInfo> {
                override fun createFromParcel(parcel: Parcel): MenuActionInfo {
                    return MenuActionInfo(
                        Uri.parse(parcel.readString()),
                        parcel.readString(),
                        parcel.readInt()
                    )
                }

                override fun newArray(size: Int): Array<MenuActionInfo?> = arrayOfNulls(size)
            }
        }
    }
}
