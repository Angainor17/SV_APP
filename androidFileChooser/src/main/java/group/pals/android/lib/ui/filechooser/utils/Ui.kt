/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils

import android.content.Context
import android.os.IBinder
import android.view.inputmethod.InputMethodManager

/**
 * UI утилиты.
 *
 * @author Hai Bison
 */
object Ui {

    /**
     * Скрывает мягкую клавиатуру.
     *
     * @param context [Context]
     * @param iBinder [IBinder]
     */
    fun hideSoftKeyboard(context: Context, iBinder: IBinder?) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(iBinder, 0)
    }
}
