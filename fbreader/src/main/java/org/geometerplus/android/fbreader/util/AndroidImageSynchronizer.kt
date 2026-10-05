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

package org.geometerplus.android.fbreader.util

import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.formatPlugin.CoverReader
import org.geometerplus.fbreader.formats.ExternalFormatPlugin
import org.geometerplus.fbreader.formats.PluginImage
import org.geometerplus.zlibrary.core.image.ZLImageManager
import org.geometerplus.zlibrary.core.image.ZLImageProxy
import org.geometerplus.zlibrary.core.image.ZLImageSimpleProxy
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage
import java.util.HashMap
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AndroidImageSynchronizer private constructor(private val myContext: Context) :
    ZLImageProxy.Synchronizer {

    private val myConnections = HashMap<ExternalFormatPlugin, Connection>()

    constructor(activity: Activity) : this(activity as Context)
    constructor(service: Service) : this(service as Context)

    override fun startImageLoading(image: ZLImageProxy, postAction: Runnable?) {
        val manager = ZLImageManager.Instance() as ZLAndroidImageManager
        manager.startImageLoading(this, image, postAction)
    }

    override fun synchronize(image: ZLImageProxy, postAction: Runnable?) {
        if (image.isSynchronized) {
            // TODO: also check if image is under synchronization
            postAction?.run()
        } else if (image is ZLImageSimpleProxy) {
            image.synchronize()
            postAction?.run()
        } else if (image is PluginImage) {
            val connection = getConnection(image.Plugin)
            connection.runOrAddAction {
                try {
                    image.setRealImage(
                        ZLBitmapImage(
                            connection.Reader!!.readBitmap(
                                image.File.getPath(), Integer.MAX_VALUE, Integer.MAX_VALUE
                            )
                        )
                    )
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
                postAction?.run()
            }
        } else {
            throw RuntimeException("Cannot synchronize " + image.javaClass)
        }
    }

    @Synchronized
    fun clear() {
        for (connection in myConnections.values) {
            myContext.unbindService(connection)
        }
        myConnections.clear()
    }

    @Synchronized
    private fun getConnection(plugin: ExternalFormatPlugin): Connection {
        var connection = myConnections[plugin]
        if (connection == null) {
            connection = Connection(plugin)
            myConnections[plugin] = connection
            myContext.bindService(
                Intent(FBReaderIntents.Action.PLUGIN_CONNECT_COVER_SERVICE)
                    .setPackage(plugin.packageName()),
                connection,
                Context.BIND_AUTO_CREATE,
            )
        }
        return connection
    }

    private class Connection(private val myPlugin: ExternalFormatPlugin) : ServiceConnection {
        private val myExecutor: ExecutorService = Executors.newSingleThreadExecutor()
        private val myPostActions = LinkedList<Runnable>()

        @Volatile
        var Reader: CoverReader? = null

        @Synchronized
        fun runOrAddAction(action: Runnable) {
            if (Reader != null) {
                myExecutor.execute(action)
            } else {
                myPostActions.add(action)
            }
        }

        @Synchronized
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            Reader = CoverReader.Stub.asInterface(binder)
            for (action in myPostActions) {
                myExecutor.execute(action)
            }
            myPostActions.clear()
        }

        @Synchronized
        override fun onServiceDisconnected(className: ComponentName) {
            Reader = null
        }
    }
}
