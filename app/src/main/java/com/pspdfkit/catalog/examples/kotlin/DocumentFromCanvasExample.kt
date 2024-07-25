/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.util.Log
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.processor.NewPage
import com.pspdfkit.document.processor.PdfProcessor
import com.pspdfkit.document.processor.PdfProcessorTask
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.IOException

/**
 * This example shows how to create a document from [Canvas] drawing.
 */
class DocumentFromCanvasExample(context: Context) : SdkExample(context, R.string.documentFromCanvasExampleTitle, R.string.documentFromCanvasExampleDescription) {

    private var documentProcessingDisposable: Disposable? = null

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Turn off saving, so we have the clean original document every time the example is launched.
        configuration.autosaveEnabled(false)

        // Create a canvas based on a A4 page.
        val pageCanvas = NewPage.fromCanvas(NewPage.PAGE_SIZE_A4) { canvas ->
            val paint = Paint().apply {
                style = Paint.Style.STROKE
            }

            val path = Path().apply {
                cubicTo(0f, 0f, 100f, 300f, 400f, 300f)
            }

            canvas.drawPath(path, paint)
        }.build()

        val task = PdfProcessorTask.newPage(pageCanvas)
        val outputFile = try {
            File(getCatalogCacheDirectory(context), "Canvas.pdf").canonicalFile
        } catch (exception: IOException) {
            throw IllegalStateException("Couldn't create Canvas.pdf file.", exception)
        }

        documentProcessingDisposable = PdfProcessor.processDocumentAsync(task, outputFile)
            // Ignore PdfProcessor progress.
            .ignoreElements()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(outputFile))
                        .configuration(configuration.build())
                        .build()
                    context.startActivity(intent)
                },
                { throwable ->
                    Log.e(TAG, "Error while trying to create PDF document.", throwable)
                }
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        documentProcessingDisposable?.dispose()
        documentProcessingDisposable = null
    }

    companion object {
        private const val TAG = "DocumentFromCanvas"
        private const val PSPDFKIT_DIRECTORY_NAME = "catalog-pspdfkit"

        @Throws(IOException::class)
        private fun getCatalogCacheDirectory(ctx: Context): File {
            val dir = File(ctx.cacheDir, PSPDFKIT_DIRECTORY_NAME)
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw IOException("Failed to create Catalog cache directory.")
                }
            }
            return dir
        }
    }
}
