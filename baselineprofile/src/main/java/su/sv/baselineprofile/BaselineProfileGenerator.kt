package su.sv.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Генерирует Baseline Profile для :app.
 *
 * Профиль предварительно компилирует (AOT) горячие участки кода и ускоряет
 * холодный старт приложения. Сейчас собирается только стартовый путь
 * (launcher-activity + первый кадр). Сюда же можно добавить навигацию по
 * ключевым экранам (каталог книг, читалка) через uiAutomator { ... }.
 *
 * Запуск (нужно подключённое устройство с API 33+ или root):
 *   ./gradlew :app:generateReleaseBaselineProfile
 *
 * Результат: app/src/release/generated/baselineProfiles/baseline-prof.txt
 */
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = "su.sv.app",
        profileBlock = {
            startActivityAndWait()
        },
    )
}
