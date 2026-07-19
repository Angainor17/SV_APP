import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
        versionCode = 11
        versionName = "0.3.1"

        testInstrumentationRunner = "su.sv.app.testing.HiltTestRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

tracer {
    create("defaultConfig") {
        pluginToken = "BCXLwm9rvlBMAZtYNZpqdM2CF5DKvuylMBxxTHfsg780"
        appToken = "c3ChjlLDndDpMt068bSjI0NBsHfHLYxU7IkePlEkhi1"

        uploadMapping = true
        uploadNativeSymbols = true
//        additionalLibrariesPath = "$projectDir/aVeryNonstandardLibsDirectory"
    }
    create("debug") {
        pluginToken = "BCXLwm9rvlBMAZtYNZpqdM2CF5DKvuylMBxxTHfsg780"
        appToken = "c3ChjlLDndDpMt068bSjI0NBsHfHLYxU7IkePlEkhi1"

        uploadMapping = true
    }
    create("release") {
        pluginToken = "BCXLwm9rvlBMAZtYNZpqdM2CF5DKvuylMBxxTHfsg780"
        appToken = "c3ChjlLDndDpMt068bSjI0NBsHfHLYxU7IkePlEkhi1"

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
