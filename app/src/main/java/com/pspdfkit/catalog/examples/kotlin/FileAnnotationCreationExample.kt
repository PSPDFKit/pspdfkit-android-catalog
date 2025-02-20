/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import androidx.annotation.UiThread
import com.pspdfkit.annotations.FileAnnotation
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.java.activities.FileAnnotationCreationActivity
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.files.EmbeddedFileSource
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * This example shows how to create file annotations programmatically.
 */
class FileAnnotationCreationExample(context: Context) : SdkExample(
    context,
    R.string.fileAnnotationCreationExampleTitle,
    R.string.fileAnnotationCreationExampleDescription
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Turn off saving, so we have the clean original document every time the example is launched.
        configuration.autosaveEnabled(false)

        // Extract the document from the assets. The launched activity will add file annotations to that document.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(FileAnnotationCreationActivity::class.java)
                .build()
            context.startActivity(intent)
        }
    }
}

class FileAnnotationCreationActivity : PdfActivity() {
    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        val pageIndex = 0

        // Create file annotation with embedded pdf file.
        // File description and modification date is optional.
        val fileAnotation = FileAnnotation(
            pageIndex,
            RectF(180f, 692f, 212f, 660f),
            EmbeddedFileSource(
                AssetDataProvider("Annotations.pdf"),
                "Annotations.pdf",
                null
            )
        ).apply {
            iconName = FileAnnotation.GRAPH
            color = Color.GREEN
        }
        requirePdfFragment().addAnnotationToPage(fileAnotation, false)

        // Create file annotation with embedded image file.
        val imageFileAnnotation = FileAnnotation(
            pageIndex,
            RectF(244f, 692f, 276f, 660f),
            EmbeddedFileSource(
                AssetDataProvider("images/android.png"),
                "android.png",
                null
            )
        ).apply {
            iconName = FileAnnotation.PAPERCLIP
            color = Color.BLUE
        }
        requirePdfFragment().addAnnotationToPage(imageFileAnnotation, false)

        // Create file annotation with embedded video file.
        val videoFileAnnotation = FileAnnotation(
            pageIndex,
            RectF(308f, 692f, 340f, 660f),
            EmbeddedFileSource(
                AssetDataProvider("media/videos/small.mp4"),
                "small.mp4",
                "Example of an annotation with embedded video file."
            )
        ).apply {
            iconName = FileAnnotation.PUSH_PIN
            color = Color.RED
        }
        requirePdfFragment().addAnnotationToPage(videoFileAnnotation, false)

        // File source can also be directly specified as byte array with file's data.
        // In this case we create file annotation from UTF-8 encoded string.
        val textFileAnnotation = FileAnnotation(
            pageIndex,
            RectF(372f, 692f, 404f, 660f),
            EmbeddedFileSource(
                "Plain text data".toByteArray(),
                "note.txt",
                "Example of an annotation with embedded plain-text file."
            )
        ).apply {
            iconName = FileAnnotation.TAG
            color = Color.YELLOW
        }
        requirePdfFragment().addAnnotationToPage(textFileAnnotation, false)
    }
}
