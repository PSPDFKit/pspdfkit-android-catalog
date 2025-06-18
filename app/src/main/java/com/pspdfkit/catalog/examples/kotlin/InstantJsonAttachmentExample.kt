/*
 *   Copyright © 2022-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import androidx.activity.viewModels
import com.pspdfkit.annotations.StampAnnotation
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.providers.InputStreamDataProvider
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import kotlin.getValue

/**
 * Example showing how to export and import instant JSON attachment binaries.
 *
 */
class InstantJsonAttachmentExample(context: Context) : SdkExample(
    context,
    R.string.instantJsonAttachmentTitle,
    R.string.instantJsonAttachmentDescription
) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We turn off auto save to avoid saving redundant annotations.
        configuration.autosaveEnabled(false)

        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            // To start the `InstantJsonAttachmentExampleActivity` create a launch intent using the builder.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(InstantJsonAttachmentExampleActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}

/**
 * This example activity subclasses [PdfActivity], and it demonstrates how to export and import instant JSON attachment binaries using
 * instant JSON Attachment APIs.
 */
class InstantJsonAttachmentExampleActivity : PdfActivity() {

    private val viewModel: AnnotationCreationViewModel by viewModels()

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)

        viewModel.createObjects {
            // Create a stamp annotation from an image bitmap.
            val annotationBitmap = getBitmapFromAsset()
            val stampAnnotation = StampAnnotation(
                0,
                RectF(100f, (annotationBitmap.height / 2) + 100f, (annotationBitmap.width / 2) + 100f, 100f),
                annotationBitmap
            )

            // Add the stamp annotation to the document.
            document.annotationProvider.addAnnotationToPage(stampAnnotation)

            // Export START.

            // Convert annotation to instant JSON.
            val jsonData = stampAnnotation.toInstantJson()

            // Prepare a file to store the image attachment.
            val tempBinaryAttachmentJsonFile = File.createTempFile("tmp_", "stampAnnotationBinaryJsonAttachment.jpeg")
            val outputStream = FileOutputStream(tempBinaryAttachmentJsonFile)

            // Write the Instant JSON attachment data to the file.
            if (stampAnnotation.hasBinaryInstantJsonAttachment()) {
                stampAnnotation.fetchBinaryInstantJsonAttachment(outputStream)
            }

            // Save the JSON data in a file.
            val tempJsonFile = File.createTempFile("tmp_", "stampAnnotationJson.txt")
            FileWriter(tempJsonFile).use { w -> w.write(jsonData) }
            // Export END.

            // Let's remove the annotation to try the import feature.
            document.annotationProvider.removeAnnotationFromPage(stampAnnotation)

            // Import START.
            val anotherStampAnnotation = document.annotationProvider.createAnnotationFromInstantJson(jsonData)

            anotherStampAnnotation.attachBinaryInstantJsonAttachment(FileDataProvider(tempBinaryAttachmentJsonFile), "image/jpeg")
            // Import END.
        }
    }

    /** Decodes bitmap from application's assets. */
    private fun getBitmapFromAsset(): Bitmap {
        return try {
            val inputStream = assets.open("images/android.png")
            BitmapFactory.decodeStream(inputStream)
        } catch (ioException: IOException) {
            throw RuntimeException("Error while trying to load PNG from assets: images/android.png", ioException)
        }
    }
}

/**
 * Data provider that handles serving data from [File].
 */
open class FileDataProvider(val file: File) : InputStreamDataProvider() {
    override fun getSize(): Long {
        return file.length()
    }

    override fun getUid(): String {
        return file.canonicalPath
    }

    override fun openInputStream(): InputStream {
        return file.inputStream()
    }

    override fun getTitle(): String? {
        return file.nameWithoutExtension
    }
}
