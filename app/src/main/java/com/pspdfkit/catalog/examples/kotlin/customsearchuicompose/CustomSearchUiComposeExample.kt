/*
 *   Copyright © 2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.customsearchuicompose

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration

class CustomSearchUiComposeExample(context: Context) : SdkExample(context, R.string.customSearchUiComposeExampleTitle, R.string.customSearchUiComposeExampleDescription) {
    /** Configuration is handled inside [CustomSearchUiComposeActivity] */
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            val intent = Intent(context, CustomSearchUiComposeActivity::class.java)
            intent.putExtra(CustomSearchUiComposeActivity.EXTRA_URI, Uri.fromFile(documentFile))
            context.startActivity(intent)
        }
    }
}
