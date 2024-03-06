/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */



plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.pspdfkit.catalog"
    compileSdk = libs.versions.targetSdk.get().toInt()

    defaultConfig {
        applicationId = namespace
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        versionName = "2024.1.2"
        versionCode = 138349

        vectorDrawables {
            useSupportLibrary = true
        }

        resValue("string", "PSPDFKIT_LICENSE_KEY", "\"LICENSE_KEY_GOES_HERE\"")
        resValue("string", "YOUTUBE_API_KEY", "\"YOUTUBE_API_KEY_GOES_HERE\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }


    lint {
        warningsAsErrors = true
        // The Catalog app is a demo app with only English strings.
        disable += "MissingTranslation"
        baseline = file("lint-baseline.xml")
    }
}

dependencies {

    // PSPDFKit is integrated from the PSPDFKit Maven repository. See the `repositories` block at the beginning
    // of this file, which shows how to set up the repository in your app.
    implementation("com.pspdfkit:pspdfkit:2024.1.2")

    // OCR library + English language pack.
    implementation("com.pspdfkit:pspdfkit-ocr:2024.1.2")
    implementation("com.pspdfkit:pspdfkit-ocr-english:2024.1.2")


    // Androidx
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle)

    // Compose
    implementation(libs.compose.activity)
    implementation(libs.compose.compiler)
    implementation(libs.compose.material)
    implementation(libs.compose.rxjava3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    androidTestImplementation(libs.compose.ui.test.junit4)

    // Picasso (image loading for inline galleries).
    implementation(libs.squareup.picasso)

    // Retrofit and Gson required by Instant examples.
    implementation(libs.gson)
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.gson)
    implementation(libs.squareup.retrofit.rxjava3)

    // Barcode Scanner
    implementation(libs.gmsBarCodeScanner)

    // Kotlin utils.
    implementation(libs.reactivex.rxKotlin)

    // Junit
    testImplementation(libs.junit)

    // Http logging.
    implementation(libs.squareup.retrofit.okhttp3.logging)

    // YouTube player (inline videos).
    implementation(files("libs/YouTubeAndroidPlayerApi.jar"))
}
