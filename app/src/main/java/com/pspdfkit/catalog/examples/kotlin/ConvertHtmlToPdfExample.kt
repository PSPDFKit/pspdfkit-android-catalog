/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

/*
 * [ProgressDialog] is deprecated, since it is not recommended to use modal progress dialogs
 * anymore, instead progress indication should be part of the activity layout. For the sake of
 * simplicity, we ignore this deprecation warning in this example.
 */
@file:Suppress("DEPRECATION")

package com.pspdfkit.catalog.examples.kotlin

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.html.HtmlToPdfConverter
import com.pspdfkit.document.processor.NewPage
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

/**
 * This example shows how to use [HtmlToPdfConverter] to convert simple HTML documents to PDF.
 */
class ConvertHtmlToPdfExample(context: Context) : SdkExample(
    context.getString(R.string.htmlToPdfConversionExampleTitle),
    context.getString(R.string.htmlToPdfConversionExampleDescription)
) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We'll show a progress dialog while generating the PDF document.
        val progressDialog = createProgressDialog(context)
        progressDialog.show()

        // Perform the conversion. We'll use an HTML document from the assets in this example.
        val subscription = HtmlToPdfConverter.fromUri(context, Uri.parse("file:///android_asset/html-conversion/invoice.html"))
            // Alternatively, you can also pass your HTML as a string via:
            // HtmlToPdfConverter.fromHTMLString(context, "<html>....</html>")
            // Use A4 page size.
            .pageSize(NewPage.PAGE_SIZE_A4)
            // Configure title of the created document.
            .title("Invoice")
            .convertToPdfAsync()
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { progressDialog.dismiss() }
            .subscribe(
                { outputFile ->
                    // Open the converted document in PdfActivity.
                    val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(outputFile))
                        .configuration(configuration.build())
                        .build()
                    context.startActivity(intent)
                },
                {
                    // Show toast when encountering an error.
                    Toast.makeText(context, "Could not convert HTML to PDF. Reason: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )

        // Cancel the conversion when progress dialog is canceled.
        progressDialog.setOnCancelListener { subscription.dispose() }
    }

    /**
     * Creates progress dialog that will be displayed while converting to PDF.
     */
    private fun createProgressDialog(context: Context) = ProgressDialog(context).apply {
        setTitle("Converting HTML to PDF")
        setProgressNumberFormat(null)
        setProgressPercentFormat(null)
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        isIndeterminate = true
        setCancelable(true)
    }
}
