/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.config

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import androidx.core.content.ContextCompat
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.options.Config.NotAvailableException
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedList

class ConfigShadow(private val myContext: Context) : Config(), ServiceConnection {
    private val myDeferredActions = LinkedList<Runnable>()

    private val myReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                setToCache(
                    intent.getStringExtra("group")!!,
                    intent.getStringExtra("name")!!,
                    intent.getStringExtra("value"),
                )
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    @Volatile
    private var myInterface: ConfigInterface? = null

    init {
        myContext.bindService(
            FBReaderIntents.internalIntent(FBReaderIntents.Action.CONFIG_SERVICE),
            this,
            Service.BIND_AUTO_CREATE,
        )
    }

    override fun isInitialized(): Boolean = myInterface != null

    override fun runOnConnect(runnable: Runnable) {
        if (myInterface != null) {
            runnable.run()
        } else {
            synchronized(myDeferredActions) {
                myDeferredActions.add(runnable)
            }
        }
    }

    override fun listGroups(): List<String>? {
        val i = myInterface ?: return emptyList()
        return try {
            i.listGroups()
        } catch (e: RemoteException) {
            emptyList()
        }
    }

    override fun listNames(group: String): List<String>? {
        val i = myInterface ?: return emptyList()
        return try {
            i.listNames(group)
        } catch (e: RemoteException) {
            emptyList()
        }
    }

    override fun removeGroup(name: String) {
        val i = myInterface ?: return
        try {
            i.removeGroup(name)
        } catch (e: RemoteException) {
            // ignore
        }
    }

    override fun getSpecialBooleanValue(name: String, defaultValue: Boolean): Boolean =
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
            .getBoolean(name, defaultValue)

    override fun setSpecialBooleanValue(name: String, value: Boolean) {
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
            .edit().putBoolean(name, value).apply()
    }

    override fun getSpecialStringValue(name: String, defaultValue: String): String? =
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
            .getString(name, defaultValue)

    override fun setSpecialStringValue(name: String, value: String) {
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
            .edit().putString(name, value).apply()
    }

    @Throws(NotAvailableException::class)
    protected override fun getValueInternal(group: String, name: String): String? {
        val i = myInterface ?: throw NotAvailableException("Config is not initialized for $group:$name")
        return try {
            i.getValue(group, name)
        } catch (e: RemoteException) {
            throw NotAvailableException("RemoteException for $group:$name")
        }
    }

    protected override fun setValueInternal(group: String, name: String, value: String) {
        val i = myInterface ?: return
        try {
            i.setValue(group, name, value)
        } catch (e: RemoteException) {
            // ignore
        }
    }

    protected override fun unsetValueInternal(group: String, name: String) {
        val i = myInterface ?: return
        try {
            i.unsetValue(group, name)
        } catch (e: RemoteException) {
            // ignore
        }
    }

    @Throws(NotAvailableException::class)
    protected override fun requestAllValuesForGroupInternal(group: String): Map<String, String>? {
        val i = myInterface ?: throw NotAvailableException("Config is not initialized for $group")
        try {
            val values = HashMap<String, String>()
            for (pair in i.requestAllValuesForGroup(group)) {
                val split = pair.split("\u0000")
                when (split.size) {
                    1 -> values[split[0]] = ""
                    2 -> values[split[0]] = split[1]
                }
            }
            return values
        } catch (e: RemoteException) {
            throw NotAvailableException("RemoteException for $group")
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        synchronized(this) {
            myInterface = ConfigInterface.Stub.asInterface(service)
            val filter = IntentFilter(FBReaderIntents.Event.CONFIG_OPTION_CHANGE)
            ContextCompat.registerReceiver(myContext, myReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }

        val actions: List<Runnable> = synchronized(myDeferredActions) {
            val result = ArrayList(myDeferredActions)
            myDeferredActions.clear()
            result
        }
        for (a in actions) {
            a.run()
        }
    }

    @Synchronized
    override fun onServiceDisconnected(name: ComponentName) {
        myContext.unregisterReceiver(myReceiver)
    }
}
