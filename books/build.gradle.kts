plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

apply(
    from = "${project.rootDir}/gradle/android_feature_commons.kts"
)

android {
    namespace = "su.sv.books"
}

dependencies {

    // Модули-фичи
    implementation(project(":reader"))

    // Модули-utils
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))

    // Compose
    implementation(libs.bundles.coil)
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Network
    implementation(libs.bundles.retrofit)
    implementation(libs.retrofit2.kotlin.coroutines.adapter)
}
