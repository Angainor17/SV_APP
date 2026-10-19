package com.github.axet.bookreader.screens.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.github.axet.bookreader.widgets.FBReaderView
import timber.log.Timber

/**
 * Приёмник изменений уровня батареи
 */
@Composable
internal fun BatteryReceiver(fbReaderView: FBReaderView?) {
    val context = LocalContext.current

    DisposableEffect(fbReaderView) {
        if (fbReaderView == null) return@DisposableEffect onDispose {}

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                fbReaderView.battery = level * 100 / scale
                fbReaderView.invalidateFooter()
            }
        }

        val batteryIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        }

        receiver.onReceive(context, batteryIntent ?: return@DisposableEffect onDispose {
            context.unregisterReceiver(receiver)
        })

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Timber.e(e, "Failed to unregister battery receiver")
            }
        }
    }
}
