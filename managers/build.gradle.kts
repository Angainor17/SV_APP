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
    namespace = "su.sv.managers"

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)

    implementation(libs.timber)

    // Compose
    implementation(libs.bundles.coil)
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // code desugaring (for java.time)
    coreLibraryDesugaring(libs.code.desugaring)
}