import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("11")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {

    // Модули-фичи
    implementation(project(":api"))

    // Модули-common
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))

    implementation(libs.androidx.core.ktx)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // JSON
    implementation(libs.gson)

    // Logging
    implementation(libs.timber)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
}