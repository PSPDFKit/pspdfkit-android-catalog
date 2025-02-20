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
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * This example shows how to make [PdfActivity] use custom theme.
 */
class DarkThemeExample(context: Context) : SdkExample(context, R.string.darkThemeExampleTitle, R.string.darkThemeExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->

            // You can set the custom theme directly through PdfActivity configuration.
            configuration.theme(R.style.PSPDFCatalog_Theme_Dark)

            // Alternatively, you can also define theme on your custom activity directly in AndroidManifest.xml:
            // <activity
            //     android:name="YourCustomActivity"
            //     android:theme="@style/PSPDFCatalog.Theme.Dark" />

            // To start the DarkThemeActivity create a launch intent using the PdfActivityIntentBuilder
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .build()

            // Start the DarkThemeActivity for the extracted document.
            context.startActivity(intent)
        }
    }
}
