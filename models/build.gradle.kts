plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
}

apply(
    from = "${project.rootDir}/android_feature_commons.kts"
)

android {
    namespace = "su.sv.models"
}

dependencies {
    // Compose runtime для @Immutable аннотации
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.runtime:runtime")
}