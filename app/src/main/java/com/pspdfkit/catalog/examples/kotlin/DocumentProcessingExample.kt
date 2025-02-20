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
import com.pspdfkit.catalog.examples.kotlin.activities.DocumentProcessingExampleActivity
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.processor.PdfProcessor
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * This example shows how to use the [PdfProcessor] to split a document, remove annotations from
 * the document, and flatten annotations on a document.
 */
class DocumentProcessingExample(context: Context) : SdkExample(context, R.string.documentProcessingExampleTitle, R.string.documentProcessingExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // This example uses a custom activity which showcases several document processing features.
        // For the sake of simplicity, deactivate actions in the processing activity.
        configuration.annotationListEnabled(false)
            .searchEnabled(false)
            .outlineEnabled(false)
            .thumbnailGridEnabled(false)

        // First extract the example document from the assets.
        ExtractAssetTask.extract(ANNOTATIONS_EXAMPLE, title, context) { documentFile ->
            // This example opens up a compound document, by providing the extracted document twice.
            // This is just an example of how you can merge two, or more documents.
            val documentUri1 = Uri.fromFile(documentFile)
            val documentUri2 = Uri.fromFile(documentFile)

            // To start the DocumentProcessingExampleActivity create a launch intent using the builder.
            val intent = PdfActivityIntentBuilder.fromUri(context, documentUri1, documentUri2)
                .configuration(configuration.build())
                .activityClass(DocumentProcessingExampleActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}
