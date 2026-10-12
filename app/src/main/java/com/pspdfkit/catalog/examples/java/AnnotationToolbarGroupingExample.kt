/*
 *   Copyright © 2016-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
@file:Suppress("DEPRECATION")

package com.pspdfkit.catalog.examples.java

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.ui.CustomAnnotationCreationToolbarGroupingRule
import com.pspdfkit.compose.toolbar.adapter.ToolbarSubscription
import com.pspdfkit.compose.toolbar.state.ContextualToolbarKind
import com.pspdfkit.compose.toolbar.state.ToolbarAction
import com.pspdfkit.compose.toolbar.state.ToolbarIcon
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.jetpack.compose.toolbar.nutrientToolbarCoordinator
import com.pspdfkit.jetpack.compose.toolbar.setMenuItemGroupingRule
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import kotlinx.collections.immutable.mutate

/**
 * Example of how to alter contextual toolbar menu items structure. Displayed on the annotation
 * creation toolbar.
 */
class AnnotationToolbarGroupingExample(context: Context) :
    SdkExample(
        context,
        R.string.annotationToolbarGroupingExampleTitle,
        R.string.annotationToolbarGroupingExampleDescription,
    ) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(AnnotationToolbarGroupingActivity::class.java)
                .build()
            context.startActivity(intent)
        }
    }

    /**
     * This example shows how to add items to the annotation creation toolbar and apply a custom
     * grouping rule. See [CustomAnnotationCreationToolbarGroupingRule] for the applied grouping.
     */
    class AnnotationToolbarGroupingActivity : PdfActivity() {

        /** Click subscription on the toolbar coordinator; closed in [onDestroy]. */
        private var actionSubscription: ToolbarSubscription? = null

        /** Contextual-toolbar state subscription; closed in [onDestroy]. */
        private var changeSubscription: ToolbarSubscription? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // The activity-scoped toolbar coordinator is the state container PdfActivity's Compose
            // toolbars run on — customizing the contextual (annotation) toolbar goes through it.
            val coordinator = nutrientToolbarCoordinator()

            // Register the grouping rule that tells the toolbar how to group its menu items. This
            // uses the deprecated MenuItemGroupingRule bridge so existing
            // PresetMenuItemGroupingRule subclasses keep working; new code should implement
            // ContextualToolbarGroupingProvider and register it with
            // coordinator.setContextualToolbarGroupingProvider(...) instead.
            coordinator.setMenuItemGroupingRule(this, CustomAnnotationCreationToolbarGroupingRule(this))

            // The SDK rebuilds the annotation toolbar's item list every time the mode is entered,
            // so we observe the contextual toolbar state and (re-)apply our customizations on
            // every show. This replaces the legacy onPrepareContextualToolbar hook for the
            // annotation toolbar. The change listener needs no coroutine scope — it fires
            // synchronously on registration (with the current state) and on every change; a
            // View-based host just closes the subscription in onDestroy.
            changeSubscription = coordinator.addContextualToolbarChangeListener { state ->
                // The state carries which bar is shown and where it is docked; null means no
                // contextual toolbar is visible.
                Log.i(LOG_TAG, "Contextual toolbar changed: kind=${state?.kind} position=${state?.position}")
                if (state?.kind != ContextualToolbarKind.Annotation) {
                    return@addContextualToolbarChangeListener
                }

                // Append our custom "Bookmark" item — guarded so state updates after the
                // initial show (e.g. our own edit below) don't add it twice.
                if (state.items.none { it.id == R.id.pspdf_menu_custom }) {
                    coordinator.updateContextualItems { items ->
                        items.add(
                            ToolbarItem.Button(
                                actionId = "catalog.custom_bookmark",
                                id = R.id.pspdf_menu_custom,
                                contentDescription = "Bookmark",
                                icon = ToolbarIcon.Resource(R.drawable.ic_bookmark),
                                position = ToolbarItem.ItemEdge.Start,
                            ),
                        )
                    }
                }

                // The Compose counterpart of the legacy ContextualToolbar.setDraggable(false):
                // hides the drag handle and pins the toolbar to its current position.
                coordinator.setContextualToolbarDraggable(false)

                // Note: the legacy setUseBackButtonForCloseWhenHorizontal behavior is built in —
                // a top-docked bar leads with a back arrow, a side-docked one shows the circled
                // X. AnnotationToolbar.setShouldShowStylusButton(false) has no Compose
                // equivalent: the stylus toggle appears automatically while a stylus is
                // connected.
            }

            // Handle clicks on our custom item. Group taps and submenu child taps arrive as
            // SubmenuChildClicked, direct item taps as ItemClicked — our grouping rule keeps the
            // item standalone, but matching both is the robust pattern.
            actionSubscription = coordinator.addActionListener { action ->
                val clickedId = when (action) {
                    is ToolbarAction.ItemClicked -> action.id
                    is ToolbarAction.SubmenuChildClicked -> action.childId
                    else -> null
                }
                if (clickedId == R.id.pspdf_menu_custom) {
                    Toast.makeText(this, "Custom Action clicked", Toast.LENGTH_SHORT).show()

                    // You can even hide menu items. This shows how to toggle the image menu item
                    // by clicking on the custom button. Hidden items keep their slot logic but are
                    // removed from the row, like View.GONE on the legacy toolbar.
                    coordinator.updateContextualItems { items ->
                        items.mutate { list ->
                            val index = list.indexOfFirst { it.id == com.pspdfkit.R.id.pspdf__annotation_toolbar_item_image }
                            if (index != -1) {
                                val item = list[index]
                                list[index] = item.withVisible(!item.state.isVisible)
                            }
                        }
                    }
                }
            }
        }

        private companion object {
            const val LOG_TAG = "CustomToolbarGrouping"
        }

        override fun onDestroy() {
            super.onDestroy()
            actionSubscription?.close()
            changeSubscription?.close()
            actionSubscription = null
        }

        /** Returns a copy of the item with [ToolbarItem.state]'s visibility flipped to [visible]. */
        private fun ToolbarItem.withVisible(visible: Boolean): ToolbarItem = when (this) {
            is ToolbarItem.Button -> copy(state = state.copy(isVisible = visible))
            is ToolbarItem.Toggle -> copy(state = state.copy(isVisible = visible))
            is ToolbarItem.Submenu -> copy(state = state.copy(isVisible = visible))
        }
    }
}
