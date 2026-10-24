package su.sv.app.push

import ru.rustore.sdk.pushclient.common.logger.Logger
import timber.log.Timber

/**
 * Логгер RuStore Push SDK поверх Timber, чтобы не тащить в приложение
 * отдельный лог-механизм SDK.
 */
class PushLogger(private val tag: String? = null) : Logger {
    override fun debug(message: String, throwable: Throwable?) =
        if (tag != null) Timber.tag(tag).d(throwable, message) else Timber.d(throwable, message)

    override fun error(message: String, throwable: Throwable?) =
        if (tag != null) Timber.tag(tag).e(throwable, message) else Timber.e(throwable, message)

    override fun info(message: String, throwable: Throwable?) =
        if (tag != null) Timber.tag(tag).i(throwable, message) else Timber.i(throwable, message)

    override fun verbose(message: String, throwable: Throwable?) =
        if (tag != null) Timber.tag(tag).v(throwable, message) else Timber.v(throwable, message)

    override fun warn(message: String, throwable: Throwable?) =
        if (tag != null) Timber.tag(tag).w(throwable, message) else Timber.w(throwable, message)

    override fun createLogger(tag: String): Logger =
        PushLogger(if (this.tag != null) "${this.tag}:$tag" else tag)
}
