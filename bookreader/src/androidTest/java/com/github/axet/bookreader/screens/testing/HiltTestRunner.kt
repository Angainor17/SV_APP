package com.github.axet.bookreader.screens.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * TestRunner для Hilt-тестов в bookreader модуле.
 *
 * Использует стандартный HiltTestApplication.
 * Инициализация FBReader выполняется в тестах через BookReaderInitializer.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}