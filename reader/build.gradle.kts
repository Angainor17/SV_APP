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
    namespace = "su.sv.reader"

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // Модули-common
    implementation(project(":models"))

    // Модули-utils
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))

    // PDF reader
    implementation(libs.androidpdfviewer)

    // Navigation
    implementation(libs.modo.compose)

    // Compose
    implementation(libs.bundles.compose)
    implementation(libs.androidx.ui.viewbinding)
    implementation(platform(libs.androidx.compose.bom))

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
}