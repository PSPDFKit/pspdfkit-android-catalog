/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.kotlin.activities.HideRevealAnnotationsCreationActivity
import com.pspdfkit.catalog.tasks.ExtractAssetTask.extract
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.ThumbnailBarMode
import com.pspdfkit.preferences.PSPDFKitPreferences
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * This example allow users to select areas to hide/reveal on a page.
 */
class HideRevealAnnotationsCreationExample(context: Context) :
    SdkExample(context, R.string.hideRevealAnnotationsCreationTitle, R.string.hideRevealAnnotationsCreationDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        configuration
            // Turn off saving, so we have the clean original document every time the example is launched.
            .autosaveEnabled(false)
            // Disable thumbnail bar.
            .setThumbnailBarMode(ThumbnailBarMode.THUMBNAIL_BAR_MODE_NONE)
            // Disable annotation copy and paste.
            .copyPastEnabled(false)
            // Disable text selection.
            .textSelectionEnabled(false)

        // The annotation creator written into newly created annotations. If not set, or set to null
        // a dialog will normally be shown when creating an annotation, asking you to enter a name.
        // We are going to skip this part and set it as "John Doe" only if it was not yet set.
        if (!PSPDFKitPreferences.get(context).isAnnotationCreatorSet) {
            PSPDFKitPreferences.get(context).setAnnotationCreator("John Doe")
        }
        // Extract the document from the assets.
        extract("Classbook.pdf", title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(HideRevealAnnotationsCreationActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}
