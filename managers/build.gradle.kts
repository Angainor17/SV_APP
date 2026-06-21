plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

apply(
    from = "${project.rootDir}/android_feature_commons.kts"
)

android {
    namespace = "su.sv.managers"
}

dependencies {

    // Модули-фичи
    implementation(project(":api"))

    // Модули-common
    implementation(project(":commonarchitecture"))

    implementation(libs.androidx.core.ktx)

    // Logging
    implementation(libs.timber)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
}