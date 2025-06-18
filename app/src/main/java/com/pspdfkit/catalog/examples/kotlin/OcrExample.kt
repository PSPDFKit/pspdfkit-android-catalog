/*
 *   Copyright © 2020-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

// We're temporarily suppressing the ProgressDialog being deprecated warning.
// Issue: https://github.com/PSPDFKit/PSPDFKit/issues/32215
@file:Suppress("DEPRECATION")

package com.pspdfkit.catalog.examples.kotlin

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.processor.PdfProcessor
import com.pspdfkit.document.processor.PdfProcessorTask
import com.pspdfkit.document.processor.ocr.OcrLanguage
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subscribers.DisposableSubscriber
import java.io.File
import kotlin.math.ceil

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

/**
 * This examples shows how to use Optical character recognition
 * to extract the text from scanned documents using the [com.pspdfkit.document.processor.PdfProcessor].
 */
class OcrProcessingActivity : PdfActivity() {

    private val disposables = CompositeDisposable()

    /**
     * Perform OCR on the current document.
     */
    private fun performOcr() {
        // Define the output file. This example writes to the internal app directory, into a file based on the document's Uid.
        val document = document ?: return
        val outputFile = File(filesDir, "${document.title}-ocr-processed.pdf")
        val pageIndexesToProcess = (0 until document.pageCount).toSet()

        // Start document processing, perform OCR.
        val task = PdfProcessorTask.fromDocument(document)
            .performOcrOnPages(pageIndexesToProcess, OcrLanguage.ENGLISH)

        val handler = ProcessorProgressHandler("Performing OCR on the document.", outputFile)

        PdfProcessor.processDocumentAsync(task, outputFile)
            // Drop update events to avoid back pressure on slow devices.
            .onBackpressureDrop()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnCancel { handler.onCancel() }
            .subscribe(handler)
    }

    /**
     * Creates menu item that will enable document OCR processing.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.ocr_example, menu)
        return true
    }

    /**
     * Triggered by selecting an action from the menu in the action bar.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_perform_ocr -> {
                performOcr()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        super.onStop()
        // Dispose all active disposables when activity goes to background.
        disposables.clear()
    }

    private fun showProcessedDocument(processedDocumentFile: File) {
        val intent = PdfActivityIntentBuilder.fromUri(this, Uri.fromFile(processedDocumentFile))
            .configuration(configuration)
            .build()
        startActivity(intent)
    }

    /**
     * Helper class for showing a progress dialog and opening the processed document.
     */
    @Suppress("DEPRECATION")
    private inner class ProcessorProgressHandler(
        progressMessage: String,
        private val outputFile: File
    ) : DisposableSubscriber<PdfProcessor.ProcessorProgress>() {

        private val progressDialog: ProgressDialog = ProgressDialog.show(
            this@OcrProcessingActivity,
            "Processing document",
            progressMessage,
            false,
            true
        ) { cancel() }

        init {
            disposables.add(this)
        }

        fun onCancel() {
            progressDialog.dismiss()
        }

        override fun onNext(processorProgress: PdfProcessor.ProcessorProgress) {
            progressDialog.progress = ceil((processorProgress.pagesProcessed / processorProgress.totalPages).toDouble()).toInt()
        }

        override fun onError(e: Throwable) {
            AlertDialog.Builder(this@OcrProcessingActivity)
                .setMessage("Error while processing file: " + e.localizedMessage)
                .show()
            progressDialog.dismiss()
        }

        override fun onComplete() {
            showProcessedDocument(outputFile)
            progressDialog.dismiss()
        }
    }
}
