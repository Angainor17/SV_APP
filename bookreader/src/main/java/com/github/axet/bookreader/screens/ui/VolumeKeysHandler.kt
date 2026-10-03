package com.github.axet.bookreader.screens.ui

import android.app.Activity
import android.view.KeyEvent
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager
import com.github.axet.bookreader.app.ReaderPreferences
import com.github.axet.bookreader.screens.viewmodel.ReaderViewModel
import com.github.axet.bookreader.widgets.FBReaderView
import org.geometerplus.fbreader.fbreader.ActionCode

/**
 * Обработчик клавиш громкости для навигации по страницам.
 *
 * Использует Window.Callback.dispatchKeyEvent вместо View.OnKeyListener,
 * чтобы перехватывать события громкости ДО того, как они попадут в иерархию View.
 * Это надёжнее, потому что:
 * - View.OnKeyListener требует, чтобы view имела фокус
 * - PagerWidget (дочерняя view FBReaderView) имеет isFocusable=true и забирает фокус
 * - Window.Callback перехватывает события независимо от фокуса
 */
@Composable
internal fun VolumeKeysHandler(
    fbReaderView: FBReaderView?,
    viewModel: ReaderViewModel
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity } ?: return

    val volumeKeysEnabled = remember {
        val shared = PreferenceManager.getDefaultSharedPreferences(context)
        shared.getBoolean(ReaderPreferences.PREFERENCE_VOLUME_KEYS, false)
    }

    DisposableEffect(fbReaderView, volumeKeysEnabled, activity) {
        if (fbReaderView == null || !volumeKeysEnabled) {
            return@DisposableEffect onDispose {}
        }

        val originalCallback = activity.window.callback
        val wrapper = object : Window.Callback by originalCallback {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (viewModel.volumeKeysEnabled) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_VOLUME_DOWN if event.action == KeyEvent.ACTION_DOWN -> {
                            fbReaderView.app?.runAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD)
                            return true
                        }

                        KeyEvent.KEYCODE_VOLUME_UP if event.action == KeyEvent.ACTION_DOWN -> {
                            fbReaderView.app?.runAction(ActionCode.VOLUME_KEY_SCROLL_BACK)
                            return true
                        }
                    }
                }
                return originalCallback.dispatchKeyEvent(event)
            }
        }
        activity.window.callback = wrapper

        onDispose {
            activity.window.callback = originalCallback
        }
    }
}
