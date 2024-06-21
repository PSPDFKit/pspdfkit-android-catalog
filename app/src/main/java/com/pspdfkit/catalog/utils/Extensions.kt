/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.utils

import android.content.Context
import com.pspdfkit.catalog.PSPDFExample
import com.pspdfkit.catalog.R

fun String.firstCharacterUpperCase(): String {
    return this
        .lowercase()
        .replaceFirstChar { char -> char.uppercase() }
}

fun PSPDFExample.isDigitalSignatureExample(context: Context, yes: () -> Unit, no: () -> Unit) = when (this.title) {
    context.getString(R.string.digitalSignatureExampleTitle),
    context.getString(R.string.manualSigningExampleTitle) -> yes.invoke()
    else -> no.invoke()
}
