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
import org.geometerplus.zlibrary.core.image.ZLImageProxy
import org.geometerplus.zlibrary.core.image.ZLImageSimpleProxy
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage
import java.util.concurrent.Executors

class AndroidImageSynchronizer(private val myContext: Context) : ZLImageProxy.Synchronizer {

    constructor(activity: Activity) : this(activity as Context)
    constructor(service: Service) : this(service as Context)

    private val myConnections = mutableMapOf<ExternalFormatPlugin, Connection>()

    override fun startImageLoading(image: ZLImageProxy, postAction: Runnable?) {
        val manager = ZLAndroidImageManager.Instance() as ZLAndroidImageManager
        manager.startImageLoading(this, image, postAction)
    }

    override fun synchronize(image: ZLImageProxy, postAction: Runnable?) {
        when {
            image.isSynchronized -> {
                // TODO: also check if image is under synchronization
                postAction?.run()
            }
            image is ZLImageSimpleProxy -> {
                image.synchronize()
                postAction?.run()
            }
            image is PluginImage -> {
                val pluginImage = image
                val connection = getConnection(pluginImage.plugin)
                connection.runOrAddAction {
                    try {
                        connection.reader?.let { r ->
                            pluginImage.setRealImage(ZLBitmapImage(r.readBitmap(pluginImage.file.path, Int.MAX_VALUE, Int.MAX_VALUE)))
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                    postAction?.run()
                }
            }
            else -> throw RuntimeException("Cannot synchronize ${image.javaClass}")
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
                Context.BIND_AUTO_CREATE
            )
        }
        return connection
    }

    private class Connection(private val myPlugin: ExternalFormatPlugin) : ServiceConnection {
        private val myExecutor = Executors.newSingleThreadExecutor()
        private val myPostActions = mutableListOf<Runnable>()
        @Volatile
        var reader: CoverReader? = null

        @Synchronized
        fun runOrAddAction(action: Runnable) {
            if (reader != null) {
                myExecutor.execute(action)
            } else {
                myPostActions.add(action)
            }
        }

        @Synchronized
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            reader = CoverReader.Stub.asInterface(binder)
            for (action in myPostActions) {
                myExecutor.execute(action)
            }
            myPostActions.clear()
        }

        @Synchronized
        override fun onServiceDisconnected(className: ComponentName) {
            reader = null
        }
    }
}
