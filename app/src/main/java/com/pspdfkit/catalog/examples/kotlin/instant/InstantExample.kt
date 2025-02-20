/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.instant

import android.content.Context
import android.content.Intent
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.kotlin.instant.activities.InstantExampleConnectionActivity
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import java.util.EnumSet

/**
 * Connects to example Instant Server (PSPDFKit Document Engine) at [pspdfkit.com/demo](https://pspdfkit.com/demo)
 */
class InstantExample(context: Context) : SdkExample(context, R.string.tryInstantExampleTitle, R.string.tryInstantExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Instant example starts with a simple login/connection screen.
        val intent = Intent(context, InstantExampleConnectionActivity::class.java)
        // Pass the configuration to the connection activity. This configuration will
        // be passed to created InstantPdfActivity with downloaded Instant document.
        // NOTE: Since Instant Comments have to be supported by the used PSPDFKit Document Engine license,
        // PSPDFKit for Android disables Instant Comment functionality by default. In our example,
        // our server supports Instant Comments, so we can safely enable these tools here.
        val enabledTools = configuration.build().configuration.enabledAnnotationTools.toMutableList()
        enabledTools.addAll(EnumSet.of(AnnotationTool.INSTANT_COMMENT_MARKER, AnnotationTool.INSTANT_HIGHLIGHT_COMMENT))
        configuration.enabledAnnotationTools(enabledTools)
        intent.putExtra(InstantExampleConnectionActivity.CONFIGURATION_ARG, configuration.build())
        context.startActivity(intent)
    }
}
