/*
 *   Copyright © 2020-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.SdkExample.Companion.TAG
import com.pspdfkit.catalog.ui.DocumentPickerActivity
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.compose.toolbar.state.ToolbarItem.ItemEdge
import com.pspdfkit.compose.toolbar.state.ToolbarItem.OverflowBehavior
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity

/**
 * Playground example that opens an activity extending the [PdfActivity] class.
 */
class PlaygroundExample(context: Context) : SdkExample(context, R.string.playgroundExampleTitle, R.string.playgroundExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Launch the picker activity to let users choose between default or custom document.
        val intent = Intent(context, PlaygroundExamplePickerActivity::class.java)
        intent.putExtra(DocumentPickerActivity.EXTRA_CONFIGURATION, configuration.build())
        context.startActivity(intent)
    }
}

/**
 * Activity that lets the user choose between picking a PDF and using the default document.
 */
class PlaygroundExamplePickerActivity : DocumentPickerActivity() {
    override val targetActivityClass = PlaygroundActivity::class.java
}

/**
 * Playground activity that provides an options menu accessible via the overflow menu (the three dots)
 * with a customizable button action, see [menuItemClicked]; and a callback when the document is successfully
 * loaded, see [onDocumentLoaded].
 */
class PlaygroundActivity : PdfActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarItemsProvider { defaultItems ->
            defaultItems + ToolbarItem.Button(
                id = CUSTOM_MENU_ITEM_ID,
                actionId = "custom_menu_item",
                contentDescription = "Custom Menu Item",
                position = ItemEdge.End,
                overflowBehavior = OverflowBehavior.Never,
            )
        }
        setOnToolbarItemClickListener { itemId ->
            when (itemId) {
                CUSTOM_MENU_ITEM_ID -> {
                    menuItemClicked()
                    true
                }

                else -> false
            }
        }
    }

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)
        Log.d(TAG, "Document loaded.")
        // Do your magic here...
    }

    private fun menuItemClicked() {
        Log.d(TAG, "Menu item clicked.")
    }

    companion object {
        private const val CUSTOM_MENU_ITEM_ID = 1
    }
}
