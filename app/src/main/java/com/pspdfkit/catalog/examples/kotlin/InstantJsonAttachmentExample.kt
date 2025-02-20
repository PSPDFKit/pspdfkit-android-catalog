/*
 *   Copyright © 2022-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.kotlin.activities.InstantJsonAttachmentExampleActivity
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * Example showing how to export and import instant JSON attachment binaries.
 *
 */
class InstantJsonAttachmentExample(context: Context) : SdkExample(
    context,
    R.string.instantJsonAttachmentTitle,
    R.string.instantJsonAttachmentDescription
) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We turn off auto save to avoid saving redundant annotations.
        configuration.autosaveEnabled(false)

        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            // To start the `InstantJsonAttachmentExampleActivity` create a launch intent using the builder.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(InstantJsonAttachmentExampleActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}
