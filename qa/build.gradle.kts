plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

apply(
    from = "${project.rootDir}/android_feature_commons.kts"
)

android {
    namespace = "su.sv.qa"
}

dependencies {

    // Модули-utils
    implementation(project(":commonarchitecture"))

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // API
    implementation(libs.bundles.retrofit)

    // Logging
    implementation(libs.timber)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
}
