/*
 *   Copyright © 2017-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.ThumbnailBarMode
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * This example demonstrates different thumbnail bar modes available in the SDK.
 * It allows toggling between Floating, Pinned, Scrollable, and None modes via the options menu.
 */
class ThumbnailBarExample(context: Context) :
    SdkExample(
        context,
        R.string.thumbnailBarExampleTitle,
        R.string.thumbnailBarExampleDescription,
    ) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Start with floating thumbnail bar mode (default)
        configuration.setThumbnailBarMode(ThumbnailBarMode.THUMBNAIL_BAR_MODE_FLOATING)

        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(ThumbnailBarExampleActivity::class)
                .build()

            context.startActivity(intent)
        }
    }
}

/**
 * Activity that demonstrates switching between different thumbnail bar modes.
 */
class ThumbnailBarExampleActivity : PdfActivity() {

    private var currentMode = ThumbnailBarMode.THUMBNAIL_BAR_MODE_FLOATING

    companion object {
        private const val KEY_THUMBNAIL_BAR_MODE = "thumbnail_bar_mode"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.thumbnail_bar_modes_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Update checked state based on current mode
        menu.findItem(R.id.thumbnail_bar_mode_floating)?.isChecked =
            currentMode == ThumbnailBarMode.THUMBNAIL_BAR_MODE_FLOATING
        menu.findItem(R.id.thumbnail_bar_mode_pinned)?.isChecked =
            currentMode == ThumbnailBarMode.THUMBNAIL_BAR_MODE_PINNED
        menu.findItem(R.id.thumbnail_bar_mode_scrollable)?.isChecked =
            currentMode == ThumbnailBarMode.THUMBNAIL_BAR_MODE_SCROLLABLE
        menu.findItem(R.id.thumbnail_bar_mode_none)?.isChecked =
            currentMode == ThumbnailBarMode.THUMBNAIL_BAR_MODE_NONE
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_THUMBNAIL_BAR_MODE, currentMode.name)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentMode = savedInstanceState.getString(KEY_THUMBNAIL_BAR_MODE)
            ?.let { ThumbnailBarMode.valueOf(it) }
            ?: ThumbnailBarMode.THUMBNAIL_BAR_MODE_FLOATING
        pspdfKitViews.thumbnailBarView?.setThumbnailBarMode(currentMode)
        invalidateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val newMode = when (item.itemId) {
            R.id.thumbnail_bar_mode_floating -> ThumbnailBarMode.THUMBNAIL_BAR_MODE_FLOATING
            R.id.thumbnail_bar_mode_pinned -> ThumbnailBarMode.THUMBNAIL_BAR_MODE_PINNED
            R.id.thumbnail_bar_mode_scrollable -> ThumbnailBarMode.THUMBNAIL_BAR_MODE_SCROLLABLE
            R.id.thumbnail_bar_mode_none -> ThumbnailBarMode.THUMBNAIL_BAR_MODE_NONE
            else -> null
        } ?: return super.onOptionsItemSelected(item)

        currentMode = newMode
        pspdfKitViews.thumbnailBarView?.setThumbnailBarMode(newMode)
        invalidateOptionsMenu()
        return true
    }
}
