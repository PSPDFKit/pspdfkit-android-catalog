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
import com.pspdfkit.catalog.examples.kotlin.activities.ConstructionExampleActivity
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfActivityIntentBuilder

class ConstructionExample(context: Context) : AssetExample(
    context,
    R.string.constructionExampleTitle,
    R.string.constructionExampleDescription
) {
    override val assetPath: String = "Floor Plan.pdf"

    override fun prepareConfiguration(configuration: PdfActivityConfiguration.Builder) {
        super.prepareConfiguration(configuration)
        configuration.autosaveEnabled(false)
        // turn off the display of the note icon for annotations with an attached note
        configuration.setAnnotationNoteHintingEnabled(false)
        // Apply a custom theme which overrides the icon for the stamp toolbar button in the annotation creation toolbar
        configuration.theme(R.style.PSPDFCatalog_Theme_ConstructionExample)
        // Turn on measurements. This is on by default if you have it in your license.
        configuration.setMeasurementToolsEnabled(true)
    }

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        prepareConfiguration(configuration)

        // Since PSPDFKit does not directly read documents from the assets, we extract them
        // to the internal device storage using a custom AsyncTask implementation.
        ExtractAssetTask.extract(assetPath, title, context) { documentFile ->
            // Now, as the PDF document file is sitting in the internal device storage, we can
            // start the ConstructionExampleActivity activity by passing it the Uri of the file.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .activityClass(ConstructionExampleActivity::class.java)
                .configuration(configuration.build())
                .build()
            context.startActivity(intent)
        }
    }
}
