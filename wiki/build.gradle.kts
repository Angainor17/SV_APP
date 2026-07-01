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

dependencies {

    // Модули-utils
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))

    // Compose
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))

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
}
