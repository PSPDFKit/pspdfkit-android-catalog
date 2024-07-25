/*
 *   Copyright © 2023-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.listeners.OnPreparePopupToolbarListener
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.PopupToolbar
import com.pspdfkit.ui.toolbar.popup.PdfTextSelectionPopupToolbar
import com.pspdfkit.ui.toolbar.popup.PopupToolbarMenuItem

/**
 * Example showing how to customise the text highlight popup toolbar.
 */
class TextHighlightPopupCustomisationExample(context: Context) : SdkExample(
    context,
    R.string.textHighlightPopupCustomisationTitle,
    R.string.textHighlightPopupCustomisationDescription
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(TextHighlightPopupCustomisationActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}

/**
 * Our activity implements these extra listeners in order customise the popup toolbar.
 */
class TextHighlightPopupCustomisationActivity :
    PdfActivity(),
    OnPreparePopupToolbarListener,
    PopupToolbar.OnPopupToolbarItemClickedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // We need to register ourselves for the on prepare callback.
        pdfFragment?.setOnPreparePopupToolbarListener(this)
    }

    /** Override this [OnPreparePopupToolbarListener] method allows us to customise which buttons are shown in the popup toolbar. */
    override fun onPrepareTextSelectionPopupToolbar(toolbar: PdfTextSelectionPopupToolbar) {
        // Make sure we register for on click callbacks.
        toolbar.setOnPopupToolbarItemClickedListener(this)

        val menuItems = toolbar.menuItems
        // Clear the defaults
        menuItems.clear()

        // Here we can customise what popup buttons we want to see when highlighting text.
        // Let's add:

        // Copy
        menuItems.add(
            PopupToolbarMenuItem(
                com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_copy,
                com.pspdfkit.R.string.pspdf__action_menu_copy
            )
        )

        // Underline
        menuItems.add(
            PopupToolbarMenuItem(
                com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_underline,
                com.pspdfkit.R.string.pspdf__edit_menu_underline
            )
        )

        // And, create link
        menuItems.add(
            PopupToolbarMenuItem(
                com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_link,
                com.pspdfkit.R.string.pspdf__create_link
            )
        )

        // Make sure to set them back on the toolbar, so we can re-initialize it.
        toolbar.menuItems = menuItems
    }

    /** Override this [PopupToolbar.OnPopupToolbarItemClickedListener] allows us to customise the click behaviour. */
    override fun onItemClicked(popupToolbarMenuItem: PopupToolbarMenuItem): Boolean {
        val toastText = when (popupToolbarMenuItem.id) {
            com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_copy -> "Copy!!"
            com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_underline -> "Underline!!"
            com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_link -> "Liiiiiink!"
            else -> ""
        }

        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show()

        // Returning false means that we don't eat the touch and the default actions will also happen.
        // You can return true if you want to eat it.
        return false
    }
}
