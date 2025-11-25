/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog

import android.app.Application
import android.util.Log

/**
 * Entry point for wiring bug reporting tools. The actual implementation lives in the
 * debug source set; release builds call into this no-op wrapper safely.
 */
object NutrientReporting {

    private const val DEBUG_IMPLEMENTATION = "com.pspdfkit.catalog.DebugNutrientReporting"
    private const val INITIALIZER_METHOD = "initializeBugReporting"

    fun initializeBugReporting(app: Application) {
        if (!BuildConfig.DEBUG) return

        runCatching {
            val impl = Class.forName(DEBUG_IMPLEMENTATION)
            val instance = impl.getDeclaredField("INSTANCE").get(null)
            val initializer = impl.getDeclaredMethod(INITIALIZER_METHOD, Application::class.java)
            initializer.invoke(instance, app)
        }.onFailure { error ->
            Log.w("NutrientReporting", "Failed to initialize debug bug reporting tools", error)
        }
    }
}
