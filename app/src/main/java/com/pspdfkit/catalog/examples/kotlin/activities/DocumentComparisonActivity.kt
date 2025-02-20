/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowInsets
import android.widget.RelativeLayout
import android.widget.Toast
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.examples.kotlin.DocumentComparisonExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.processor.ComparisonDialogListener
import com.pspdfkit.document.processor.ComparisonDocument
import com.pspdfkit.document.processor.DocumentComparisonDialog
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.tabs.PdfTabBarCloseMode

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
