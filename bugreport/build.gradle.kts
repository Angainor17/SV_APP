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
    namespace = "su.sv.bugreport"
    buildFeatures {
        compose = true
    }
}

dependencies {

    // Модули-utils
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))

    // Compose
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Material Icons Extended (для BugReport иконки)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.modo.compose)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Logging
    implementation(libs.timber)

    // Tracer
    implementation(platform(libs.tracer.platform))
    implementation(libs.tracer.crash.report)

    // WorkManager for background upload
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // OkHttp for Imgbb API
    implementation(libs.okhttp)
}