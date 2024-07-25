/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * Playground example that opens an activity extending the [PdfActivity] class.
 */
class PlaygroundExample(context: Context) : SdkExample(context, R.string.playgroundExampleTitle, R.string.playgroundExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            // To start the `CustomLayoutActivity` create a launch intent using the builder.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(PlaygroundActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}

/**
 * Playground activity that provides an options menu accessible via the overflow menu (the three dots)
 * with a customizable button action, see [menuItemClicked]; and a callback when the document is successfully
 * loaded, see [onDocumentLoaded].
 */
class PlaygroundActivity : PdfActivity() {

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, CUSTOM_MENU_ITEM_ID, 0, "Custom Menu Item")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == CUSTOM_MENU_ITEM_ID) {
            menuItemClicked()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)
        Log.d(LOG_TAG, "Document loaded.")
        // Do your magic here...
    }

    private fun menuItemClicked() {
        Toast.makeText(this, "Menu item clicked!", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val CUSTOM_MENU_ITEM_ID = 1
        private const val LOG_TAG = "PlaygroundActivity"
    }
}
