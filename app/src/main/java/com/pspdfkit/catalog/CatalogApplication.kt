/*
 *   Copyright © 2025-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog

import android.app.Application
import com.pspdfkit.catalog.ui.model.PreferenceKeys
import com.pspdfkit.catalog.utils.FreezeDetector
import com.pspdfkit.catalog.utils.dataStore
import com.pspdfkit.preferences.PSPDFKitPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Application entry point for the Catalog app. Debug-only wiring happens inside
 * [NutrientReporting], which safely no-ops in release builds.
 */
class CatalogApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        NutrientReporting.initializeBugReporting(this)
        observeFreezeDetectorPreference()
        observeMemoryTraceLoggingPreference()
    }

    private fun observeFreezeDetectorPreference() {
        applicationScope.launch {
            dataStore.data
                .map { it[PreferenceKeys.FreezeDetectorEnabled] ?: false }
                .distinctUntilChanged()
                .onEach { enabled ->
                    if (enabled) FreezeDetector.start() else FreezeDetector.stop()
                }
                .collect {}
        }
    }

    /**
     * Bridges the catalog's DataStore checkbox to the SDK-global [PSPDFKitPreferences] flag that
     * `MemoryNotificationHandler` reads, so ticking "Enable memory trace logging" turns on the per-poll
     * `Nutri.MemTrace` diagnostic. Mirrors how an integrator would enable it in their own app.
     */
    private fun observeMemoryTraceLoggingPreference() {
        applicationScope.launch {
            dataStore.data
                .map { it[PreferenceKeys.MemoryTraceLoggingEnabled] ?: false }
                .distinctUntilChanged()
                .onEach { enabled ->
                    PSPDFKitPreferences.get(this@CatalogApplication).setMemoryTraceLoggingEnabled(enabled)
                }
                .collect {}
        }
    }
}
