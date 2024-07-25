/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfActivity

/**
 * Opens the [PdfActivity] for viewing a PDF stored within the app's asset folder.
 */
abstract class AssetExample(context: Context, @StringRes titleRes: Int, @StringRes descriptionRes: Int) : SdkExample(context, titleRes, descriptionRes) {
    /**
     * Gets the path to the asset that we want to display.
     * @return The path to the asset
     */
    protected open val assetPath: String
        get() = QUICK_START_GUIDE

    /**
     * Allows subclasses to adjust the configuration before displaying the document.
     * @param configuration The configuration that will be used to display the document.
     */
    protected open fun prepareConfiguration(configuration: PdfActivityConfiguration.Builder) {
        // Turning off autosave allows us to see the original asset each time we start the example.
        configuration.autosaveEnabled(false)
    }

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Let example subclasses make changes to the configuration before launching the example.
        prepareConfiguration(configuration)

        // Extract the document to the Catalog's private files, so that examples can freely modify the file.
        // Since PSPDFKit does not directly read documents from the assets, we extract them
        // to the internal device storage using a custom AsyncTask implementation.
        ExtractAssetTask.extract(assetPath, title, context) { documentFile ->
            // Now, as the documentFile is sitting in the internal device storage, we can
            // start the PdfActivity by passing it the Uri of the file.
            PdfActivity.showDocument(context, Uri.fromFile(documentFile), configuration.build())
        }
    }
}
