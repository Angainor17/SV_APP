import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.tracer)
}

android {
    namespace = "su.sv.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "su.sv.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    ksp(libs.hilt.android.compiler)

    implementation(libs.threetenabp)
}
