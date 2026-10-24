package su.sv.featureflags

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.rustore.sdk.remoteconfig.AppId
import ru.rustore.sdk.remoteconfig.AppVersion
import ru.rustore.sdk.remoteconfig.RemoteConfig
import ru.rustore.sdk.remoteconfig.RemoteConfigClient
import ru.rustore.sdk.remoteconfig.RemoteConfigClientBuilder
import ru.rustore.sdk.remoteconfig.RemoteConfigClientEventListener
import ru.rustore.sdk.remoteconfig.RemoteConfigException
import ru.rustore.sdk.remoteconfig.UpdateBehaviour
import timber.log.Timber
import kotlin.time.Duration.Companion.minutes

/**
 * Флаги фич, управляемые через RuStore Remote Config.
 *
 * По умолчанию всё выключено: фича активируется, только когда сервер явно
 * вернёт `true`. Неудачная загрузка конфига поэтому безопасна — фичи остаются
 * скрытыми.
 */
data class Flags(
    val shpargalkiEnabled: Boolean = false,
    val quizEnabled: Boolean = false,
)

/**
 * Единая точка доступа к Remote Config.
 *
 * Инициализируется из `su.sv.app.SvApp` (модуль `:app`) — туда приходит `appId`
 * из BuildConfig. Сам `appId` не секрет и нужен только на этапе инициализации;
 * чтение флагов идёт через [RemoteConfigClient.instance].
 */
object FeatureFlags {

    private const val KEY_SHPARGALKI = "shpargalki_enabled"
    private const val KEY_QUIZ = "quiz_enabled"

    private const val UPDATE_INTERVAL_MINUTES = 15

    private val _flags = MutableStateFlow(Flags())
    val flags: StateFlow<Flags> = _flags.asStateFlow()

    /**
     * Инициализирует SDK и запускает первую загрузку конфига.
     * Безопасно вызывать один раз из `Application.onCreate()`.
     */
    fun init(context: Context, appId: String, appVersion: String) {
        if (appId.isBlank()) {
            Timber.d("FeatureFlags: appId пуст — Remote Config отключён")
            return
        }
        runCatching {
            RemoteConfigClientBuilder(
                appId = AppId(appId),
                context = context.applicationContext,
            )
                .setAppVersion(AppVersion(appVersion))
                .setUpdateBehaviour(UpdateBehaviour.Default(UPDATE_INTERVAL_MINUTES.minutes))
                .setRemoteConfigClientEventListener(EventListener())
                .build()
                .init()
                .addOnSuccessListener { refresh() }
                .addOnFailureListener { Timber.w(it, "FeatureFlags init failed") }
        }.onFailure { Timber.w(it, "FeatureFlags init exception") }
    }

    /** Перечитывает конфиг и обновляет [flags]. */
    fun refresh() {
        runCatching {
            RemoteConfigClient.instance.getRemoteConfig()
                .addOnSuccessListener { config -> _flags.value = config.toFlags() }
                .addOnFailureListener { Timber.w(it, "FeatureFlags refresh failed") }
        }.onFailure { Timber.w(it, "FeatureFlags refresh exception") }
    }

    private fun RemoteConfig.toFlags(): Flags =
        Flags(
            shpargalkiEnabled = booleanOrFalse(KEY_SHPARGALKI),
            quizEnabled = booleanOrFalse(KEY_QUIZ),
        )

    private fun RemoteConfig.booleanOrFalse(key: String): Boolean =
        runCatching { containsKey(key) && getBoolean(key) }.getOrDefault(false)

    private class EventListener : RemoteConfigClientEventListener {
        override fun initComplete() = Unit

        override fun firstLoadComplete() = FeatureFlags.refresh()

        override fun persistentStorageUpdated() = FeatureFlags.refresh()

        override fun memoryCacheUpdated() = Unit

        override fun remoteConfigNetworkRequestFailure(throwable: Throwable) =
            Timber.w(throwable, "FeatureFlags network failure")

        override fun backgroundJobErrors(exception: RemoteConfigException.BackgroundConfigUpdateError) =
            Timber.w(exception, "FeatureFlags background job error")
    }
}
