package org.geometerplus.android.fbreader.config

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ConfigService : Service() {
    private var myConfig: ConfigInterface.Stub? = null

    override fun onBind(intent: Intent): IBinder? = myConfig

    override fun onCreate() {
        super.onCreate()
        myConfig = SQLiteConfig(this)
    }

    override fun onDestroy() {
        if (myConfig != null) {
            // TODO: close db
            myConfig = null
        }
        super.onDestroy()
    }
}
