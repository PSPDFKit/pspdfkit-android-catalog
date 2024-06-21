/*
 *   Copyright © 2023-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import com.pspdfkit.catalog.R
import com.pspdfkit.ui.PdfActivity

/**
 * Opens the [PdfActivity] for viewing a PDF stored within the app's asset folder.
 */
class DefaultExample(context: Context) : AssetExample(context, R.string.defaultExampleTitle, R.string.defaultExampleDescription)
