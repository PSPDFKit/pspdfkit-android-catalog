/*
 *   Copyright © 2018-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

// We're temporarily suppressing the ProgressDialog being deprecated warning.
// Issue: https://github.com/PSPDFKit/PSPDFKit/issues/32215
@file:Suppress("DEPRECATION")

package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowInsets
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.graphics.toColorInt
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.pspdfkit.annotations.BlendMode
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.ThumbnailBarMode
import com.pspdfkit.configuration.sharing.ShareFeatures
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.document.processor.ComparisonDialogListener
import com.pspdfkit.document.processor.ComparisonDocument
import com.pspdfkit.document.processor.DocumentComparisonDialog
import com.pspdfkit.document.processor.PagePdf
import com.pspdfkit.document.processor.PdfProcessor
import com.pspdfkit.document.processor.PdfProcessorTask
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.tabs.PdfTabBarCloseMode
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.util.EnumSet

/**
 * This example shows how to use [PdfProcessor] for comparing PDF pages using a different
 * stroke color for each and blending these colored pages into a single document. Furthermore, it shows how to integrate the
 * {@link com.pspdfkit.document.processor.DocumentComparisonDialog} to align two documents for better comparison results.
 */
class DocumentComparisonExample(context: Context) : SdkExample(
    context.getString(R.string.documentComparisonExampleTitle),
    context.getString(R.string.documentComparisonExampleDescription)
) {
    private val documentAIndex = 0 // Destination page index
    private val documentBIndex = 0 // Source page index

    @SuppressLint("CheckResult")
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We'll show progress dialog while processing documents with PdfProcessor.
        val progressDialog = createProgressDialog(context)
        progressDialog.show()

        configuration.apply {
            // The example uses a custom activity with a floating action button to start document alignment.
            layout(R.layout.activity_document_comparison)
            // Disable features that are not important for this example.
            setThumbnailBarMode(ThumbnailBarMode.THUMBNAIL_BAR_MODE_NONE)
            annotationEditingEnabled(false)
            outlineEnabled(false)
            settingsMenuEnabled(false)
            bookmarkListEnabled(false)
            thumbnailGridEnabled(false)
            annotationListEnabled(false)
            documentInfoViewEnabled(false)
            searchEnabled(false)
            printingEnabled(false)
            contentEditingEnabled(false)
            setEnabledShareFeatures(EnumSet.noneOf(ShareFeatures::class.java))
            invertColors(false)
        }

        // Comparison process consists from 2 steps:
        // 1. Color strokes from both documents in different colors.
        // 2. Merge 2 pages from these documents with colored strokes together.
        val processDocumentsForComparison = Single.defer {
            // Color strokes in the old document to GREEN.
            val greenDocumentUri = changeStrokeColorForDocumentFromAssets(
                context,
                "comparison/Document-A.pdf",
                oldDocumentColor,
                "Document-A",
                documentAIndex
            )

            // Color strokes in the new document to RED.
            val redDocumentUri = changeStrokeColorForDocumentFromAssets(
                context,
                "comparison/Document-B.pdf",
                newDocumentColor,
                "Document-B",
                documentBIndex
            )

            // Now generate document by merging both colored pages.
            val mergedDocumentUri = generateComparisonDocument(context, greenDocumentUri, redDocumentUri, "Comparison")

            // Return Single emitting triple of red, green and merged documents.
            return@defer Single.just(Triple(greenDocumentUri, redDocumentUri, mergedDocumentUri))
        }

        processDocumentsForComparison.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                // Hide the progress dialog when processing finishes.
                progressDialog.hide()
            }
            .subscribe(
                Consumer {
                    // Start the PdfActivity with all 3 documents loaded in tabs. This will show documents A and B, as well as the
                    // comparison result.
                    val documentTabs = arrayOf(
                        DocumentDescriptor.fromUri(it.first),
                        DocumentDescriptor.fromUri(it.second),
                        DocumentDescriptor.fromUri(it.third).apply {
                            setTitle("Comparison")
                        }
                    )
                    val intent = PdfActivityIntentBuilder.fromDocumentDescriptor(context, *documentTabs)
                        .configuration(configuration.build())
                        // Make the tab with comparison document visible after starting the activity.
                        .visibleDocument(2)
                        .activityClass(DocumentComparisonActivity::class.java)
                        .build()
                    context.startActivity(intent)
                }
            )
    }

    private fun changeStrokeColorForDocumentFromAssets(context: Context, documentAsset: String, color: Int, outputFileName: String, pageIndex: Int): Uri {
        val outputFile = File(context.filesDir, "$outputFileName.pdf")

        val sourceDocument = PdfDocumentLoader.openDocument(context, DocumentSource(AssetDataProvider(documentAsset)))
        val task = PdfProcessorTask.fromDocument(sourceDocument).changeStrokeColorOnPage(pageIndex, color)
        PdfProcessor.processDocument(task, outputFile)

        return Uri.fromFile(outputFile)
    }

    private fun generateComparisonDocument(context: Context, oldDocumentUri: Uri, newDocumentUri: Uri, outputFileName: String): Uri {
        val outputFile = File(context.filesDir, "$outputFileName.pdf")

        val oldDocument = PdfDocumentLoader.openDocument(context, oldDocumentUri)
        val task = PdfProcessorTask.fromDocument(oldDocument)
            .mergePage(PagePdf(context, newDocumentUri, documentBIndex), documentAIndex, BlendMode.DARKEN)
        PdfProcessor.processDocument(task, outputFile)

        return Uri.fromFile(outputFile)
    }

    private fun createProgressDialog(context: Context): ProgressDialog {
        val progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Comparing documents")
        progressDialog.setProgressNumberFormat(null)
        progressDialog.setProgressPercentFormat(null)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.isIndeterminate = true
        progressDialog.setCancelable(false)
        return progressDialog
    }

    companion object {
        /** Tint color used for the old document. */
        val oldDocumentColor = "#F5281B".toColorInt()

        /** Tint color used for the new document. */
        val newDocumentColor = "#31C1FF".toColorInt()
    }
}

/**
 * This activity displays two documents to compare and the merged document.
 * It receives the selected points to updates the aligned comparison document.
 */
class DocumentComparisonActivity : PdfActivity(), ComparisonDialogListener {
    private lateinit var oldDocumentSource: DocumentSource
    private lateinit var newDocumentSource: DocumentSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (documentCoordinator.documents.size != 3) {
            error("This example is built to be launched with exactly 3 documents only (document A, B, and comparison result).")
        }

        // This example assumes that the first two documents are the original documents A and B.
        oldDocumentSource = documentCoordinator.documents[0].documentSource
        newDocumentSource = documentCoordinator.documents[1].documentSource
        initViews()

        if (savedInstanceState != null) {
            // If this activity is recreated after a configuration change, calling restore() ensures that the document will have the
            // correct callback (this activity) if it was shown before the configuration change. If no dialog was shown, this call is a
            // no-op.
            DocumentComparisonDialog.restore(this, this)
        }
    }

    private fun initViews() {
        pspdfKitViews.tabBar?.apply {
            setCloseMode(PdfTabBarCloseMode.CLOSE_DISABLED)
            // Center the tabs.
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val alignDocumentsButton = findViewById<ExtendedFloatingActionButton>(R.id.pspdf_fab_align)

        // Make sure the floating action button is not hidden behind the navigation bar (neither in normal mode nor in immersive mode).
        alignDocumentsButton.setOnApplyWindowInsetsListener { _, insets ->
            alignDocumentsButton.layoutParams = (alignDocumentsButton.layoutParams as RelativeLayout.LayoutParams).apply {
                val margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()

                bottomMargin =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        margin + insets.getInsets(WindowInsets.Type.systemBars()).bottom
                    } else {
                        @Suppress("DEPRECATION")
                        margin + insets.systemWindowInsetBottom
                    }
            }
            insets
        }

        alignDocumentsButton.setOnClickListener {
            val outputFile = filesDir.resolve("comparison-result.pdf")
            val oldDocument = ComparisonDocument(oldDocumentSource, 0, DocumentComparisonExample.oldDocumentColor)
            val newDocument = ComparisonDocument(newDocumentSource, 0, DocumentComparisonExample.newDocumentColor)
            DocumentComparisonDialog.show(this, configuration, oldDocument, newDocument, outputFile, this)
        }
    }

    override fun onSetActivityTitle(configuration: PdfActivityConfiguration, document: PdfDocument?) {
        super.onSetActivityTitle(configuration, document)
        supportActionBar?.title = "Compare Documents"
    }

    override fun onComparisonSuccessful(alignedDocument: DocumentSource) {
        // Amit note: not sure if this code needs to handle non-file sources, but it doesn't, so return if not.
        val fileUri = alignedDocument.fileUri ?: return
        // Try to replace comparison document.
        try {
            documentCoordinator.removeDocument(documentCoordinator.documents[2])
            documentCoordinator.addDocument(DocumentDescriptor.fromUri(fileUri), 2)
            documentCoordinator.documents[2].setTitle("Comparison")
            documentCoordinator.setVisibleDocument(documentCoordinator.documents[2])
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }

    override fun onError(error: Throwable) {
        Log.e("Example", "An error happened while comparing the documents.", error)
        Toast.makeText(this, "There was a comparison error. Check logcat for details.", Toast.LENGTH_LONG).show()
    }
}
