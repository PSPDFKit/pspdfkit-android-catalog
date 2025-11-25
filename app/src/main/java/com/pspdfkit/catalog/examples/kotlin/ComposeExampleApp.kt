/*
 *   Copyright © 2023-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration

private const val COMPOSE_EXAMPLE_LINK = "https://github.com/PSPDFKit/pspdfkit-jetpack-compose-pdf-viewer"

class ComposeExampleApp(context: Context) : SdkExample(context, R.string.composeExampleTitle, R.string.composeExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = COMPOSE_EXAMPLE_LINK.toUri()
            }
        )
    }
}
