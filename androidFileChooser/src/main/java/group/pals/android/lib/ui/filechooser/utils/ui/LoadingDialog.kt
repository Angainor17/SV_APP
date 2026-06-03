/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils.ui

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.util.Log

/**
 * [AsyncTask], используемый для показа [ProgressDialog] во время выполнения
 * фоновых задач.
 *
 * @author Hai Bison
 * @since v2.1 alpha
 */
@Suppress("DEPRECATION")
open class LoadingDialog(
    context: Context,
    msg: String,
    cancelable: Boolean
) : AsyncTask<Void, Void, Any?>() {

    companion object {
        const val CLASS_NAME = "LoadingDialog"
    }

    private val dialog: ProgressDialog = ProgressDialog(context).apply {
        setMessage(msg)
        isIndeterminate = true
        setCancelable(cancelable)
        if (cancelable) {
            setCanceledOnTouchOutside(true)
            setOnCancelListener {
                cancel(true)
            }
        }
    }

    private var delayTime = 500
    private var finished = false
    private var lastException: Throwable? = null

    /**
     * Создаёт новый [LoadingDialog].
     *
     * @param context [Context]
     * @param msgId ID ресурса сообщения, которое будет показано в диалоге.
     * @param cancelable можно ли отменить диалог.
     */
    constructor(context: Context, msgId: Int, cancelable: Boolean) : this(
        context,
        context.getString(msgId),
        cancelable
    )

    override fun doInBackground(vararg params: Void): Any? {
        return null
    }

    /**
     * Если вы переопределяете этот метод, вы должны вызвать `super.onPreExecute()`
     * в самом начале метода.
     */
    override fun onPreExecute() {
        Handler().postDelayed({
            if (!finished) {
                try {
                    // Иногда активность уже завершена до показа диалога, это вызовет ошибку
                    dialog.show()
                } catch (t: Throwable) {
                    Log.e(CLASS_NAME, "onPreExecute() - show dialog: $t")
                }
            }
        }, delayTime.toLong())
    }

    /**
     * Если вы переопределяете этот метод, вы должны вызвать `super.onPostExecute(result)`
     * в начале метода.
     */
    override fun onPostExecute(result: Any?) {
        doFinish()
    }

    /**
     * Если вы переопределяете этот метод, вы должны вызвать `super.onCancelled()`
     * в начале метода.
     */
    override fun onCancelled() {
        doFinish()
        super.onCancelled()
    }

    private fun doFinish() {
        finished = true
        try {
            // Иногда активность уже завершена до закрытия диалога, это вызовет ошибку
            dialog.dismiss()
        } catch (t: Throwable) {
            Log.e(CLASS_NAME, "doFinish() - dismiss dialog: $t")
        }
    }

    /**
     * Получает время задержки перед показом диалога.
     *
     * @return время задержки.
     */
    fun getDelayTime(): Int = delayTime

    /**
     * Устанавливает время задержки перед показом диалога.
     *
     * @param delayTime время задержки.
     * @return [LoadingDialog]
     */
    fun setDelayTime(delayTime: Int): LoadingDialog {
        this.delayTime = if (delayTime >= 0) delayTime else 0
        return this
    }

    /**
     * Получает последнее исключение.
     *
     * @return [Throwable]
     */
    protected fun getLastException(): Throwable? = lastException

    /**
     * Устанавливает последнее исключение. Этот метод полезен, если исключение
     * возникло внутри [doInBackground].
     *
     * @param t [Throwable]
     */
    protected fun setLastException(t: Throwable?) {
        lastException = t
    }
}
