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
import com.pspdfkit.configuration.activity.ThumbnailBarMode
import com.pspdfkit.configuration.activity.UserInterfaceViewMode
import com.pspdfkit.configuration.page.PageFitMode
import com.pspdfkit.configuration.sharing.ShareFeatures

/**
 * Opens the JavaScript Calculator example from assets.
 */
class JavaScriptCalculatorExample(context: Context) : AssetExample(context, R.string.javaScriptCalculatorExampleTitle, R.string.javaScriptCalculatorExampleDescription) {

    override val assetPath: String
        get() = "Calculator.pdf"

    override fun prepareConfiguration(configuration: PdfActivityConfiguration.Builder) {
        configuration
            .fitMode(PageFitMode.FIT_TO_WIDTH)
            // Disable all PSPDFKit views.
            .disableAnnotationEditing()
            .disableDocumentEditor()
            .disableSearch()
            .disableOutline()
            .disableBookmarkList()
            .disableAnnotationList()
            .setThumbnailBarMode(ThumbnailBarMode.THUMBNAIL_BAR_MODE_NONE)
            .hideDocumentTitleOverlay()
            .hidePageNumberOverlay()
            .hideThumbnailGrid()
            .setEnabledShareFeatures(ShareFeatures.none())
            .disablePrinting()
            .hideSettingsMenu()
            // Force toolbar visibility.
            .setUserInterfaceViewMode(UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_VISIBLE)
            // Disable text selection.
            .textSelectionEnabled(false)
            // Disable zoom.
            .maxZoomScale(1.0f)
    }
}
