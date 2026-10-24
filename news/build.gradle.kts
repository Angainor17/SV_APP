import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    namespace = "su.sv.news"
    buildFeatures {
        compose = true
    }
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
    implementation(project(":quiz")) // квизы (доступность — через Remote Config)

    // Модули-utils
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))
    implementation(project(":managers"))
    implementation(project(":featureflags"))

    // Compose
    implementation(libs.bundles.coil)
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.browser)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.modo.compose)

    // Logging
    implementation(libs.timber)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.threetenabp)
}