import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

apply(
    from = "${project.rootDir}/android_feature_commons.kts"
)

android {
    namespace = "su.sv.featureflags"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("11")
    }
}

dependencies {

    // RuStore Remote Config (фичефлаги)
    implementation(libs.rustore.remote.config)

    // StateFlow (kotlinx-coroutines не пробрасывается транзитивно из SDK)
    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation(libs.timber)
}
