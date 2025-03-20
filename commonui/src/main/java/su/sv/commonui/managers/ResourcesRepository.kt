package su.sv.commonui.managers

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.IntegerRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class ResourcesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getLanguage(): String = Locale.getDefault().language

    fun getTimeZone(): String = TimeZone.getDefault().id

    fun getString(@StringRes id: Int): String = context.getString(id)

    fun getStringArray(@ArrayRes id: Int): Array<String> = context.resources.getStringArray(id)

    fun getString(@StringRes id: Int, vararg formatArgs: Any): String =
        context.getString(id, *formatArgs)

    fun getQuantityString(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any): String {
        return context.resources.getQuantityString(
            id,
            quantity,
            *formatArgs,
        )
    }

    fun getInteger(@IntegerRes id: Int): Int = context.resources.getInteger(id)

    fun getDimensionPixelSize(resId: Int): Int = context.resources.getDimensionPixelSize(resId)

    fun getCacheDir(): File = context.cacheDir
}
