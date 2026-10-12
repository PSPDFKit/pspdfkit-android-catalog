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
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.compose.toolbar.state.ToolbarItem.ItemEdge
import com.pspdfkit.compose.toolbar.state.ToolbarItem.OverflowBehavior
import com.pspdfkit.compose.toolbar.state.ToolbarItemState
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.ThumbnailBarMode
import com.pspdfkit.listeners.scrolling.ScrollState
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.PdfThumbnailBar
import com.pspdfkit.utils.PdfLog

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

    companion object {
        private const val KEY_THUMBNAIL_BAR_MODE = "thumbnail_bar_mode"
        private const val KEY_THUMBNAIL_BAR_TAG = "thumbnail_bar"
        private const val ITEM_MODE_FLOATING = 90
        private const val ITEM_MODE_PINNED = 91
        private const val ITEM_MODE_SCROLLABLE = 92
        private const val ITEM_MODE_NONE = 93
    }

    private var currentMode = ThumbnailBarMode.THUMBNAIL_BAR_MODE_FLOATING
    private val onScrollListener: PdfThumbnailBar.OnScrollListener = object : PdfThumbnailBar.OnScrollListener() {
        override fun onScroll(firstVisiblePage: Int, firstVisibleOffsetPx: Int) {
            super.onScroll(firstVisiblePage, firstVisibleOffsetPx)
            PdfLog.i(KEY_THUMBNAIL_BAR_TAG, "firstVisiblePage $firstVisiblePage firstVisibleOffsetPx $firstVisibleOffsetPx")
        }

        override fun onScrollStateChanged(newState: ScrollState) {
            super.onScrollStateChanged(newState)
            PdfLog.i(KEY_THUMBNAIL_BAR_TAG, "ScrollState $newState")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setToolbarItemsProvider { defaultItems ->
            // Recompute the selected mode each time so the toolbar reflects the current state.
            defaultItems +
                thumbnailBarModeToggle(ITEM_MODE_FLOATING, ThumbnailBarMode.THUMBNAIL_BAR_MODE_FLOATING) +
                thumbnailBarModeToggle(ITEM_MODE_PINNED, ThumbnailBarMode.THUMBNAIL_BAR_MODE_PINNED) +
                thumbnailBarModeToggle(ITEM_MODE_SCROLLABLE, ThumbnailBarMode.THUMBNAIL_BAR_MODE_SCROLLABLE) +
                thumbnailBarModeToggle(ITEM_MODE_NONE, ThumbnailBarMode.THUMBNAIL_BAR_MODE_NONE)
        }

        setOnToolbarItemClickListener { itemId ->
            val newMode = when (itemId) {
                ITEM_MODE_FLOATING -> ThumbnailBarMode.THUMBNAIL_BAR_MODE_FLOATING
                ITEM_MODE_PINNED -> ThumbnailBarMode.THUMBNAIL_BAR_MODE_PINNED
                ITEM_MODE_SCROLLABLE -> ThumbnailBarMode.THUMBNAIL_BAR_MODE_SCROLLABLE
                ITEM_MODE_NONE -> ThumbnailBarMode.THUMBNAIL_BAR_MODE_NONE
                else -> return@setOnToolbarItemClickListener false
            }

            val bar = pspdfKitViews.thumbnailBarView
            currentMode = newMode
            bar?.setThumbnailBarMode(newMode)
            if (newMode == ThumbnailBarMode.THUMBNAIL_BAR_MODE_SCROLLABLE) bar?.setOnScrollListener(onScrollListener)

//            a custom outer margin can be added to the thumbnail bar by using
//            bar?.setOuterMargin(100, 100, 100, 100)

            invalidateToolbarItems()
            true
        }
    }

    private fun thumbnailBarModeToggle(itemId: Int, mode: ThumbnailBarMode): ToolbarItem.Toggle {
        val title = when (itemId) {
            ITEM_MODE_FLOATING -> getString(R.string.thumbnail_bar_mode_floating)
            ITEM_MODE_PINNED -> getString(R.string.thumbnail_bar_mode_pinned)
            ITEM_MODE_SCROLLABLE -> getString(R.string.thumbnail_bar_mode_scrollable)
            else -> getString(R.string.thumbnail_bar_mode_none)
        }
        return ToolbarItem.Toggle(
            id = itemId,
            actionId = "thumbnail_bar_mode_${mode.name}",
            contentDescription = title,
            state = ToolbarItemState(isSelected = currentMode == mode),
            position = ItemEdge.End,
            overflowBehavior = OverflowBehavior.Never,
        )
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
        invalidateToolbarItems()
    }
}
