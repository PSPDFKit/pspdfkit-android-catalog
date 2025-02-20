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
import com.pspdfkit.configuration.sharing.ShareFeatures
import java.util.EnumSet

class ReaderViewExample(context: Context) : AssetExample(context, R.string.readerViewExampleTitle, R.string.readerViewExampleDescription) {
    override val assetPath: String
        get() = "The-Cosmic-Context-for-Life.pdf"

    override fun prepareConfiguration(configuration: PdfActivityConfiguration.Builder) {
        configuration.apply {
            // The reader view is disabled by default. You can enable it using the activity configuration, which will put the reader view
            // action item into the primary toolbar.
            enableReaderView(true)

            // We disable additional features which are enabled by default. This just keeps the example simpler.
            disableAnnotationEditing()
            disableAnnotationList()
            disableBookmarkList()
            disableOutline()
            disablePrinting()
            disableSearch()
            setEnabledShareFeatures(EnumSet.noneOf(ShareFeatures::class.java))
            hideSettingsMenu()
        }
    }
}
