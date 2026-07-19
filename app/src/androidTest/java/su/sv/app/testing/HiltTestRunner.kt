package su.sv.app.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * TestRunner для Hilt-тестов.
 *
 * Заменяет реальное приложение SvApp на HiltTestApplication,
 * что позволяет использовать моки и тестовые зависимости.
 *
 * Используется автоматически при запуске androidTest.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}