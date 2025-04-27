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
    namespace = "su.sv.books"

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {

    // Модули-фичи
    implementation(project(":bookreader"))

    // Модули-common
    implementation(project(":models"))

    // Модули-utils
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))

    // Compose
    implementation(libs.bundles.coil)
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)

    // Logging
    implementation(libs.timber)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Navigation
    implementation(libs.modo.compose)

    // Network
    implementation(libs.bundles.retrofit)
    implementation(libs.retrofit2.kotlin.coroutines.adapter)

    // code desugaring (for java.time)
    coreLibraryDesugaring(libs.code.desugaring)
}
