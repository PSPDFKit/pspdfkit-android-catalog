/*
 *   Copyright © 2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.ai

import android.content.Context
import android.content.Intent
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration

class AiAssistantComposeExample(context: Context) : SdkExample(
    context,
    R.string.jetpackAiAssistantExampleTitle,
    R.string.jetpackAiAssistantExampleDescription
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val intent = Intent(context, AiAssistantComposeActivity::class.java)
        context.startActivity(intent)
    }
}
