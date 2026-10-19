package su.sv.commonarchitecture.utils

import android.util.Base64

/**
 * Деобфускация строк, закодированных как XOR + Base64.
 *
 * ВАЖНО: это НЕ шифрование и не защита от целенаправленного анализа. Ключ и
 * закодированное значение лежат в том же APK, поэтому против динамического
 * анализа (Frida, дамп памяти, дебаггер) это не помогает. Цель — убрать
 * plaintext API-ключей из исходников и строк APK, чтобы они не светились в
 * grep/strings/декомпиляторе и не ловились секрет-сканерами.
 */
object ApiKeyObfuscator {

    /**
     * Декодирует строку, закодированную XOR с повторяющимся ключом и Base64.
     */
    fun decode(encoded: String, xorKey: String): String {
        val data = Base64.decode(encoded, Base64.NO_WRAP)
        val key = xorKey.toByteArray(Charsets.UTF_8)
        val out = ByteArray(data.size)
        for (i in data.indices) {
            val a = data[i].toInt() and 0xFF
            val b = key[i % key.size].toInt() and 0xFF
            out[i] = (a xor b).toByte()
        }
        return String(out, Charsets.UTF_8)
    }
}
