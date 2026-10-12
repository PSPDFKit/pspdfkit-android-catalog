/*
 *   Copyright © 2020-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.compose.toolbar.state.ToolbarItem.ItemEdge
import com.pspdfkit.compose.toolbar.state.ToolbarItem.OverflowBehavior
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.TabBarHidingMode
import com.pspdfkit.configuration.activity.UserInterfaceViewMode
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * This example shows how to change user interface view modes. To do so, it subclasses the
 * [PdfActivity] and calls [PdfActivity.setUserInterfaceViewMode] method
 * to toggle between various user interface view modes.
 */
class UserInterfaceViewModesExample(context: Context) :
    SdkExample(context, R.string.userInterfaceViewModesExampleTitle, R.string.userInterfaceViewModesExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Configure the PdfActivity to use the examples subclass. This is
        // important if your app wants to tweak the default behavior of its implementation.
        configuration.layout(R.layout.user_interface_view_modes_activity)

        // Hide navigation buttons and tab bar since they are not used by this example.
        configuration.navigationButtonsEnabled(false)
        configuration.setTabBarHidingMode(TabBarHidingMode.HIDE)

        // The custom layout has no content editor. In order to prevent the activity from accessing
        // it we have to deactivate it in the configuration.
        configuration.contentEditingEnabled(false)

        // Disable measurements as well
        configuration.setMeasurementToolsEnabled(false)

        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            // Launch the custom example activity using the document and configuration.
            val intent =
                PdfActivityIntentBuilder
                    .fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(UserInterfaceViewModesActivity::class)
                    .build()

            // Start the UserInterfaceViewModesActivity for the extracted document.
            context.startActivity(intent)
        }
    }
}

/**
 * This subclass of [PdfActivity] adds a set of actions to change user interface view modes.
 */
class UserInterfaceViewModesActivity : PdfActivity() {
    companion object {
        private const val VIEW_MODE_AUTOMATIC = 90
        private const val VIEW_MODE_AUTOMATIC_BORDER_PAGES = 91
        private const val VIEW_MODE_VISIBLE = 92
        private const val VIEW_MODE_HIDDEN = 93
        private const val VIEW_MODE_MANUAL = 94
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<View>(R.id.show_user_interface_button).setOnClickListener {
            // This method overrides all restraints on showing user interface.
            // Shows user interface even when using USER_INTERFACE_VIEW_MODE_HIDDEN.
            setUserInterfaceVisible(true, true)
        }

        findViewById<View>(R.id.hide_user_interface_button).setOnClickListener {
            // This method overrides all restraints on hiding user interface.
            // Hides user interface even when using USER_INTERFACE_MODE_VISIBLE.
            setUserInterfaceVisible(false, true)
        }

        setToolbarItemsProvider { defaultItems ->
            defaultItems +
                viewModeButton(
                    VIEW_MODE_AUTOMATIC,
                    "user_interface_view_mode_automatic",
                    R.string.user_interface_view_mode_automatic,
                ) +
                viewModeButton(
                    VIEW_MODE_AUTOMATIC_BORDER_PAGES,
                    "user_interface_view_mode_automatic_border_pages",
                    R.string.user_interface_view_mode_automatic_border_pages,
                ) +
                viewModeButton(
                    VIEW_MODE_VISIBLE,
                    "user_interface_view_mode_visible",
                    R.string.user_interface_view_mode_visible,
                ) +
                viewModeButton(
                    VIEW_MODE_HIDDEN,
                    "user_interface_view_mode_hidden",
                    R.string.user_interface_view_mode_hidden,
                ) +
                viewModeButton(
                    VIEW_MODE_MANUAL,
                    "user_interface_view_mode_manual",
                    R.string.user_interface_view_mode_manual,
                )
        }

        setOnToolbarItemClickListener { itemId -> onToolbarItemClick(itemId) }
    }

    private fun viewModeButton(itemId: Int, actionId: String, titleRes: Int): ToolbarItem.Button = ToolbarItem.Button(
        id = itemId,
        actionId = actionId,
        contentDescription = getString(titleRes),
        position = ItemEdge.End,
        overflowBehavior = OverflowBehavior.Never,
    )

    private fun onToolbarItemClick(itemId: Int): Boolean {
        val newUserInterfaceViewMode =
            when (itemId) {
                VIEW_MODE_AUTOMATIC -> {
                    UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_AUTOMATIC
                }

                VIEW_MODE_AUTOMATIC_BORDER_PAGES -> {
                    UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_AUTOMATIC_BORDER_PAGES
                }

                VIEW_MODE_VISIBLE -> {
                    UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_VISIBLE
                }

                VIEW_MODE_HIDDEN -> {
                    UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_HIDDEN
                }

                VIEW_MODE_MANUAL -> {
                    UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_MANUAL
                }

                else -> {
                    null
                }
            } ?: return false

        this.userInterfaceViewMode = newUserInterfaceViewMode
        return true
    }

    override fun onUserInterfaceVisibilityChanged(visible: Boolean) {
        super.onUserInterfaceVisibilityChanged(visible)
        // You can monitor UI visibility changes by overriding this method.
    }
}
