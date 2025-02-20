/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.editor.PdfDocumentEditorFactory
import com.pspdfkit.document.processor.NewPage
import com.pspdfkit.document.processor.PagePattern
import com.pspdfkit.listeners.scrolling.DocumentScrollListener
import com.pspdfkit.listeners.scrolling.ScrollState
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.utils.PdfLog
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

/**
 * An example that shows how to use the onDocumentScrolled() callback to detect when the page has reached its end.
 */
class DocumentScrollExample(context: Context) : SdkExample(context, R.string.documentScrollExampleTitle, R.string.documentScrollExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            // To start the `CustomLayoutActivity` create a launch intent using the builder.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(DocumentScrollActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}

/**
 * Shows how to determine whether the document has been scrolled to the end
 */
class DocumentScrollActivity : PdfActivity() {

    private var currentZoom = 1f
    private var currentScroll = 0
    private var maxScroll = 0
    private var extend = 0
    private var isLastPage = false

    override fun onDocumentZoomed(document: PdfDocument, pageIndex: Int, scaleFactor: Float) {
        currentZoom = scaleFactor
    }

    override fun onPageChanged(document: PdfDocument, pageIndex: Int) {
        isLastPage = pageIndex + 1 == document.pageCount
    }

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)
        val pageScrollDirection = configuration.configuration.scrollDirection
        val scrollMode = configuration.configuration.scrollMode

        requirePdfFragment().addDocumentScrollListener(object : DocumentScrollListener {
            override fun onScrollStateChanged(state: ScrollState) {}

            override fun onDocumentScrolled(currX: Int, currY: Int, maxX: Int, maxY: Int, extendX: Int, extendY: Int) {
                val isVerticalScroll = pageScrollDirection == PageScrollDirection.VERTICAL
                val isHorizontalScroll = pageScrollDirection == PageScrollDirection.HORIZONTAL
                val isNoZoomOrContinuousScroll = currentZoom == 1f || scrollMode == PageScrollMode.CONTINUOUS
                val isPerPage = scrollMode == PageScrollMode.PER_PAGE

                if (isVerticalScroll && isNoZoomOrContinuousScroll) {
                    currentScroll = currY
                    maxScroll = maxY
                    extend = extendY
                } else if (isHorizontalScroll && isNoZoomOrContinuousScroll) {
                    currentScroll = currX
                    maxScroll = maxX
                    extend = extendX
                }

                // Handle cases for when this is the last page and the document has been zoomed in and the user tries swiping to access the next page
                // If you do not care about the zoom, then you can get rid of this section as well as onDocumentZoomed() and onPageChanged() above
                if (isLastPage && isPerPage && currentZoom > 1.0f) {
                    if (isVerticalScroll) {
                        currentScroll = currY
                        maxScroll = maxY
                        extend = extendY
                    } else {
                        currentScroll = currX
                        maxScroll = maxX
                        extend = extendX
                    }
                }

                val scrollRange = maxScroll - extend
                if (currentScroll == scrollRange) {
                    PdfLog.d(LOG_TAG, "Document end: Current scroll : $currentScroll Scroll range: $scrollRange")
                    addNewPageToDocument(document)
                }
            }
        })
    }

    @SuppressLint("CheckResult")
    private fun addNewPageToDocument(document: PdfDocument) {
        val editor = PdfDocumentEditorFactory.createForDocument(document)
        val newPage = NewPage.patternPage(NewPage.PAGE_SIZE_A5, PagePattern.LINES_7MM)
            .backgroundColor(Color.rgb(241, 236, 121))
            .build()

        // Add a new page at the end of the document
        editor.addPage(document.pageCount, newPage).blockingSubscribe()

        // Save the document and set it
        editor
            .saveDocument(this, null)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    val documentDescriptor = DocumentDescriptor.fromDocumentSource(document.documentSource)
                    documentCoordinator.setDocument(documentDescriptor)
                },
                { e ->
                    PdfLog.e(LOG_TAG, e, "Document couldn't be saved.")
                }
            )
    }

    companion object {
        private const val LOG_TAG = "DocumentScrollActivity"
    }
}
