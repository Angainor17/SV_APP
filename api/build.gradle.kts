import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    namespace = "su.sv.api"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("11")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {

    // Модули-utils
    implementation(project(":commonarchitecture"))

    // Logging
    implementation(libs.timber)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Network
    implementation(libs.bundles.retrofit)
}
