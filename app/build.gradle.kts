/*
 *   Copyright © 2019-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */



plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pspdfkit.catalog"
    compileSdk = 36

    defaultConfig {
        applicationId = namespace
        minSdk = 26
        targetSdk = compileSdk

        versionName = "10.9.0"
        versionCode = 145049

        vectorDrawables {
            useSupportLibrary = true
        }

        resValue("string", "NUTRIENT_LICENSE_KEY", "\"LICENSE_KEY_GOES_HERE\"")
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
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
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
    implementation("io.nutrient:nutrient:10.9.0")

    // OCR library + English language pack.
    implementation("io.nutrient:nutrient-ocr:10.9.0")
    implementation("io.nutrient:nutrient-ocr-english:10.9.0")


    // Androidx
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-rxjava3")

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
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // Markwon (Markdown rendering).
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")

    // Socket.IO
    implementation("io.socket:socket.io-client:2.1.1")

    // Json Web Tokens
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
}
