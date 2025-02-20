/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import com.pspdfkit.PSPDFKit
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.policy.ApplicationPolicy
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * Example of how to customize application policy preventing copy/paste action.
 */
class CustomApplicationPolicyExample(context: Context) : SdkExample(context, R.string.customApplicationPolicyExampleTitle, R.string.customApplicationPolicyExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // To customize the application policy we extend ApplicationPolicy.
        val customApplicationPolicy = CustomApplicationPolicy()

        // Application policy needs to be set before documents are loaded.
        PSPDFKit.setApplicationPolicy(customApplicationPolicy)

        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            // Open the example document in PdfActivity.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .build()
            context.startActivity(intent)
        }
    }
}

/**
 * Custom Application policy that disables text copy and paste.
 */
class CustomApplicationPolicy : ApplicationPolicy() {
    override fun hasPermissionForEvent(event: PolicyEvent): Boolean {
        return when (event) {
            PolicyEvent.TEXT_COPY_PASTE ->
                // Disable text copy/paste policy.
                false
            else -> {
                // Enable remaining policies.
                true
            }
        }
    }
}
