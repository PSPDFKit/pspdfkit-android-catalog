/*
 *   Copyright © 2019-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
@file:Suppress("DEPRECATION")

package com.pspdfkit.catalog.examples.java

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pspdfkit.annotations.configuration.LineAnnotationConfiguration
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.compose.toolbar.adapter.ToolbarCoordinator
import com.pspdfkit.compose.toolbar.adapter.ToolbarSubscription
import com.pspdfkit.compose.toolbar.state.ContextualToolbarKind
import com.pspdfkit.compose.toolbar.state.ToolbarAction
import com.pspdfkit.compose.toolbar.state.ToolbarIcon
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.jetpack.compose.toolbar.nutrientToolbarCoordinator
import com.pspdfkit.jetpack.compose.toolbar.setMenuItemGroupingRule
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.annotations.OnAnnotatingModeChangeListener
import com.pspdfkit.ui.special_mode.controller.AnnotatingController
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import com.pspdfkit.ui.special_mode.controller.AnnotationToolVariant
import com.pspdfkit.ui.toolbar.grouping.presets.MenuItem
import com.pspdfkit.ui.toolbar.grouping.presets.PresetMenuItemGroupingRule
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Example showing how to add custom annotation creation toolbar items and bind them to an
 * annotation tool and variant.
 */
class AnnotationToolbarCustomItemsExample(context: Context) :
    SdkExample(
        context,
        R.string.annotationToolbarCustomItemsExampleTitle,
        R.string.annotationToolbarCustomItemsExampleDescription,
    ) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(AnnotationToolbarCustomItemsActivity::class.java)
                .build()
            context.startActivity(intent)
        }
    }

    /**
     * This example shows how to manipulate the annotation creation toolbar to add your own
     * annotation tool items. In this example we add three line annotation tools, each of them for
     * drawing lines of different colors.
     */
    class AnnotationToolbarCustomItemsActivity :
        PdfActivity(),
        OnAnnotatingModeChangeListener {

        private lateinit var coordinator: ToolbarCoordinator

        /** Click subscription on the toolbar coordinator; closed in [onDestroy]. */
        private var actionSubscription: ToolbarSubscription? = null

        /** The annotating controller while annotation creation mode is active, `null` otherwise. */
        private var annotatingController: AnnotatingController? = null

        /**
         * The custom line tool variants keyed by their toolbar item id — the replacement for the
         * legacy `AnnotationToolbar.ItemToAnnotationToolMapper` mapping. All items map to
         * [AnnotationTool.LINE]; the variant selects the per-color editing defaults registered in
         * [onDocumentLoaded].
         */
        private val lineVariantsByItemId: Map<Int, AnnotationToolVariant> = mapOf(
            R.id.line_blue to AnnotationToolVariant.fromName(VARIANT_NAME_LINE_BLUE),
            R.id.line_red to AnnotationToolVariant.fromName(VARIANT_NAME_LINE_RED),
            R.id.line_yellow to AnnotationToolVariant.fromName(VARIANT_NAME_LINE_YELLOW),
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // The activity-scoped toolbar coordinator is the state container PdfActivity's Compose
            // toolbars run on — customizing the contextual (annotation) toolbar goes through it.
            coordinator = nutrientToolbarCoordinator()

            // Register the grouping rule that tells the toolbar how to group its menu items —
            // including where our custom items go. This uses the deprecated MenuItemGroupingRule
            // bridge so existing PresetMenuItemGroupingRule subclasses keep working; new code
            // should implement ContextualToolbarGroupingProvider and register it with
            // coordinator.setContextualToolbarGroupingProvider(...) instead.
            coordinator.setMenuItemGroupingRule(this, CustomAnnotationCreationToolbarGroupingRule(this))

            // The SDK rebuilds the annotation toolbar's item list every time the mode is entered,
            // so we observe the contextual toolbar state and (re-)append our custom items on every
            // show. This replaces the legacy onPrepareContextualToolbar + setMenuItems hook.
            // repeatOnLifecycle scopes the collection to the STARTED state, the standard pattern
            // for collecting flows from an activity.
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    coordinator.contextualToolbar.collect { state ->
                        if (state?.kind != ContextualToolbarKind.Annotation) return@collect
                        // Guarded so state updates after the initial show (e.g. our own selection
                        // sync below) don't add the items twice.
                        if (state.items.none { it.id == R.id.line_blue }) {
                            coordinator.updateContextualItems { items ->
                                items.addAll(0, customLineItems())
                            }
                            // The freshly (re-)added toggles start unselected; if a custom line
                            // variant is already active (e.g. the mode was restored with it),
                            // sync their selection right away.
                            annotatingController?.let(::syncCustomItemSelection)
                        }
                    }
                }
            }

            // Handle clicks on our custom items and enter annotation creation mode with the
            // matching tool variant. A direct tap on the item arrives as ItemClicked; a tap on a
            // group representing the item (or on the item inside an open submenu) arrives as
            // SubmenuChildClicked.
            actionSubscription = coordinator.addActionListener { action ->
                when (action) {
                    // Re-tapping the active tool in the bar deactivates it, mirroring the
                    // built-in tools.
                    is ToolbarAction.ItemClicked -> handleLineToolClick(action.id, allowToggleOff = true)

                    // From a submenu the tap re-selects the child as the group default instead of
                    // toggling the tool off — the same semantics as the built-in tools.
                    is ToolbarAction.SubmenuChildClicked -> handleLineToolClick(action.childId, allowToggleOff = false)

                    else -> Unit
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            actionSubscription?.close()
            actionSubscription = null
        }

        /**
         * Our three custom line items. Toggle items reflect a selected state, which we keep in
         * sync with the active tool in [syncCustomItemSelection].
         *
         * Note: the legacy `ItemToAnnotationToolMapper.isStyleIndicatorCircleEnabled` style
         * indicator circles are not available for custom items in the Compose toolbar — the
         * variant color is conveyed by the item icons instead.
         */
        private fun customLineItems(): List<ToolbarItem> = listOf(
            ToolbarItem.Toggle(
                actionId = "catalog.line_blue",
                id = R.id.line_blue,
                contentDescription = "Line: Blue",
                icon = ToolbarIcon.Resource(com.pspdfkit.R.drawable.pspdf__ic_line),
                position = ToolbarItem.ItemEdge.Start,
            ),
            ToolbarItem.Toggle(
                actionId = "catalog.line_red",
                id = R.id.line_red,
                contentDescription = "Line: Red",
                // The red line uses a customized colored drawable, so tinting must be disabled —
                // an Unspecified tint renders the drawable's own colors instead of the theme tint.
                icon = ToolbarIcon.Resource(R.drawable.ic_line_red, tint = ComposeColor.Unspecified),
                position = ToolbarItem.ItemEdge.Start,
            ),
            ToolbarItem.Toggle(
                actionId = "catalog.line_yellow",
                id = R.id.line_yellow,
                contentDescription = "Line: Yellow",
                icon = ToolbarIcon.Resource(com.pspdfkit.R.drawable.pspdf__ic_line),
                position = ToolbarItem.ItemEdge.Start,
            ),
        )

        /** Activates (or toggles off) the line tool variant bound to the tapped item. */
        private fun handleLineToolClick(itemId: Int, allowToggleOff: Boolean) {
            val variant = lineVariantsByItemId[itemId] ?: return
            val controller = annotatingController
            if (controller != null) {
                val isActive = controller.activeAnnotationTool == AnnotationTool.LINE &&
                    controller.activeAnnotationToolVariant == variant
                if (isActive && !allowToggleOff) return
                if (isActive) {
                    controller.changeAnnotationCreationMode(AnnotationTool.NONE, AnnotationToolVariant.defaultVariant())
                } else {
                    controller.changeAnnotationCreationMode(AnnotationTool.LINE, variant)
                }
            } else {
                pdfFragment?.enterAnnotatingMode(AnnotationTool.LINE, variant)
            }
        }

        override fun onDocumentLoaded(document: PdfDocument) {
            super.onDocumentLoaded(document)

            val pdfFragment = pdfFragment ?: return

            // Selection state of our custom items follows the fragment's annotation mode.
            pdfFragment.addOnAnnotatingModeChangeListener(this)

            // Here we set editing defaults for the items we're gonna create.
            val annotationConfiguration = pdfFragment.annotationConfiguration

            // Blue line configuration.
            annotationConfiguration.put(
                AnnotationTool.LINE,
                AnnotationToolVariant.fromName(VARIANT_NAME_LINE_BLUE),
                LineAnnotationConfiguration.builder(this) // Use preset configuration for creating lines.
                    .setDefaultColor(Color.BLUE) // Use blue as a default color.
                    .setAvailableColors(listOf(Color.BLUE)) // Only allow blue to be set.
                    .setForceDefaults(true) // Always force these defaults.
                    .setRestrictToAvailableColors(true)
                    .build(),
            )

            // Red line configuration.
            annotationConfiguration.put(
                AnnotationTool.LINE,
                AnnotationToolVariant.fromName(VARIANT_NAME_LINE_RED),
                LineAnnotationConfiguration.builder(this) // Use preset configuration for creating lines.
                    .setDefaultColor(Color.RED) // Use red as a default color.
                    .setAvailableColors(listOf(Color.RED)) // Only allow red to be set.
                    .setForceDefaults(true) // Always force these defaults.
                    .setRestrictToAvailableColors(true)
                    .build(),
            )

            // Yellow line configuration.
            annotationConfiguration.put(
                AnnotationTool.LINE,
                AnnotationToolVariant.fromName(VARIANT_NAME_LINE_YELLOW),
                LineAnnotationConfiguration.builder(this) // Use preset configuration for creating lines.
                    .setDefaultColor(Color.YELLOW) // Use yellow as a default color.
                    .setAvailableColors(listOf(Color.YELLOW)) // Only allow yellow to be set.
                    .setForceDefaults(true) // Always force these defaults.
                    .setRestrictToAvailableColors(true)
                    .build(),
            )
        }

        //region OnAnnotatingModeChangeListener — flips each custom item's selected state to match
        // the active tool/variant, the replacement for the legacy mapper-driven selection.
        override fun onEnterAnnotatingMode(controller: AnnotatingController) {
            annotatingController = controller
            syncCustomItemSelection(controller)
        }

        override fun onChangeAnnotatingMode(controller: AnnotatingController) {
            syncCustomItemSelection(controller)
        }

        override fun onExitAnnotatingMode(controller: AnnotatingController) {
            annotatingController = null
        }
        //endregion

        private fun syncCustomItemSelection(controller: AnnotatingController) {
            val activeVariant = controller.activeAnnotationToolVariant
                .takeIf { controller.activeAnnotationTool == AnnotationTool.LINE }
            coordinator.updateContextualItems { items ->
                items.map { item ->
                    val variant = lineVariantsByItemId[item.id] ?: return@map item
                    val selected = variant == activeVariant
                    if (item is ToolbarItem.Toggle && item.state.isSelected != selected) {
                        item.copy(state = item.state.copy(isSelected = selected))
                    } else {
                        item
                    }
                }.toPersistentList()
            }
        }

        /**
         * Class that implements the [PresetMenuItemGroupingRule], used to tell the toolbar how to
         * group menu items in the toolbar.
         *
         * @see com.pspdfkit.catalog.ui.CustomAnnotationCreationToolbarGroupingRule for more details.
         */
        private class CustomAnnotationCreationToolbarGroupingRule(context: Context) : PresetMenuItemGroupingRule(context) {

            private val customGrouping = listOf(
                // This adds our default markup items under our default markup group item.
                MenuItem(
                    com.pspdfkit.R.id.pspdf__annotation_toolbar_group_markup,
                    intArrayOf(
                        com.pspdfkit.R.id.pspdf__annotation_toolbar_item_highlight,
                        com.pspdfkit.R.id.pspdf__annotation_toolbar_item_squiggly,
                        com.pspdfkit.R.id.pspdf__annotation_toolbar_item_strikeout,
                        com.pspdfkit.R.id.pspdf__annotation_toolbar_item_underline,
                        com.pspdfkit.R.id.pspdf__annotation_toolbar_item_line,
                    ),
                ),
                // This adds line items under our group item ('writing' in this case). You can also
                // use your own but then you need to add them to the menu items.
                MenuItem(
                    com.pspdfkit.R.id.pspdf__annotation_toolbar_group_writing,
                    intArrayOf(R.id.line_blue, R.id.line_red, R.id.line_yellow),
                ),
                // Some standalone item from the default framework implementation.
                MenuItem(com.pspdfkit.R.id.pspdf__annotation_toolbar_item_ink_highlighter),
                // To access the property inspector, the color picker item needs to be added as well.
                MenuItem(com.pspdfkit.R.id.pspdf__annotation_toolbar_item_picker),
            )

            override fun getGroupPreset(capacity: Int, itemsCount: Int): List<MenuItem> {
                // Capacity shouldn't be less than 4. If that is the case, return empty list.
                if (capacity < MIN_TOOLBAR_CAPACITY) return ArrayList(capacity)

                // Return our custom groupings for all other capacities.
                return customGrouping
            }

            override fun areGeneratedGroupItemsSelectable(): Boolean = true
        }

        companion object {
            // Variants of the line tool that we will add.
            private const val VARIANT_NAME_LINE_BLUE = "blue_line"
            private const val VARIANT_NAME_LINE_RED = "red_line"
            private const val VARIANT_NAME_LINE_YELLOW = "yellow_line"
        }
    }
}
