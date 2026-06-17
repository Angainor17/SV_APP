/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.httpd

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import org.geometerplus.android.fbreader.util.AndroidImageSynchronizer
import java.io.IOException

class DataService : Service() {

    val imageSynchronizer = AndroidImageSynchronizer(this)
    private var myServer: DataServer? = null
    @Volatile
    private var myPort = -1

    override fun onCreate() {
        Thread {
            for (port in 12000 until 12500) {
                try {
                    myServer = DataServer(this@DataService, port)
                    myServer!!.start()
                    myPort = port
                    break
                } catch (e: IOException) {
                    myServer = null
                }
            }
        }.start()
    }

    override fun onDestroy() {
        myServer?.let {
            Thread {
                it.stop()
                myServer = null
            }.start()
        }
        imageSynchronizer.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return object : DataInterface.Stub() {
            override fun getPort(): Int = myPort
        }
    }

    class Connection : ServiceConnection {
        private var myDataInterface: DataInterface? = null

        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            myDataInterface = DataInterface.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            myDataInterface = null
        }

        fun getPort(): Int {
            return try {
                myDataInterface?.port ?: -1
            } catch (e: RemoteException) {
                -1
            }
        }
    }
}
