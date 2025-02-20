/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * This example shows how to use [PdfActivityIntentBuilder.emptyActivity] to open the
 * [com.pspdfkit.ui.PdfActivity] without any document loaded.
 * This is most useful when also providing a custom activity subclass with options for the user to open a document.
 */
class EmptyActivityExample(context: Context) : SdkExample(context, R.string.emptyActivityExampleTitle, R.string.emptyActivityExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We use emptyActivity() to start the PdfActivity without any document loaded.
        val intent = PdfActivityIntentBuilder.emptyActivity(context)
            .configuration(configuration.build())
            .build()
        context.startActivity(intent)
    }
}
