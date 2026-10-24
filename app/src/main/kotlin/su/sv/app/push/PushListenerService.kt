package su.sv.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ru.rustore.sdk.pushclient.messaging.exception.RuStorePushClientException
import ru.rustore.sdk.pushclient.messaging.model.RemoteMessage
import ru.rustore.sdk.pushclient.messaging.service.RuStoreMessagingService
import su.sv.app.MainActivity
import su.sv.app.R
import timber.log.Timber

/**
 * Приёмник push-уведомлений RuStore.
 *
 * Точка входа для всех входящих сообщений (поздравления, новости, инфо).
 * Декларируется в манифесте с intent-filter `MESSAGING_EVENT`.
 */
class PushListenerService : RuStoreMessagingService() {
    override fun onNewToken(token: String) {
        Timber.d("onNewToken token=$token")
        // TODO: отправить токен на сервер рассылки, чтобы адресовать сообщения этому устройству.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.d("onMessageReceived id=${message.messageId} from=${message.from}")
        val notification = message.notification ?: return
        showNotification(
            title = notification.title,
            body = notification.body,
            channelId = notification.channelId ?: getString(R.string.notifications_data_push_channel_id),
        )
    }

    override fun onDeletedMessages() {
        Timber.d("onDeletedMessages")
    }

    override fun onError(errors: List<RuStorePushClientException>) {
        errors.forEach { Timber.w(it, "RuStore push error") }
    }

    private fun showNotification(title: String?, body: String?, channelId: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat
                .Builder(this, channelId)
                .setSmallIcon(R.drawable.splash_icon)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        /**
         * Создаёт каналы уведомлений (Android 8+, API 26+). Вызывается из [su.sv.app.SvApp]
         * один раз при старте — до инициализации SDK, т.к. channelId в манифесте
         * (`default_notification_channel_id`) должен существовать к моменту показа уведомления.
         */
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        context.getString(R.string.notifications_notification_push_channel_id),
                        context.getString(R.string.notifications_notification_push_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ),
                    NotificationChannel(
                        context.getString(R.string.notifications_data_push_channel_id),
                        context.getString(R.string.notifications_data_push_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ),
                ),
            )
        }
    }
}
