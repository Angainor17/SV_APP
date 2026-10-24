import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

apply(
    from = "${project.rootDir}/android_feature_commons.kts"
)

android {
    namespace = "su.sv.wiki"
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("11")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {

    // Модули-utils
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))
    implementation(project(":featureflags"))

    // Модули-фичи
    implementation(project(":shpargalki")) // шпаргалки (доступность — через Remote Config)

    // Compose
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material.icons.extended)

    // Image loading
    implementation(libs.bundles.coil)

    // Navigation
    implementation(libs.modo.compose)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // API
    implementation(libs.bundles.retrofit)

    // Logging
    implementation(libs.timber)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // ========== Test ==========
    testImplementation(libs.bundles.test)
}
