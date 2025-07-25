/*
 *   Copyright © 2020-2025 PSPDFKit GmbH. All rights reserved.
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
 * Starts the InstantExampleConnectionActivity, which connects to the example Instant Server
 * (Nutrient Document Engine) at [nutrient.io/demo](https://nutrient.io/demo)
 * and displays the Instant document.
 */
class InstantExample(context: Context) : SdkExample(context, R.string.tryInstantExampleTitle, R.string.tryInstantExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Instant example starts with a simple login/connection screen.
        val intent = Intent(context, InstantExampleConnectionActivity::class.java)
        // Pass the configuration to the connection activity. This configuration will
        // be passed to created InstantPdfActivity with downloaded Instant document.
        // NOTE: Since Instant Comments have to be supported by the used Nutrient Document Engine license,
        // Nutrient for Android disables Instant Comment functionality by default. In our example,
        // our server supports Instant Comments, so we can safely enable these tools here.
        val enabledTools = configuration.build().configuration.enabledAnnotationTools.toMutableList()
        enabledTools.addAll(EnumSet.of(AnnotationTool.INSTANT_COMMENT_MARKER, AnnotationTool.INSTANT_HIGHLIGHT_COMMENT))
        configuration.enabledAnnotationTools(enabledTools)
        intent.putExtra(InstantExampleConnectionActivity.CONFIGURATION_ARG, configuration.build())
        context.startActivity(intent)
    }
}
