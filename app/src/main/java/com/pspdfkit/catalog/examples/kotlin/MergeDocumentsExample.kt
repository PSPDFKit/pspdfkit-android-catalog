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
import android.net.Uri
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.document.processor.NewPage
import com.pspdfkit.document.processor.PdfProcessor
import com.pspdfkit.document.processor.PdfProcessorTask
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.ui.PdfActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File

/**
 * This example shows how to merge multiple PDFs using the document processor.
 */
class MergeDocumentsExample(context: Context) : SdkExample(context, R.string.mergeDocumentsExampleTitle, R.string.mergeDocumentsExampleDescription) {

    /** Disposable for document merge operation of [com.pspdfkit.document.processor.PdfProcessor].*/
    private var mergingDisposable: Disposable? = null

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val dialog = ProgressDialog.show(context, "Merging Documents", "Please wait", true, true) {
            mergingDisposable?.dispose()
        }

        mergingDisposable = Single.fromCallable {
            // Open the documents we are going to merge.
            val documents = listOf("AnnualReport.pdf", "Scientific-Report.pdf", QUICK_START_GUIDE)
                .asSequence()
                .map { PdfDocumentLoader.openDocument(context, DocumentSource(AssetDataProvider(it))) }

            // Start with an empty document.
            val task = PdfProcessorTask.empty()

            // Add all document pages.
            var totalPageCount = 0
            for (document in documents) {
                for (i in 0 until document.pageCount) {
                    // We use `totalPageCount` here to add the pages to the end.
                    // But you are free to add them at any place in the document you'd like.
                    task.addNewPage(NewPage.fromPage(document, i).build(), totalPageCount)
                    totalPageCount++
                }
            }

            // Finally create the resulting document.
            val mergedDocumentsFile = File(context.getDir("documents", Context.MODE_PRIVATE), "merged-documents.pdf")
            PdfProcessor.processDocument(task, mergedDocumentsFile)

            return@fromCallable Uri.fromFile(mergedDocumentsFile)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { dialog.cancel() }
            .subscribe { uri -> PdfActivity.showDocument(context, uri, configuration.build()) }
    }
}
