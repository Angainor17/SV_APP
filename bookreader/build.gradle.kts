plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

apply(
    from = "${project.rootDir}/android_feature_commons.kts"
)
android {
    namespace = "com.github.axet.bookreader"
    buildFeatures {
        compose = true
    }
    defaultConfig {
        testInstrumentationRunner = "com.github.axet.bookreader.screens.testing.HiltTestRunner"
    }
    packaging {
        jniLibs {
            excludes.add("META-INF/DEPENDENCIES")
        }
        resources {
            excludes.add("META-INF/DEPENDENCIES")
        }
    }
}

dependencies {

    // Модули-фичи
    implementation(project(":managers"))
    implementation(project(":commonui"))
    implementation(project(":commonarchitecture"))
    implementation(project(":fbreader")) {
        exclude("org.apache.httpcomponents", "httpmime")
    }

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Compose
    implementation(libs.bundles.coil)
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation (Modo)
    implementation(libs.modo.compose)

    // DI
    implementation(libs.androidx.hilt.navigation.compose)

    implementation("com.github.axet:android-library:1.35.21") {
        exclude("org.apache.httpcomponents", "httpmime")
    }

    // AndroidX Preference (replaces deprecated android.preference)
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation(libs.axet.djvulibre) {
        exclude("org.apache.httpcomponents", "httpmime")
    }
    implementation(libs.legere.pdfiumandroid) {
        // Тянет более новый kotlin-stdlib, чем текущий Kotlin-компилятор проекта умеет
        // читать (metadata version mismatch) — используем stdlib, разрешённый проектом.
        exclude("org.jetbrains.kotlin", "kotlin-stdlib")
    }
    implementation(libs.axet.k2pdfopt) {
        exclude("org.apache.httpcomponents", "httpmime")
    }
    implementation(libs.axet.wget) {
        exclude("org.apache.httpcomponents", "httpmime")
    } //{ exclude group: 'org.json', module: 'json' }

    // Logging
    implementation(libs.timber)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)

    // ============== Testing ==============
    // UI tests
    androidTestImplementation(libs.bundles.androidTest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    kspAndroidTest(libs.hilt.android.compiler)
}