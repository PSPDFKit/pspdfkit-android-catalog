/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
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
import com.pspdfkit.catalog.examples.kotlin.activities.JetpackComposeImageActivity
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration

/**
 * Opens the example image using Jetpack Compose.
 */
class JetpackComposeImageExample(context: Context) : SdkExample(
    context,
    R.string.jetpackImageExampleTitle,
    R.string.jetpackImageExampleDescription
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(ANDROID_IMAGE_PNG, title, context) { documentFile ->
            val intent = Intent(context, JetpackComposeImageActivity::class.java)
            intent.putExtra(JetpackComposeImageActivity.EXTRA_URI, Uri.fromFile(documentFile))
            context.startActivity(intent)
        }
    }
}
