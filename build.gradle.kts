// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// Apply detekt to all subprojects for type resolution
subprojects {
    apply(plugin = "dev.detekt")

    detekt {
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    }
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    source.setFrom(
        files(
            "app/src/main/java",
            "commonarchitecture/src/main/java",
            "commonui/src/main/java",
            "main/src/main/java",
            "books/src/main/java",
            "wiki/src/main/java",
            "news/src/main/java",
            "info/src/main/java",
            "models/src/main/java",
            "api/src/main/java",
            "managers/src/main/java",
        )
    )
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    reports {
        html.required.set(true)
    }
}