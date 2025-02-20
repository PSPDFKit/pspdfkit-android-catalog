/*
 *   Copyright © 2022-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.activities

import android.content.Context
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationFlags
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.annotations.actions.HideAction
import com.pspdfkit.annotations.configuration.StampAnnotationConfiguration
import com.pspdfkit.annotations.stamps.StampPickerItem
import com.pspdfkit.catalog.R
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.special_mode.controller.AnnotationEditingController
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController
import com.pspdfkit.ui.special_mode.manager.AnnotationManager
import com.pspdfkit.ui.toolbar.AnnotationCreationToolbar
import com.pspdfkit.ui.toolbar.ContextualToolbar
import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout
import com.pspdfkit.ui.toolbar.grouping.presets.MenuItem
import com.pspdfkit.ui.toolbar.grouping.presets.PresetMenuItemGroupingRule
import java.util.EnumSet

class ConstructionExampleActivity :
    PdfActivity(),
    ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener,
    AnnotationManager.OnAnnotationSelectedListener,
    AnnotationManager.OnAnnotationEditingModeChangeListener {

    var annotationsHidden = false
    val desiredAnnotationTypes = EnumSet.allOf(AnnotationType::class.java).apply {
        // ignore LINK annotations
        remove(AnnotationType.LINK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // First we set the listener for the toolbar lifecycle changes so we
        // can observe when the toolbar is being prepared, shown or dismissed
        // inside the activity's UI.
        setOnContextualToolbarLifecycleListener(this)
    }

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)

        // Check if we have any hidden annotation. If so, we should show the "show" item in the options menu
        val hasHiddenAnnotations = document.annotationProvider.getAllAnnotationsOfType(desiredAnnotationTypes).firstOrNull {
            it.hasFlag(AnnotationFlags.HIDDEN)
        } != null

        if (hasHiddenAnnotations != annotationsHidden) {
            annotationsHidden = hasHiddenAnnotations
            invalidateOptionsMenu()
        }

        setupPinStamp()
    }

    /**
     * Create a custom configuration for Stamp annotations which creates a Pin annotation.
     * This can be achieved by setting a single StampPickerItem (which visualizes a pin in our case) to the configuration,
     * which leads to a behavior change in the annotation creation. Instead of showing a stamp picker dialog, the stamp
     * annotation is created immediatly from this only item.
     */
    private fun setupPinStamp() {
        val fragment = requirePdfFragment()

        val pinBitmap =
            AppCompatResources.getDrawable(this, R.drawable.ic_pin_drop)?.toBitmap() ?: return

        // First we create our custom StampPickerItem
        val pinStamp = StampPickerItem.fromBitmap(pinBitmap).withSize(50f, 50f)
            .build()

        // Then put it into a StampAnnotationConfiguration
        val stampConfig = StampAnnotationConfiguration.builder(this)
            // Here we return list of stamp picker items that are going to be available in the stamp picker.
            .setAvailableStampPickerItems(listOf(pinStamp))
            .build()

        // Replace the default StampAnnotationConfiguration with our custom one
        fragment
            .annotationConfiguration
            .put(AnnotationType.STAMP, stampConfig)

        // When creating such a pin stamp we always want to add a note to it.
        // To spare the user the extra click for adding a note, we invoke the NoteEditor automatically
        // Therefore we need two listeners which inform us when an annotation is created and when
        // the editing mode is invoked (both events happen right one after the other)
        fragment.addOnAnnotationSelectedListener(this)
        fragment.addOnAnnotationEditingModeChangeListener(this)
    }

    override fun onGenerateMenuItemIds(menuItems: MutableList<Int>): MutableList<Int> {
        // Generate our custom menu item ids.
        menuItems.add(0, R.id.custom_action_hide)
        menuItems.add(1, R.id.custom_action_show)
        menuItems.removeAt(menuItems.indexOf(MENU_OPTION_THUMBNAIL_GRID))
        return menuItems
    }

    // add show/hide annotation items to options menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        // Let's say we want to tint icons same as the default ones. We can read the color
        // from the theme, or specify the same color we have in theme. Reading from theme is a bit
        // more complex but a better way to do it, so here's how to:
        val a = theme
            .obtainStyledAttributes(
                null,
                com.pspdfkit.R.styleable.pspdf__ActionBarIcons,
                com.pspdfkit.R.attr.pspdf__actionBarIconsStyle,
                com.pspdfkit.R.style.PSPDFKit_ActionBarIcons
            )
        val mainToolbarIconsColor = a.getColor(
            com.pspdfkit.R.styleable.pspdf__ActionBarIcons_pspdf__iconsColor,
            ContextCompat.getColor(this, R.color.white)
        )
        a.recycle()

        // setup our custom menu items for showing and hiding annotations
        setupCustomMenuItem(
            menu.findItem(R.id.custom_action_hide),
            "Hide",
            R.drawable.ic_hide,
            mainToolbarIconsColor
        )
        setupCustomMenuItem(
            menu.findItem(R.id.custom_action_show),
            "Show",
            R.drawable.ic_show,
            mainToolbarIconsColor
        )

        return true
    }

    private fun setupCustomMenuItem(item: android.view.MenuItem?, title: String, iconRes: Int, mainToolbarIconsColor: Int) {
        if (item == null) return
        item.title = title
        item.setIcon(iconRes)
        item.icon?.let { DrawableCompat.setTint(it, mainToolbarIconsColor) }
        item.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val result = super.onPrepareOptionsMenu(menu)

        // we only want to show either of our custom menu items, depending on the visible/hidden state of our annotations
        menu.findItem(R.id.custom_action_hide)?.let {
            it.isVisible = !annotationsHidden
        }
        menu.findItem(R.id.custom_action_show)?.let {
            it.isVisible = annotationsHidden
        }
        return result
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            R.id.custom_action_hide,
            R.id.custom_action_show -> {
                // show/hide annotations accordingly and update the option menu
                annotationsHidden = !annotationsHidden
                setAnnotationVisibility(annotationsHidden)
                invalidateOptionsMenu()
                return true
            }

            else ->
                return super.onOptionsItemSelected(item)
        }
    }

    // add or remove the HIDDEN flag to the annotations to achieve the hide/show effect
    private fun setAnnotationVisibility(shouldHide: Boolean) {
        val doc = document ?: return
        val annotations = doc
            .annotationProvider
            .getAllAnnotationsOfType(desiredAnnotationTypes)

        pdfFragment?.executeAction(HideAction(annotations, null, shouldHide))
    }

    //region OnContextualToolbarLifecycleListener
    override fun onPrepareContextualToolbar(toolbar: ContextualToolbar<*>) {
        if (toolbar is AnnotationCreationToolbar) {
            // Register grouping rule to tell toolbar how to group menu items.
            toolbar.setMenuItemGroupingRule(
                CustomAnnotationCreationToolbarGroupingRule(this)
            )
        }
    }

    override fun onDisplayContextualToolbar(toolbar: ContextualToolbar<*>) {}
    override fun onRemoveContextualToolbar(toolbar: ContextualToolbar<*>) {}
    //endregion

    private var newPinAnnotationCreated = false

    //region OnAnnotationSelectedListener
    override fun onPrepareAnnotationSelection(
        controller: AnnotationSelectionController,
        annotation: Annotation,
        annotationCreated: Boolean
    ): Boolean {
        // since our custom pin is the only stamp that we create in this example we can just use the type for detection
        newPinAnnotationCreated = annotationCreated && annotation.type == AnnotationType.STAMP
        return true
    }

    override fun onAnnotationSelected(annotation: Annotation, annotationCreated: Boolean) {}
    //endregion

    //region OnAnnotationEditingModeChangeListener
    // this method is called directly after create/selection
    override fun onEnterAnnotationEditingMode(controller: AnnotationEditingController) {
        // in case the last annotation selection was in context with a pin creation, invoke the note editor
        if (newPinAnnotationCreated) {
            controller.currentSingleSelectedAnnotation?.let {
                controller.showAnnotationEditor(it)
            }
        }
    }

    override fun onChangeAnnotationEditingMode(controller: AnnotationEditingController) {}
    override fun onExitAnnotationEditingMode(controller: AnnotationEditingController) {}
    //endregion

    /**
     * Class that implements the [PresetMenuItemGroupingRule], used to tell the toolbar how to
     * group menu items in the toolbar.
     *
     * @see com.pspdfkit.catalog.ui.CustomAnnotationCreationToolbarGroupingRule for more details.
     */
    private class CustomAnnotationCreationToolbarGroupingRule constructor(context: Context) : PresetMenuItemGroupingRule(context) {
        companion object {
            private const val LOW_CAPACITY_ITEMS_COUNT = 5
            private const val HIGH_CAPACITY_ITEMS_COUNT = 7
        }
        private val LOW_CAPACITY_ITEMS_GROUPING: MutableList<MenuItem> = ArrayList(LOW_CAPACITY_ITEMS_COUNT)
        private val HIGH_CAPACITY_ITEMS_GROUPING: MutableList<MenuItem> = ArrayList(HIGH_CAPACITY_ITEMS_COUNT)
        override fun getGroupPreset(capacity: Int, itemsCount: Int): List<MenuItem> =
            when {
                capacity >= HIGH_CAPACITY_ITEMS_COUNT -> HIGH_CAPACITY_ITEMS_GROUPING
                capacity >= LOW_CAPACITY_ITEMS_COUNT -> LOW_CAPACITY_ITEMS_GROUPING
                // in case we don't have enough capacity, return an empty list
                else -> ArrayList(capacity)
            }

        override fun areGeneratedGroupItemsSelectable(): Boolean {
            return true
        }

        init {
            LOW_CAPACITY_ITEMS_GROUPING.addAll(
                listOf(
                    MenuItem(
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_group_drawing,
                        intArrayOf(
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_line,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_line_arrow,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_ink_pen,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_magic_ink,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_freetext,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_freetext_callout,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_note,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_measurement_distance,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_measurement_perimeter,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_measurement_area_polygon,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_measurement_area_rect,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_measurement_area_ellipse,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_measurement_scale_calibration
                        )
                    ),
                    MenuItem(
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_group_markup,
                        intArrayOf(
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_cloudy_square,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_cloudy_circle,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_dashed_square,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_dashed_circle,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_square,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_circle
                        )
                    ),
                    MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_image),
                    MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_stamp),
                    MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_picker),
                    MenuItem(
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_group_undo_redo,
                        intArrayOf(
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_undo,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_redo
                        )
                    )
                )
            )
            HIGH_CAPACITY_ITEMS_GROUPING.addAll(
                listOf(
                    MenuItem(
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_group_measurement,
                        intArrayOf(
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_measurement_distance,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_measurement_perimeter,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_measurement_area_polygon,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_measurement_area_rect,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_measurement_area_ellipse,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_measurement_scale_calibration
                        )
                    ),
                    MenuItem(
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_group_drawing,
                        intArrayOf(
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_line,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_line_arrow,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_ink_pen,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_magic_ink
                        )
                    ),
                    MenuItem(
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_group_markup,
                        intArrayOf(
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_cloudy_square,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_cloudy_circle,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_dashed_square,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_dashed_circle,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_square,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_circle
                        )
                    ),
                    MenuItem(
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_group_writing,
                        intArrayOf(
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_freetext,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_freetext_callout,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_note
                        )
                    ),
                    MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_image),
                    MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_stamp),
                    MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_picker),
                    MenuItem(
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_group_undo_redo,
                        intArrayOf(
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_undo,
                            com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_redo
                        )
                    )
                )
            )
        }
    }
}
