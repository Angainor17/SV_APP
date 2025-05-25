plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
apply(
    from = "${project.rootDir}/android_feature_commons.kts"
)

android {
    namespace = "su.sv.api"
}

dependencies {

    // Модули-utils
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)

    // Compose
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))

    // Logging
    implementation(libs.timber)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Network
    implementation(libs.bundles.retrofit)
}
