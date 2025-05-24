plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

apply(
    from = "${project.rootDir}/android_feature_commons.kts"
)
android {
    namespace = "com.github.axet.bookreader"
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

    implementation("com.github.axet:android-library:1.35.21") {
        exclude("org.apache.httpcomponents", "httpmime")
    }

    implementation(libs.axet.djvulibre) {
        exclude("org.apache.httpcomponents", "httpmime")
    }
    implementation(libs.axet.pdfium) {
        exclude("org.apache.httpcomponents", "httpmime")
    }
    implementation(libs.axet.k2pdfopt) {
        exclude("org.apache.httpcomponents", "httpmime")
    }
    implementation(libs.axet.wget) {
        exclude("org.apache.httpcomponents", "httpmime")
    } //{ exclude group: 'org.json', module: 'json' }

    implementation(libs.timber)

    implementation(libs.appcompat)
    implementation(libs.material)
}