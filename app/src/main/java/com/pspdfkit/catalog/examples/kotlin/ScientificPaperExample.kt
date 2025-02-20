/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import com.pspdfkit.catalog.R
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.UserInterfaceViewMode
import com.pspdfkit.configuration.page.PageFitMode
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.ui.PdfActivity

/**
 * Opens the [PdfActivity] configured for viewing a scientific paper.
 */
class ScientificPaperExample(context: Context) : AssetExample(context, R.string.scientificPaperExampleTitle, R.string.scientificPaperExampleDescription) {

    override val assetPath: String
        get() = "Scientific-paper.pdf"

    override fun prepareConfiguration(configuration: PdfActivityConfiguration.Builder) {
        configuration.scrollMode(PageScrollMode.CONTINUOUS)
        configuration.scrollDirection(PageScrollDirection.VERTICAL)
        configuration.fitMode(PageFitMode.FIT_TO_WIDTH)
        configuration.pagePadding(5)
        configuration.setUserInterfaceViewMode(UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_VISIBLE)
    }
}
