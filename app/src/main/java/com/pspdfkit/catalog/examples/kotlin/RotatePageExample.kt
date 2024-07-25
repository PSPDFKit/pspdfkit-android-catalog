/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * This example shows how to rotate pages in PdfActivity.
 */
class RotatePageExample(context: Context) : SdkExample(context, R.string.rotatePageExampleTitle, R.string.rotatePageExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // This example uses a custom activity which adds some option menu items. The default
        // menu items are deactivated for simplicity.
        configuration.disableSearch().disableOutline()

        // Start the activity once the example document has been extracted from the app's assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(RotatePageActivity::class.java)
                .build()

            // Start the RotatePageActivity for the extracted document.
            context.startActivity(intent)
        }
    }
}

class RotatePageActivity : PdfActivity() {
    /**
     * Creates custom rotation menu used by this example.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // It's important to call super before inflating the custom menu, or the custom menu won't be shown.
        super.onCreateOptionsMenu(menu)

        // Inflate our custom menu items for rotating pages.
        menuInflater.inflate(R.menu.activity_rotate_example, menu)
        return true
    }

    /**
     * Handles clicks on the navigation option menu items.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.rotateClockwise -> {
                rotatePage(pageIndex, 90)
                true
            }
            R.id.rotateCounterClockwise -> {
                rotatePage(pageIndex, -90)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun rotatePage(pageIndex: Int, rotationToApply: Int) {
        // Ensure the document is loaded.
        val document = document ?: return

        // Get the existing rotation offset of the current page.
        val currentRotationOffset = document.getRotationOffset(pageIndex)

        // Add the desired rotation to the current offset.
        var newRotation = currentRotationOffset + rotationToApply

        // Make sure that the new rotation offset is in bounds.
        if (newRotation < 0) {
            newRotation += 360
        } else if (newRotation >= 360) {
            newRotation -= 360
        }

        newRotation = when (newRotation) {
            0 -> PdfDocument.NO_ROTATION
            90 -> PdfDocument.ROTATION_90
            180 -> PdfDocument.ROTATION_180
            270 -> PdfDocument.ROTATION_270
            else -> return
        }

        // Applies a temporary rotation to the specified page of the document.
        // This will change the size reported by the document to match the new rotation.
        // The document will not be modified by this call.
        document.setRotationOffset(newRotation, pageIndex)
    }
}
