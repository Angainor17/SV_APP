import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.tracer)
    id("kotlin-parcelize")
    alias(libs.plugins.androidx.baselineprofile)
}

// Единый источник секретов (keystore, tracer-токены): gradle-свойство → env → local.properties.
// Секреты НЕ хранятся в VCS: local.properties / .env — в .gitignore.
// Объявлено до android {}, т.к. signingConfigs вызывает secret() при конфигурации.
val localProps: Properties =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

fun secret(name: String): String? =
    (project.findProperty(name) as? String)
        ?: System.getenv(name)
        ?: localProps.getProperty(name)

android {
    namespace = "su.sv.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "su.sv.app"
        minSdk = 24
        targetSdk = 37
        // Для нового релиза поднять versionCode и versionName
        versionCode = 15
        versionName = "0.3.5"

        testInstrumentationRunner = "su.sv.app.testing.HiltTestRunner"

        // RuStore push: project_id для SDK (НЕ секрет — вшивается в APK, виден в манифесте).
        // Пустое значение → инициализация push в SvApp пропускается.
        buildConfigField(
            "String",
            "RUSTORE_PUSH_PROJECT_ID",
            "\"${secret("RUSTORE_PUSH_PROJECT_ID") ?: ""}\"",
        )

        // Локализация: только русский
        androidResources.localeFilters.add("ru")

        // Только живые ABI. armeabi (нет устройств, и в AAR от axet лежит лишь
        // часть библиотек — приложение на нём всё равно не запустилось бы) и x86
        // (нет устройств, 32-битный эмулятор не используется) — мёртвый вес в APK.
        // x86_64 оставлен для эмулятора на Intel Mac.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }
    signingConfigs {
        // Подпись release берётся из local.properties / env / gradle-свойств (см. secret()),
        // чтобы keystore и пароли не попадали в VCS. Пока keystore не задан — release unsigned.
        create("release") {
            storeFile = secret("RELEASE_STORE_FILE")?.let { rootProject.file(it) }
            storePassword = secret("RELEASE_STORE_PASSWORD")
            keyAlias = secret("RELEASE_KEY_ALIAS")
            keyPassword = secret("RELEASE_KEY_PASSWORD")
        }
    }
    val releaseSigning = signingConfigs.getByName("release")
    buildTypes {
        release {
            // Подписываем release только если keystore реально задан.
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            // Отдельная версия для разработки: другой package id, имя и иконка,
            // чтобы на одном устройстве можно было держать и прод, и дев.
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Свободное время dev")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        // resValue("string", "app_name", …) в debug buildType
        resValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("11")
    }
}

// Токены tracer приходят закодированными (XOR + Base64); здесь декодируются.
fun decodeTracerToken(
    encoded: String,
    xorKey: String,
): String {
    val data = Base64.getDecoder().decode(encoded)
    val key = xorKey.toByteArray(Charsets.UTF_8)
    val out = ByteArray(data.size)
    for (i in data.indices) {
        val a = data[i].toInt() and 0xFF
        val b = key[i % key.size].toInt() and 0xFF
        out[i] = (a xor b).toByte()
    }
    return String(out, Charsets.UTF_8)
}

// Декодирует tracer-токен из секрета; при отсутствии — пустая строка (отчёты не шлются).
fun tracerToken(
    tokenKey: String,
    xorKeyKey: String,
): String {
    val encoded = secret(tokenKey) ?: return ""
    val xorKey = secret(xorKeyKey) ?: return ""
    return decodeTracerToken(encoded, xorKey)
}

tracer {
    create("defaultConfig") {
        pluginToken = tracerToken("TRACER_PLUGIN_TOKEN", "TRACER_PLUGIN_TOKEN_XORKEY")
        appToken = tracerToken("TRACER_APP_TOKEN", "TRACER_APP_TOKEN_XORKEY")

        uploadMapping = true
        uploadNativeSymbols = true
//        additionalLibrariesPath = "$projectDir/aVeryNonstandardLibsDirectory"
    }
    create("debug") {
        pluginToken = tracerToken("TRACER_PLUGIN_TOKEN", "TRACER_PLUGIN_TOKEN_XORKEY")
        appToken = tracerToken("TRACER_APP_TOKEN", "TRACER_APP_TOKEN_XORKEY")

        uploadMapping = true
    }
    create("release") {
        pluginToken = tracerToken("TRACER_PLUGIN_TOKEN", "TRACER_PLUGIN_TOKEN_XORKEY")
        appToken = tracerToken("TRACER_APP_TOKEN", "TRACER_APP_TOKEN_XORKEY")

        uploadMapping = true
    }
}

// ru.ok.tracer 1.5.x регистрирует CollectSymbolsTask (generateTracerNativeSymbols*),
// который обращается к Task.project во время выполнения — это несовместимо с
// configuration cache (org.gradle.configuration-cache=true). Помечаем таск как
// несовместимый: сборка не падает, а config cache отключается только на время
// выполнения этого таска.
tasks.matching { it.name.startsWith("generateTracerNativeSymbols") }.configureEach {
    notCompatibleWithConfigurationCache(
        "ru.ok.tracer CollectSymbolsTask uses Task.project at execution time",
    )
}

dependencies {
    // Baseline Profiles: ускоряют холодный старт (AOT-компиляция горячего кода ART).
    // Профиль генерируется модулем :baselineprofile → ./gradlew :app:generateReleaseBaselineProfile
    baselineProfile(project(":baselineprofile"))

    // Модули-фичи
    implementation(project(":main"))

    // читалка
    implementation(project(":bookreader"))
    implementation("com.github.axet:android-library:1.35.21") {
        exclude("org.apache.httpcomponents", "httpmime")
    }

    // Модули-utils
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))
    implementation(project(":managers"))

    // Logging
    implementation(libs.timber)

    // Tracer
    implementation(platform(libs.tracer.platform))
    // Сбор и анализ крешей и ANR
    implementation(libs.tracer.crash.report)
    // Сбор и анализ нативных крешей
    implementation(libs.tracer.crash.report.native)
    // 0.1.40 тащит bundled libz.so (4 КБ alignment → 16 KB page size); 0.1.41-android её убрала.
    implementation(libs.tracer.native.bindings)
    // Сбор и анализ хипдапмов при OOM
    implementation(libs.tracer.heap.dumps)
    // Анализ потребления дискового места на устройстве
    implementation(libs.tracer.disk.usage)

    // Compose
    implementation(libs.bundles.coil)
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))

    // Navigation
    implementation(libs.modo.compose)

    // DI
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)

    implementation(libs.threetenabp)

    // RuStore push-уведомления
    implementation(libs.rustore.push.client)

    // ============== Testing ==============
    // Unit tests
    testImplementation(libs.bundles.test)

    // UI tests
    androidTestImplementation(libs.bundles.androidTest)
    androidTestImplementation(libs.hilt.android)
    androidTestImplementation(libs.hilt.core)
    androidTestImplementation(libs.dagger)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)
    kspAndroidTest(libs.hilt.android.compiler)
}

// =====================================================
// UI Test Tasks
// =====================================================

/**
 * Запуск smoke-тестов (критичные сценарии)
 * ./gradlew runSmokeTests
 */
tasks.register("runSmokeTests") {
    group = "verification"
    description = "Run smoke UI tests (critical scenarios)"

    dependsOn("connectedAndroidTest")

    doFirst {
        project.extensions.extraProperties.set(
            "android.testInstrumentationRunnerArguments.annotation",
            "su.sv.app.testing.SmokeTest",
        )
    }
}

/**
 * Запуск release-тестов (перед релизом)
 * ./gradlew runReleaseTests
 */
tasks.register("runReleaseTests") {
    group = "verification"
    description = "Run release UI tests (before release)"

    dependsOn("connectedAndroidTest")

    doFirst {
        project.extensions.extraProperties.set(
            "android.testInstrumentationRunnerArguments.annotation",
            "su.sv.app.testing.ReleaseTest",
        )
    }
}

/**
 * Запуск тестов навигации
 * ./gradlew runNavigationTests
 */
tasks.register("runNavigationTests") {
    group = "verification"
    description = "Run navigation UI tests"

    dependsOn("connectedAndroidTest")

    doFirst {
        project.extensions.extraProperties.set(
            "android.testInstrumentationRunnerArguments.annotation",
            "su.sv.app.testing.NavigationTest",
        )
    }
}

/**
 * Полный релизный пайплайн:
 * - Сборка release APK
 * - Запуск smoke-тестов
 * ./gradlew releasePipeline
 */
tasks.register("releasePipeline") {
    group = "build"
    description = "Full release pipeline: build release APK and run smoke tests"

    dependsOn("assembleRelease")
    dependsOn("runSmokeTests")

    doLast {
        println("\n✅ Release pipeline completed!")
        println("📱 APK: app/build/outputs/apk/release/app-release.apk")
    }
}
