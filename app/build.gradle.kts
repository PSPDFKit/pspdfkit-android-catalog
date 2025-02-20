/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */



plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pspdfkit.catalog"
    compileSdk = 35

    defaultConfig {
        applicationId = namespace
        minSdk = 26
        targetSdk = compileSdk

        versionName = "2024.9.1"
        versionCode = 141407

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
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }


    lint {
        warningsAsErrors = true
        // The Catalog app is a demo app with only English strings.
        // ObsoleteLintCustomCheck can be enabled again when upgrading `AppCompat` to 1.7
        disable += setOf("MissingTranslation", "ObsoleteLintCustomCheck")
        // This needs to be a separate line for the lint dependency check script to work.
        disable.add("GradleDependency")
        baseline = file("lint-baseline.xml")
    }
}

dependencies {

    // Nutrient is integrated from the Nutrient Maven repository. See the `repositories` block at the beginning
    // of this file, which shows how to set up the repository in your app.
    implementation("com.pspdfkit:pspdfkit:2024.9.1")

    // OCR library + English language pack.
    implementation("com.pspdfkit:pspdfkit-ocr:2024.9.1")
    implementation("com.pspdfkit:pspdfkit-ocr-english:2024.9.1")


    // Androidx
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material")
// Activate this to switch the app to Material 3
//    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.runtime:runtime-rxjava3:1.6.8")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.8")

    // Picasso (image loading for inline galleries).
    implementation("com.squareup.picasso:picasso:2.5.2")

    // Retrofit and Gson required by Instant examples.
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava3:2.9.0")

    // Barcode Scanner
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // Kotlin utils.
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")

    // Junit
    testImplementation("junit:junit:4.13.2")

    // Http logging.
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Compose navigation
    implementation("androidx.navigation:navigation-compose:2.5.3")

    // YouTube player (inline videos).
    implementation(files("libs/YouTubeAndroidPlayerApi.jar"))
}
