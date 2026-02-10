/*
 *   Copyright © 2017-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationFlags
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.ui.CustomAnnotationEditingToolbarGroupingRule
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar
import com.pspdfkit.ui.toolbar.ContextualToolbar
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem
import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.EnumSet

/**
 * Shows how to use [com.pspdfkit.annotations.AnnotationFlags] to modify annotation
 * characteristics.
 */
class AnnotationFlagsExample(context: Context) : SdkExample(
    context,
    R.string.annotationFlagsExampleTitle,
    R.string.annotationFlagsExampleDescription
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        configuration
            // Turn off saving, so we have the clean original document every time the example is
            // launched.
            .autosaveEnabled(false)
            // Notes are treated as if they had the NOZOOM flag set by default.
            // We override this behavior here by enabling NOZOOM flag handling for note
            // annotations.
            .setEnableNoteAnnotationNoZoomHandling(true)

        // Extract the document from the assets.
        ExtractAssetTask.extract(ANNOTATIONS_EXAMPLE, title, context) { documentFile ->
            // To start the AnnotationFlagsActivity create a launch intent using the
            // builder.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(AnnotationFlagsActivity::class.java)
                .build()

            context.startActivity(intent)
        }
    }
}

/** This example showcases supported [AnnotationFlags]. */
class AnnotationFlagsActivity : PdfActivity(), ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener {

    override fun onPause() {
        super.onPause()
        setOnContextualToolbarLifecycleListener(null)
    }

    override fun onResume() {
        super.onResume()
        setOnContextualToolbarLifecycleListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, R.id.reset_all_flags, 0, "Reset All Flags")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.reset_all_flags) {
            setFlagsOnAllAnnotations(EnumSet.of(AnnotationFlags.PRINT, AnnotationFlags.NOZOOM))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)

        // In this example we handle only single annotation selection.
        val fragment = pdfFragment ?: return
        if (fragment.selectedAnnotations.size != 1) return
        val annotation = fragment.selectedAnnotations[0]

        // Populate contextual menu.
        menuInflater.inflate(R.menu.flags_example, menu)

        menu.findItem(R.id.toggle_read_only_flag).title =
            if (!annotation.hasFlag(AnnotationFlags.READONLY)) "Enable Read-Only Flag" else "Disable Read-Only Flag"

        menu.findItem(R.id.toggle_hidden_flag).title =
            if (!annotation.hasFlag(AnnotationFlags.HIDDEN)) "Enable Hidden Flag" else "Disable Hidden Flag"

        menu.findItem(R.id.toggle_print_flag).title =
            if (!annotation.hasFlag(AnnotationFlags.PRINT)) "Enable Print Flag" else "Disable Print Flag"

        menu.findItem(R.id.toggle_no_view_flag).title =
            if (!annotation.hasFlag(AnnotationFlags.NOVIEW)) "Enable No-View Flag" else "Disable No-View Flag"

        menu.findItem(R.id.toggle_locked_flag).title =
            if (!annotation.hasFlag(AnnotationFlags.LOCKED)) "Enable Locked Flag" else "Disable Locked Flag"

        menu.findItem(R.id.toggle_locked_contents_flag).title =
            if (!annotation.hasFlag(AnnotationFlags.LOCKEDCONTENTS)) "Enable Locked-Contents Flag" else "Disable Locked-Contents Flag"

        // No-zoom flag is supported only for Note, File and Stamp, and free-text annotations (but not callouts).
        val noZoomMenuItem = menu.findItem(R.id.toggle_no_zoom_flag)
        if (annotation.type == AnnotationType.NOTE ||
            annotation.type == AnnotationType.FREETEXT ||
            annotation.type == AnnotationType.FILE ||
            annotation.type == AnnotationType.STAMP
        ) {
            noZoomMenuItem.isVisible = true
            noZoomMenuItem.title =
                if (!annotation.hasFlag(AnnotationFlags.NOZOOM)) "Enable No-Zoom Flag" else "Disable No-Zoom Flag"
        } else {
            noZoomMenuItem.isVisible = false
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        // In this example we handle only single annotation selection.
        val fragment = pdfFragment ?: return super.onContextItemSelected(item)
        if (fragment.selectedAnnotations.size != 1) return super.onContextItemSelected(item)
        val annotation = fragment.selectedAnnotations[0]

        return when (item.itemId) {
            R.id.reset_all_flags -> {
                annotation.flags = EnumSet.of(AnnotationFlags.PRINT, AnnotationFlags.NOZOOM)
                true
            }
            R.id.toggle_read_only_flag -> {
                toggleAnnotationFlag(annotation, AnnotationFlags.READONLY)
                true
            }
            R.id.toggle_hidden_flag -> {
                toggleAnnotationFlag(annotation, AnnotationFlags.HIDDEN)
                true
            }
            R.id.toggle_print_flag -> {
                toggleAnnotationFlag(annotation, AnnotationFlags.PRINT)
                true
            }
            R.id.toggle_no_view_flag -> {
                toggleAnnotationFlag(annotation, AnnotationFlags.NOVIEW)
                true
            }
            R.id.toggle_locked_flag -> {
                toggleAnnotationFlag(annotation, AnnotationFlags.LOCKED)
                true
            }
            R.id.toggle_locked_contents_flag -> {
                toggleAnnotationFlag(annotation, AnnotationFlags.LOCKEDCONTENTS)
                true
            }
            R.id.toggle_no_zoom_flag -> {
                toggleAnnotationFlag(annotation, AnnotationFlags.NOZOOM)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onPrepareContextualToolbar(toolbar: ContextualToolbar<*>) {
        // Add item to annotation editing toolbar that will show our context menu for toggling flags
        // on selected annotation.
        if (toolbar is AnnotationEditingToolbar) {
            // In this example we handle only single annotation selection.
            val fragment = pdfFragment ?: return
            if (fragment.selectedAnnotations.size != 1) return

            // Set custom grouping rule for our extended editing toolbar.
            toolbar.setMenuItemGroupingRule(CustomAnnotationEditingToolbarGroupingRule(this))

            // Get the existing menu items so we can add our item later.
            val menuItems = toolbar.menuItems

            // Create our custom menu item.
            val settingsDrawable: Drawable = ContextCompat.getDrawable(this, R.drawable.ic_settings)
                ?: return
            val customItem = ContextualToolbarMenuItem.createSingleItem(
                this,
                R.id.pspdf_menu_custom,
                settingsDrawable,
                "Annotation flags",
                Color.MAGENTA,
                Color.CYAN,
                ContextualToolbarMenuItem.Position.END,
                false
            )
            // Register it to for context menu.
            registerForContextMenu(customItem)

            // Tell the toolbar about our new item.
            menuItems.add(customItem)
            toolbar.setMenuItems(menuItems)

            // Add a listener so we can handle clicking on our item.
            toolbar.setOnMenuItemClickListener { _, menuItem ->
                if (menuItem.id == R.id.pspdf_menu_custom) {
                    menuItem.showContextMenu()
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun onDisplayContextualToolbar(toolbar: ContextualToolbar<*>) {}

    override fun onRemoveContextualToolbar(toolbar: ContextualToolbar<*>) {}

    /** Sets/unsets [AnnotationFlags] on single annotation. */
    private fun setFlagOnAnnotation(annotation: Annotation, flag: AnnotationFlags, isSet: Boolean) {
        val flags = annotation.flags
        if (isSet) {
            flags.add(flag)
        } else {
            flags.remove(flag)
        }
        annotation.flags = flags
    }

    /** Set flags on all annotations in document. */
    private fun setFlagsOnAllAnnotations(newFlags: EnumSet<AnnotationFlags>) {
        val doc = document ?: return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val annotations = doc.annotationProvider.getAllAnnotationsOfType(
                    EnumSet.allOf(AnnotationType::class.java)
                )
                annotations.forEach { annotation -> annotation.flags = newFlags }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to set flags on annotations", e)
            }
        }
    }

    /** Toggles single annotation flag. */
    private fun toggleAnnotationFlag(annotation: Annotation, flag: AnnotationFlags) {
        setFlagOnAnnotation(annotation, flag, !annotation.hasFlag(flag))
    }

    companion object {
        private const val LOG_TAG = "AnnotationFlagsActivity"
    }
}
