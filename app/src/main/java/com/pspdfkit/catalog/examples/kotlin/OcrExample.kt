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
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.kotlin.activities.OcrProcessingActivity
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * This examples shows how to use Optical character recognition
 * to extract the text from scanned documents using the [com.pspdfkit.document.processor.PdfProcessor].
 */
class OcrExample(context: Context) : SdkExample(context, R.string.ocrExampleTitle, R.string.ocrExampleDescription) {

    companion object {
        const val OCR_PDF_PATH = "ocr/Remote Work.pdf"
    }

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(OCR_PDF_PATH, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(
                    configuration
                        .documentInfoViewEnabled(false)
                        .annotationListEnabled(false)
                        .bookmarkListEnabled(false)
                        .settingsMenuEnabled(false)
                        .thumbnailGridEnabled(false)
                        .outlineEnabled(false).build()
                )
                .activityClass(OcrProcessingActivity::class)
                .build()

            // Start the OcrProcessingActivity for the extracted document.
            context.startActivity(intent)
        }
    }
}
