/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

// We're temporarily suppressing the ProgressDialog being deprecated warning.
// Issue: https://github.com/PSPDFKit/PSPDFKit/issues/32215
@file:Suppress("DEPRECATION")

package com.pspdfkit.catalog.examples.kotlin.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.catalog.R
import com.pspdfkit.document.processor.NewPage
import com.pspdfkit.document.processor.PageImage
import com.pspdfkit.document.processor.PagePattern
import com.pspdfkit.document.processor.PagePosition
import com.pspdfkit.document.processor.PdfProcessor
import com.pspdfkit.document.processor.PdfProcessor.ProcessorProgress
import com.pspdfkit.document.processor.PdfProcessorTask
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subscribers.DisposableSubscriber
import java.io.File
import java.io.IOException
import java.util.HashSet
import kotlin.math.ceil

/**
 * This activity uses the [PdfProcessor] to split a document, removing annotations from the document, and flatten annotation
 * on a document.
 */
class DocumentProcessingExampleActivity : PdfActivity() {

    private val disposables = CompositeDisposable()

    /**
     * Creates menu items that will trigger document processing.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        // This will add actions for all document processing examples provided by this activity.
        menuInflater.inflate(R.menu.processor_example, menu)
        return true
    }

    /**
     * Triggered by selecting an action from the overflow menu in the action bar.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_extract_range -> {
                createDocumentFromRange()
                true
            }
            R.id.item_flatten_annotations -> {
                createFlattenedDocument()
                true
            }
            R.id.item_remove_link_annotations -> {
                createDocumentWithoutLinkAnnotations()
                true
            }
            R.id.item_rotate_pages -> {
                createDocumentWithRotatedPages()
                true
            }
            R.id.item_new_page -> {
                createDocumentWithNewPages()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createDocumentFromRange() {
        val document = document ?: return

        // Define the output file. This example writes to the internal app directory, into a file based on the document's Uid.
        val outputFile = File(filesDir, document.uid + "-range.pdf")

        // Extract pages with indexes 1, 2, 3, 5, 6, 14. All other pages won't be copied.
        val task = PdfProcessorTask.fromDocument(document)
            .removePages(HashSet(listOf(0, 4, 7, 8, 9, 10, 11, 12, 13)))

        // Start document processing, but without annotation flattening.
        PdfProcessor.processDocumentAsync(task, outputFile)
            // Drop update events to avoid back pressure on slow devices.
            .onBackpressureDrop()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(ProcessorProgressHandler("Extracting pages.", outputFile))
    }

    private fun createFlattenedDocument() {
        // Define the output file. This example writes to the internal app directory, into a file based on the document's Uid.
        val document = document
        val outputFile = File(filesDir, document!!.uid + "-flattened.pdf")

        // Start document processing, requesting a flattening of all annotations.
        val task = PdfProcessorTask.fromDocument(document)
            .changeAllAnnotations(PdfProcessorTask.AnnotationProcessingMode.FLATTEN)

        PdfProcessor.processDocumentAsync(task, outputFile)
            // Drop update events to avoid back pressure on slow devices.
            .onBackpressureDrop()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(ProcessorProgressHandler("Flattening annotations.", outputFile))
    }

    private fun createDocumentWithoutLinkAnnotations() {
        val document = document ?: return

        // Define the output file. This example writes to the internal app directory, into a file based on the document's Uid.
        val outputFile = File(filesDir, document.uid + "-without-link-annotations.pdf")

        // Start document processing, requesting a flattening of all annotations.
        val task = PdfProcessorTask.fromDocument(document)
            .changeAnnotationsOfType(AnnotationType.LINK, PdfProcessorTask.AnnotationProcessingMode.DELETE)

        PdfProcessor.processDocumentAsync(task, outputFile)
            // Drop update events to avoid back pressure on slow devices.
            .onBackpressureDrop()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(ProcessorProgressHandler("Removing link annotations.", outputFile))
    }

    private fun createDocumentWithRotatedPages() {
        val document = document ?: return

        // Define the output file. This example writes to the internal app directory, into a file based on the document's Uid.
        val outputFile = File(filesDir, document.uid + "-rotated.pdf")

        // Rotate all pages of the document by 90°.
        val task = PdfProcessorTask.fromDocument(document)
        var pageIndex = 0
        val pageCount = document.pageCount
        while (pageIndex < pageCount) {
            task.rotatePage(pageIndex, 90)
            pageIndex++
        }

        // Start document processing.
        PdfProcessor.processDocumentAsync(task, outputFile)
            // Drop update events to avoid back pressure on slow devices.
            .onBackpressureDrop()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(ProcessorProgressHandler("Rotating pages.", outputFile))
    }

    private fun createDocumentWithNewPages() {
        val document = document ?: return

        // Define the output file. This example writes to the internal app directory, into a file based on the document's Uid.
        val outputFile = File(filesDir, document.uid + "-new-page.pdf")

        // Create a yellow A5 page with a line pattern as first page.
        val task = PdfProcessorTask.fromDocument(document)
        task.addNewPage(
            NewPage.patternPage(NewPage.PAGE_SIZE_A5, PagePattern.LINES_7MM)
                .backgroundColor(Color.rgb(241, 236, 121))
                .build(),
            0
        )

        // Create an A0 page with an image as second page.
        try {
            val bitmap = BitmapFactory.decodeStream(assets.open("media/images/cover.jpg"))
            task.addNewPage(
                NewPage.emptyPage(NewPage.PAGE_SIZE_A0)
                    .withPageItem(PageImage(bitmap, PagePosition.CENTER))
                    .build(),
                1
            )
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Could not read page image.")
        }

        // The third page is cloned from the last page of the document, but rotated by 90°.
        task.addNewPage(
            NewPage.fromPage(document, document.pageCount - 1)
                .rotation(90)
                .build(),
            2
        )

        // Start document processing.
        PdfProcessor.processDocumentAsync(task, outputFile)
            // Drop update events to avoid back pressure on slow devices.
            .onBackpressureDrop()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(ProcessorProgressHandler("Creating new pages.", outputFile))
    }

    override fun onStop() {
        super.onStop()
        // Dispose all active disposables when activity goes to background.
        disposables.clear()
    }

    private fun showProcessedDocument(processedDocumentFile: File) {
        val context = this@DocumentProcessingExampleActivity
        val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(processedDocumentFile))
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
    ) : DisposableSubscriber<ProcessorProgress>() {

        private val progressDialog: ProgressDialog = ProgressDialog.show(
            this@DocumentProcessingExampleActivity,
            "Processing document",
            progressMessage,
            false,
            true
        ) { cancel() }

        init {
            disposables.add(this)
        }

        override fun onNext(processorProgress: ProcessorProgress) {
            progressDialog.progress = ceil((processorProgress.pagesProcessed / processorProgress.totalPages).toDouble()).toInt()
        }

        override fun onError(e: Throwable) {
            AlertDialog.Builder(this@DocumentProcessingExampleActivity)
                .setMessage("Error while processing file: " + e.localizedMessage)
                .show()
            progressDialog.dismiss()
        }

        override fun onComplete() {
            showProcessedDocument(outputFile)
            progressDialog.dismiss()
        }
    }

    companion object {
        private const val LOG_TAG = "DocumentProcessing"
    }
}
