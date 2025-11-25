/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog

import android.app.Application
import com.pspdfkit.catalog.utils.startFreezeDetectorIfEnabled

/**
 * Application entry point for the Catalog app. Debug-only wiring happens inside
 * [NutrientReporting], which safely no-ops in release builds.
 */
class CatalogApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        NutrientReporting.initializeBugReporting(this)
        startFreezeDetectorIfEnabled()
    }
}
