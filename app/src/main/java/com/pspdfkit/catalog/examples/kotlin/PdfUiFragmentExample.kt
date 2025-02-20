/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.kotlin.activities.PdfUiFragmentExampleActivity
import com.pspdfkit.catalog.tasks.ExtractAssetTask.extract
import com.pspdfkit.configuration.activity.PdfActivityConfiguration

/**
 * This example shows how to use the [PdfUiFragment] to display PDFs.
 */
class PdfUiFragmentExample(context: Context) : SdkExample(context.getString(R.string.pdfUiFragmentExampleTitle), context.getString(R.string.pdfUiFragmentExampleDescription)) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        extract(QUICK_START_GUIDE, title, context) { documentFile ->
            val intent = Intent(context, PdfUiFragmentExampleActivity::class.java)
            intent.putExtra(PdfUiFragmentExampleActivity.EXTRA_URI, Uri.fromFile(documentFile))
            intent.putExtra(PdfUiFragmentExampleActivity.EXTRA_CONFIGURATION, configuration.build())
            context.startActivity(intent)
        }
    }
}
