/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.ImageDocumentLoader
import com.pspdfkit.ui.PdfActivity

/**
 * This example shows how to open a demo image document from the assets folder.
 */
class ImageDocumentExample(context: Context) : AssetExample(context, R.string.imageDocumentExampleTitle, R.string.imageDocumentExampleDescription) {

    override val assetPath: String
        get() = "images/android.png"

    override fun launchExample(
        context: Context,
        configuration: PdfActivityConfiguration.Builder
    ) {
        // We start off with the filename (or path) of an image document inside the app's assets.
        val assetPath = assetPath

        // Get the default image document configuration.
        // Default options in this configuration are specifically thought to
        // enhance the user experience for image documents (e.g. thumbnail bar and
        // page number overlay are hidden).
        val imageDocumentConfiguration = ImageDocumentLoader.getDefaultImageDocumentActivityConfiguration(configuration.build())

        // Since PSPDFKit does not directly read documents from the assets, we extract them
        // to the internal device storage using a custom AsyncTask implementation.
        ExtractAssetTask.extract(assetPath, title, context) { documentFile ->
            // Now, as the image document file is sitting in the internal device storage, we can
            // start the PdfActivity activity by passing it the Uri of the file.
            PdfActivity.showImage(
                context,
                Uri.fromFile(documentFile),
                imageDocumentConfiguration
            )
        }
    }
}
