plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "org.geometerplus"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    externalNativeBuild {
        ndkBuild {
            path("src/main/jni/Android.mk")// TODO check voronin
        }
    }
    buildFeatures {
        aidl = true
    }
}

dependencies {

    // Модули-фичи
    api(project(":util"))
    api(project(":dragSortListview"))
    api(project(":androidFileChooser"))
    api(project(":superToasts"))
    api(project(":ambilWarna"))

//    api("com.github.axet.fbreader:ambilwarna:1.0") // compile project(":ambilWarna")
//    api( "com.github.axet.fbreader:androidfilechooser:5.0" )// compile project(":androidFileChooser")
//    api( "com.github.axet.fbreader:dragsortlistview:0.6.1") // compile project(":dragSortListview")
//    api( "com.github.axet.fbreader:supertoasts:1.3.4") // compile project(":superToasts")
    api("com.github.axet:pdfparse-lib:1.0")
    api("com.googlecode.json-simple:json-simple:1.1.1") {
        exclude(group = "junit")
    }
    api("org.apache.httpcomponents:httpmime:4.5.2")
    api("com.nanohttpd:nanohttpd:2.1.0")


    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}