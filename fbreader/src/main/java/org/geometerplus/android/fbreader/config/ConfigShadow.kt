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
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.zlibrary.core.options.Config

class ConfigShadow(private val myContext: Context) : Config(), ServiceConnection {

    private val myDeferredActions = mutableListOf<Runnable>()
    private val myReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                setToCache(
                    intent.getStringExtra("group") ?: return,
                    intent.getStringExtra("name") ?: return,
                    intent.getStringExtra("value")
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
            Service.BIND_AUTO_CREATE
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

    override fun listGroups(): List<String> {
        if (myInterface == null) {
            return emptyList()
        }
        return try {
            ArrayList(myInterface!!.listGroups())
        } catch (e: RemoteException) {
            emptyList()
        }
    }

    override fun listNames(group: String): List<String> {
        if (myInterface == null) {
            return emptyList()
        }
        return try {
            ArrayList(myInterface!!.listNames(group))
        } catch (e: RemoteException) {
            emptyList()
        }
    }

    override fun removeGroup(name: String) {
        if (myInterface != null) {
            try {
                myInterface!!.removeGroup(name)
            } catch (e: RemoteException) {
            }
        }
    }

    override fun getSpecialBooleanValue(name: String, defaultValue: Boolean): Boolean =
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
            .getBoolean(name, defaultValue)

    override fun setSpecialBooleanValue(name: String, value: Boolean) {
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE).edit()
            .putBoolean(name, value).commit()
    }

    override fun getSpecialStringValue(name: String, defaultValue: String?): String? =
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
            .getString(name, defaultValue)

    override fun setSpecialStringValue(name: String, value: String?) {
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE).edit()
            .putString(name, value).commit()
    }

    override fun getValueInternal(group: String, name: String): String? {
        if (myInterface == null) {
            throw NotAvailableException("Config is not initialized for $group:$name")
        }
        return try {
            myInterface!!.getValue(group, name)
        } catch (e: RemoteException) {
            throw NotAvailableException("RemoteException for $group:$name")
        }
    }

    override fun setValueInternal(group: String, name: String, value: String?) {
        if (myInterface != null) {
            try {
                myInterface!!.setValue(group, name, value)
            } catch (e: RemoteException) {
            }
        }
    }

    override fun unsetValueInternal(group: String, name: String) {
        if (myInterface != null) {
            try {
                myInterface!!.unsetValue(group, name)
            } catch (e: RemoteException) {
            }
        }
    }

    override fun requestAllValuesForGroupInternal(group: String): Map<String, String> {
        if (myInterface == null) {
            throw NotAvailableException("Config is not initialized for $group")
        }
        return try {
            val values = HashMap<String, String>()
            for (pair in myInterface!!.requestAllValuesForGroup(group)) {
                val split = pair.split(" ")
                when (split.size) {
                    1 -> values[split[0]] = ""
                    2 -> values[split[0]] = split[1]
                }
            }
            values
        } catch (e: RemoteException) {
            throw NotAvailableException("RemoteException for $group")
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        synchronized(this) {
            myInterface = ConfigInterface.Stub.asInterface(service)
            myContext.registerReceiver(
                myReceiver, IntentFilter(FBReaderIntents.Event.CONFIG_OPTION_CHANGE)
            )
        }

        val actions: List<Runnable>
        synchronized(myDeferredActions) {
            actions = ArrayList(myDeferredActions)
            myDeferredActions.clear()
        }
        for (a in actions) {
            a.run()
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        synchronized(this) {
            myContext.unregisterReceiver(myReceiver)
        }
    }
}
