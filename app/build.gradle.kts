import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.tracer)
    id("kotlin-parcelize")
}

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

        // Локализация: только русский
        androidResources.localeFilters.add("ru")
    }
    buildTypes {
        release {
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
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("11")
    }
}

// Обфускация токенов tracer (XOR + Base64), чтобы не хранить plaintext в VCS.
// Полной защиты нет (ключ в том же файле), но сырые токены не лежат в репо.
fun decodeTracerToken(encoded: String, xorKey: String): String {
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

tracer {
    create("defaultConfig") {
        pluginToken = decodeTracerToken("MTUHOAUMWhcEMzIhND0dNz0sLwUWLFEmNGo0JwMSEAI+NCcMJikFFhVoSFw=", "sv_tracer_plugin")
        appToken = decodeTracerToken("EEUcHBgNLyEcOyUAPQdGaUwQMgksQhEjAzgVPhMtCjRULBk6MRw1GB42RQ==", "sv_tracer_app")

        uploadMapping = true
        uploadNativeSymbols = true
//        additionalLibrariesPath = "$projectDir/aVeryNonstandardLibsDirectory"
    }
    create("debug") {
        pluginToken = decodeTracerToken("MTUHOAUMWhcEMzIhND0dNz0sLwUWLFEmNGo0JwMSEAI+NCcMJikFFhVoSFw=", "sv_tracer_plugin")
        appToken = decodeTracerToken("EEUcHBgNLyEcOyUAPQdGaUwQMgksQhEjAzgVPhMtCjRULBk6MRw1GB42RQ==", "sv_tracer_app")

        uploadMapping = true
    }
    create("release") {
        pluginToken = decodeTracerToken("MTUHOAUMWhcEMzIhND0dNz0sLwUWLFEmNGo0JwMSEAI+NCcMJikFFhVoSFw=", "sv_tracer_plugin")
        appToken = decodeTracerToken("EEUcHBgNLyEcOyUAPQdGaUwQMgksQhEjAzgVPhMtCjRULBk6MRw1GB42RQ==", "sv_tracer_app")

        uploadMapping = true
    }
}

dependencies {

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

    // ============== Testing ==============
    // Unit tests
    testImplementation(libs.bundles.test)

    // UI tests
    androidTestImplementation(libs.bundles.androidTest)
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
            "su.sv.app.testing.SmokeTest"
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
            "su.sv.app.testing.ReleaseTest"
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
            "su.sv.app.testing.NavigationTest"
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
