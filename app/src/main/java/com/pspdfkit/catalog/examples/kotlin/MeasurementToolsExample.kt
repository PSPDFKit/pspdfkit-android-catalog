/*
 *   Copyright © 2022-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.kotlin.activities.MeasurementToolsActivity
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.page.PageLayoutMode
import com.pspdfkit.configuration.sharing.ShareFeatures
import com.pspdfkit.preferences.PSPDFKitPreferences
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.special_mode.controller.AnnotationTool

/**
 * Measurement Tools example.
 * This example showcases the measurement tools introduced in Nutrient for Android 8.4.
 * You can read more about the measurement tools in our [guides](https://nutrient.io/guides/android/measurements/)
 *  and [Announcement blog post](https://nutrient.io/blog/2022/announcing-pspdfkit-measurement-tools).
 */
class MeasurementToolsExample(context: Context) :
    SdkExample(context, R.string.measurementToolsExampleTitle, R.string.measurementToolsExampleDescription) {

    companion object {
        const val MEASUREMENTS_PDF = "Measurements.pdf"
    }

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Using this flag to show the ways of changing the defaults without affecting this example.
        val changeDefaultConfiguration = false

        // Here are some ways in which you can change the default settings of the measurement tools:
        if (changeDefaultConfiguration) {
            configuration.apply {
                // Measurement tools are enabled by default if they are licensed. You can turn them off in the configuration if you prefer:
                setMeasurementToolsEnabled(false)

                // Measurement tools use the Android magnifier to help create more accurate measurements. You can turn off this feature here:
                enableMagnifier(false)
            }

            // Measurement tools have a feature where you can snap the drawing to the vector graphics on the page. It is on by default,
            // but you can turn it off by default using the PSPDFKitPreferences singleton:
            PSPDFKitPreferences.get(context).isMeasurementSnappingEnabled = false
        }

        // Disable some features that are not relevant to the example.
        configuration
            // Turn off saving, so we have the clean original document every time the example is launched.
            .autosaveEnabled(false)
            .layoutMode(PageLayoutMode.SINGLE)
            .documentInfoViewEnabled(false)
            .searchEnabled(false)
            .contentEditingEnabled(false)
            .setEnabledShareFeatures(ShareFeatures.none())
            .printingEnabled(false)
            .thumbnailGridEnabled(false)
            .setRedactionUiEnabled(false)
            // We only enable the measurement tools for this example.
            .enabledAnnotationTools(
                listOf(
                    AnnotationTool.MEASUREMENT_DISTANCE,
                    AnnotationTool.MEASUREMENT_PERIMETER,
                    AnnotationTool.MEASUREMENT_AREA_POLYGON,
                    AnnotationTool.MEASUREMENT_AREA_ELLIPSE,
                    AnnotationTool.MEASUREMENT_AREA_RECT,
                    AnnotationTool.MEASUREMENT_SCALE_CALIBRATION
                )
            )

        ExtractAssetTask.extract(MEASUREMENTS_PDF, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(MeasurementToolsActivity::class)
                .build()

            // Start the MeasurementToolsActivity for the extracted document.
            context.startActivity(intent)
        }
    }
}
