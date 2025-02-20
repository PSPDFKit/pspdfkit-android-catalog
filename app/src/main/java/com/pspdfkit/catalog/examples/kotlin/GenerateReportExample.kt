/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

// We're temporarily suppressing the ProgressDialog being deprecated warning.
// Issue: https://github.com/PSPDFKit/PSPDFKit/issues/32215
@file:Suppress("DEPRECATION")

package com.pspdfkit.catalog.examples.kotlin

import android.app.ProgressDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.text.TextPaint
import com.pspdfkit.annotations.FreeTextAnnotation
import com.pspdfkit.annotations.StampAnnotation
import com.pspdfkit.annotations.appearance.AssetAppearanceStreamGenerator
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.DocumentPermissions
import com.pspdfkit.document.DocumentSaveOptions
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.document.processor.NewPage
import com.pspdfkit.document.processor.PageCanvas
import com.pspdfkit.document.processor.PagePattern
import com.pspdfkit.document.processor.PdfProcessor
import com.pspdfkit.document.processor.PdfProcessorTask
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.utils.Size
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.util.EnumSet

/**
 * This example shows how to programmatically create a PDF report.
 */
class GenerateReportExample(context: Context) : SdkExample(context, R.string.generateReportExampleTitle, R.string.generateReportExampleDescription) {

    /**
     * Disposable used to cancel the [com.pspdfkit.document.processor.PdfProcessor]
     * report generation when canceling the progress dialog.
     */
    private var generationDisposable: Disposable? = null

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val dialog = ProgressDialog.show(context, "Generating Report", "Please wait", true, true) {
            generationDisposable?.dispose()
        }

        generationDisposable = Single.fromCallable {
            // Open the document we are going to take the first and last page from.
            val sourceDocument = PdfDocumentLoader.openDocument(context, DocumentSource(AssetDataProvider("AnnualReport.pdf")))
            val pageSize = sourceDocument.getPageSize(0)

            // Create PDF processor task from the document.
            val task = PdfProcessorTask.fromDocument(sourceDocument)

            // Keep only the first and the last page of the original document.
            // Keep only the first and the last page of the original document.
            val pagesToRemove = mutableSetOf<Int>()
            for (i in 1 until sourceDocument.pageCount - 1) {
                pagesToRemove.add(i)
            }
            task.removePages(pagesToRemove)

            // Add a newly created single-paged document as the second page of the report
            val secondPageDocument = generateSecondPage(pageSize, context)
            task.addNewPage(NewPage.fromPage(secondPageDocument, 0).build(), 1)

            // Add a new page with a pattern grid as the third page of the report.
            task.addNewPage(
                NewPage.patternPage(pageSize, PagePattern.GRID_5MM)
                    .backgroundColor(Color.WHITE)
                    .build(),
                2
            )

            // Add a page from an existing document.
            val importDocument = PdfDocumentLoader.openDocument(context, DocumentSource(AssetDataProvider("Aviation.pdf")))
            task.addNewPage(NewPage.fromPage(importDocument, 0).build(), 3)

            // Scale the recently added page to the first page size
            task.resizePage(3, pageSize)

            // Draw "Generated for John Doe. Page X" watermark on every page
            drawWatermark("John Doe", task, pageSize, 5)

            // Flatten all annotations.
            task.changeAllAnnotations(PdfProcessorTask.AnnotationProcessingMode.FLATTEN)

            // Only allow opening by users that know the password.
            val password = "password"
            val saveOptions = DocumentSaveOptions(
                password,
                EnumSet.of(DocumentPermissions.PRINTING),
                false,
                null
            )

            // Finally create the resulting document.
            val generatedReportFile = File(context.getDir("documents", Context.MODE_PRIVATE), "generated-report.pdf")
            PdfProcessor.processDocument(task, generatedReportFile, saveOptions)

            return@fromCallable Uri.fromFile(generatedReportFile)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { dialog.cancel() }
            // Open the produced document in PdfActivity.
            .subscribe { uri -> PdfActivity.showDocument(context, uri, configuration.build()) }
    }

    @Throws(IOException::class)
    private fun generateSecondPage(pageSize: Size, context: Context): PdfDocument {
        // Create a separate single-paged document, which will be added as the second page of the report.

        // Start with the empty page.
        val task = PdfProcessorTask.newPage(NewPage.emptyPage(pageSize).backgroundColor(Color.WHITE).build())

        // Invoke processor to create new document.
        val tempFile = File.createTempFile("second_page", null, context.cacheDir)
        PdfProcessor.processDocument(task, tempFile)

        // Create the document which will be the report's second page
        val pageDocument = PdfDocumentLoader.openDocument(context, Uri.fromFile(tempFile))

        // Create a free text annotation as the title of the page.
        FreeTextAnnotation(
            0,
            RectF(228f, 1024f, 828f, 964f),
            "Some Annotations"
        ).apply {
            textSize = 40f

            pageDocument.annotationProvider.addAnnotationToPage(this)
        }

        // Create a vector stamp annotation.
        StampAnnotation(
            0,
            RectF(50f, 724f, 250f, 524f),
            "Stamp with custom AP stream"
        ).apply {
            // Set PDF from assets containing vector logo as annotation's appearance stream generator.
            appearanceStreamGenerator = AssetAppearanceStreamGenerator("images/PSPDFKit_Logo.pdf")

            pageDocument.annotationProvider.addAnnotationToPage(this)
        }

        // Create a free-text annotation as a label of the vector stamp.
        FreeTextAnnotation(
            0,
            RectF(67f, 520f, 667f, 420f),
            "The logo above is a vector stamp annotation."
        ).apply {
            textSize = 18f
            pageDocument.annotationProvider.addAnnotationToPage(this)
        }

        // Create an image stamp annotation.
        val image = BitmapFactory.decodeStream(context.assets.open("images/android.png"))
        StampAnnotation(
            0,
            RectF(60f, 400f, (60 + image.width / 4).toFloat(), (400 - image.height / 4).toFloat()),
            image
        ).let { pageDocument.annotationProvider.addAnnotationToPage(it) }

        // Create a free-text annotation as a label of the image stamp
        FreeTextAnnotation(
            0,
            RectF(67f, 240f, 667f, 160f),
            "The image above is an image stamp annotation."
        ).apply {
            textSize = 18f
            pageDocument.annotationProvider.addAnnotationToPage(this)
        }

        // Flatten all annotations
        val flattenedTempFile = File.createTempFile("flattened_second_page", null, context.cacheDir)
        val flattenTask = PdfProcessorTask.fromDocument(pageDocument)
        flattenTask.changeAllAnnotations(PdfProcessorTask.AnnotationProcessingMode.FLATTEN)
        PdfProcessor.processDocument(flattenTask, flattenedTempFile)

        return PdfDocumentLoader.openDocument(context, Uri.fromFile(flattenedTempFile))
    }

    private fun drawWatermark(name: String, task: PdfProcessorTask, pageSize: Size, pageCount: Int) {
        val textPaint = TextPaint().apply {
            textSize = 30f
            color = Color.argb(128, 255, 0, 0)
            textAlign = Paint.Align.CENTER
        }

        for (pageIndex in 0 until pageCount) {
            val canvas = PageCanvas(pageSize) { canvas ->
                canvas.drawText(
                    "Generated for $name. Page ${pageIndex + 1}",
                    pageSize.width / 2,
                    pageSize.height - 100,
                    textPaint
                )
            }
            task.addCanvasDrawingToPage(canvas, pageIndex)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        generationDisposable?.dispose()
    }
}
