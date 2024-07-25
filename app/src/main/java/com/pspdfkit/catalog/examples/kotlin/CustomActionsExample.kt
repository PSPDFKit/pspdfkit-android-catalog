/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.kotlin.activities.CustomActionsActivity
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * This example shows how to use the Kotlin language to configure a custom [com.pspdfkit.ui.PdfActivity]. Kotlin is a JVM-compatible programming language and
 * the second by Google supported programming language for developing Android apps (see introduction: https://developer.android.com/kotlin/index.html).
 */
class CustomActionsExample(context: Context) :
    SdkExample(context.getString(R.string.customActionsExampleTitle), context.getString(R.string.customActionsExampleDescription)) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(SdkExample.QUICK_START_GUIDE, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(CustomActionsActivity::class)
                .build()

            // You can add your own intent extras to the activity too.
            intent.putExtra(CustomActionsActivity.STRING_SAMPLE_ARG, "This toast message is passed via intent extras.")

            // Start the CustomActionsActivity for the extracted document.
            context.startActivity(intent)
        }
    }
}
