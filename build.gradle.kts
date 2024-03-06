
/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath(libs.androidGradle.core)
        classpath(libs.kotlin.gradle)
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()

        // The PSPDFKit library is loaded from the PSPDFKit Maven repository, added by this configuration.
        maven(url = "https://customers.pspdfkit.com/maven/")
    }
}
