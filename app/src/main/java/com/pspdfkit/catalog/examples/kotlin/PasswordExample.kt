/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import com.pspdfkit.catalog.R
import com.pspdfkit.ui.PdfActivity

/**
 * Shows the [PdfActivity] with a password protected document loaded.
 */
class PasswordExample(context: Context) : AssetExample(context, R.string.passwordExampleTitle, R.string.passwordExampleDescription) {
    override val assetPath: String
        get() = "password.pdf"
}
