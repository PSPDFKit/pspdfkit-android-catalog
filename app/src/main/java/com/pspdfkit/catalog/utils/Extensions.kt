/*
 *   Copyright © 2021-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.utils

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample

fun String.firstCharacterUpperCase(): String {
    return this
        .lowercase()
        .replaceFirstChar { char -> char.uppercase() }
}

fun SdkExample.isDigitalSignatureExample(context: Context) = when (this.title) {
    context.getString(R.string.digitalSignatureExampleTitle),
    context.getString(R.string.manualSigningExampleTitle),
    context.getString(R.string.twoStepSigningExampleTitle),
    context.getString(R.string.thirdPartySigningExampleTitle) -> true
    else -> false
}
fun SdkExample.isAiAssistantExample(context: Context) = when (this.title) {
    context.getString(R.string.aiAssistantViewPagerExampleTitle),
    context.getString(R.string.aiAssistantTabbedDocumentExampleTitle),
    context.getString(R.string.jetpackAiAssistantMultiDocExampleTitle),
    context.getString(R.string.jetpackAiAssistantExampleTitle) -> true
    else -> false
}

fun Context.getColorThemeRes(@AttrRes id: Int): Int {
    val resolvedAttr = TypedValue()
    this.theme.resolveAttribute(id, resolvedAttr, true)
    return this.getColor(resolvedAttr.resourceId)
}
