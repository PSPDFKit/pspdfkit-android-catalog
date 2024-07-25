/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.kotlin.activities.OverlayViewsActivity
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfActivityIntentBuilder

class OverlayViewsExample(context: Context) :
    SdkExample(context.getString(R.string.overlayViewsExample), context.getString(R.string.overlayViewsExampleDescription)) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context, true) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(
                    configuration
                        // We disable annotation editing to keep the example focused.
                        .editableAnnotationTypes(listOf(AnnotationType.NONE))
                        .build()
                )
                .activityClass(OverlayViewsActivity::class)
                .build()

            // Start the OverlayViewsActivity for the extracted document.
            context.startActivity(intent)
        }
    }
}
