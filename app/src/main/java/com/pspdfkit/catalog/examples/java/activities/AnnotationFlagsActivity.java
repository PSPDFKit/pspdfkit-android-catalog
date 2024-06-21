/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.graphics.Color;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.AnnotationFlags;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.ui.CustomAnnotationEditingToolbarGroupingRule;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem;
import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout;
import java.util.EnumSet;
import java.util.List;

/** This example showcases supported {@link com.pspdfkit.annotations.AnnotationFlags}. */
public class AnnotationFlagsActivity extends PdfActivity
        implements ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener {

    @Override
    protected void onPause() {
        super.onPause();
        setOnContextualToolbarLifecycleListener(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setOnContextualToolbarLifecycleListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, R.id.reset_all_flags, 0, "Reset All Flags");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.reset_all_flags) {
            setFlagsOnAllAnnotations(EnumSet.of(AnnotationFlags.PRINT, AnnotationFlags.NOZOOM));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // In this example we handle only single annotation selection.
        if (getPdfFragment().getSelectedAnnotations().size() != 1) return;
        final Annotation annotation = getPdfFragment().getSelectedAnnotations().get(0);

        // Populate contextual menu.
        getMenuInflater().inflate(R.menu.flags_example, menu);

        menu.findItem(R.id.toggle_read_only_flag)
                .setTitle(
                        !annotation.hasFlag(AnnotationFlags.READONLY)
                                ? "Enable Read-Only Flag"
                                : "Disable Read-Only Flag");

        menu.findItem(R.id.toggle_hidden_flag)
                .setTitle(!annotation.hasFlag(AnnotationFlags.HIDDEN) ? "Enable Hidden Flag" : "Disable Hidden Flag");

        menu.findItem(R.id.toggle_print_flag)
                .setTitle(!annotation.hasFlag(AnnotationFlags.PRINT) ? "Enable Print Flag" : "Disable Print Flag");

        menu.findItem(R.id.toggle_no_view_flag)
                .setTitle(!annotation.hasFlag(AnnotationFlags.NOVIEW) ? "Enable No-View Flag" : "Disable No-View Flag");

        menu.findItem(R.id.toggle_locked_flag)
                .setTitle(!annotation.hasFlag(AnnotationFlags.LOCKED) ? "Enable Locked Flag" : "Disable Locked Flag");

        menu.findItem(R.id.toggle_locked_contents_flag)
                .setTitle(
                        !annotation.hasFlag(AnnotationFlags.LOCKEDCONTENTS)
                                ? "Enable Locked-Contents Flag"
                                : "Disable Locked-Contents Flag");

        // No-zoom flag is supported only for Note, File and Stamp, and free-text annotations (but not callouts).
        MenuItem noZoomMenuItem = menu.findItem(R.id.toggle_no_zoom_flag);
        if (annotation.getType() == AnnotationType.NOTE
                || annotation.getType() == AnnotationType.FREETEXT
                || annotation.getType() == AnnotationType.FILE
                || annotation.getType() == AnnotationType.STAMP) {
            noZoomMenuItem.setVisible(true);
            noZoomMenuItem.setTitle(
                    !annotation.hasFlag(AnnotationFlags.NOZOOM) ? "Enable No-Zoom Flag" : "Disable No-Zoom Flag");
        } else {
            noZoomMenuItem.setVisible(false);
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        // In this example we handle only single annotation selection.
        if (getPdfFragment().getSelectedAnnotations().size() != 1) return super.onContextItemSelected(item);
        final Annotation annotation = getPdfFragment().getSelectedAnnotations().get(0);

        final int itemId = item.getItemId();
        if (itemId == R.id.reset_all_flags) {
            annotation.setFlags(EnumSet.of(AnnotationFlags.PRINT, AnnotationFlags.NOZOOM));
            getPdfFragment().notifyAnnotationHasChanged(annotation);
            return true;
        } else if (itemId == R.id.toggle_read_only_flag) {
            toggleAnnotationFlag(annotation, AnnotationFlags.READONLY);
            return true;
        } else if (itemId == R.id.toggle_hidden_flag) {
            toggleAnnotationFlag(annotation, AnnotationFlags.HIDDEN);
            return true;
        } else if (itemId == R.id.toggle_print_flag) {
            toggleAnnotationFlag(annotation, AnnotationFlags.PRINT);
            return true;
        } else if (itemId == R.id.toggle_no_view_flag) {
            toggleAnnotationFlag(annotation, AnnotationFlags.NOVIEW);
            return true;
        } else if (itemId == R.id.toggle_locked_flag) {
            toggleAnnotationFlag(annotation, AnnotationFlags.LOCKED);
            return true;
        } else if (itemId == R.id.toggle_locked_contents_flag) {
            toggleAnnotationFlag(annotation, AnnotationFlags.LOCKEDCONTENTS);
            return true;
        } else if (itemId == R.id.toggle_no_zoom_flag) {
            toggleAnnotationFlag(annotation, AnnotationFlags.NOZOOM);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onPrepareContextualToolbar(@NonNull ContextualToolbar toolbar) {
        // Add item to annotation editing toolbar that will show our context menu for toggling flags
        // on selected annotation.
        if (toolbar instanceof AnnotationEditingToolbar) {
            // In this example we handle only single annotation selection.
            if (getPdfFragment().getSelectedAnnotations().size() != 1) return;

            // Set custom grouping rule for our extended editing toolbar.
            toolbar.setMenuItemGroupingRule(new CustomAnnotationEditingToolbarGroupingRule(this));

            // Get the existing menu items so we can add our item later.
            final List<ContextualToolbarMenuItem> menuItems = ((AnnotationEditingToolbar) toolbar).getMenuItems();

            // Create our custom menu item.
            final ContextualToolbarMenuItem customItem = ContextualToolbarMenuItem.createSingleItem(
                    this,
                    R.id.pspdf_menu_custom,
                    ContextCompat.getDrawable(this, R.drawable.ic_settings),
                    "Annotation flags",
                    Color.MAGENTA,
                    Color.CYAN,
                    ContextualToolbarMenuItem.Position.END,
                    false);
            // Register it to for context menu.
            registerForContextMenu(customItem);

            // Tell the toolbar about our new item.
            menuItems.add(customItem);
            toolbar.setMenuItems(menuItems);

            // Add a listener so we can handle clicking on our item.
            toolbar.setOnMenuItemClickListener((toolbar1, menuItem) -> {
                if (menuItem.getId() == R.id.pspdf_menu_custom) {
                    menuItem.showContextMenu();
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void onDisplayContextualToolbar(@NonNull ContextualToolbar toolbar) {}

    @Override
    public void onRemoveContextualToolbar(@NonNull ContextualToolbar toolbar) {}

    /** Sets/unsets {@link AnnotationFlags} on single annotation. */
    private void setFlagOnAnnotation(
            @NonNull final Annotation annotation, @NonNull final AnnotationFlags flag, final boolean isSet) {
        final EnumSet<AnnotationFlags> flags = annotation.getFlags();
        if (isSet) {
            flags.add(flag);
        } else {
            flags.remove(flag);
        }
        annotation.setFlags(flags);
        getPdfFragment().notifyAnnotationHasChanged(annotation);
    }

    /** Set flags on all annotations in document. */
    private void setFlagsOnAllAnnotations(@NonNull final EnumSet<AnnotationFlags> flags) {
        getDocument()
                .getAnnotationProvider()
                .getAllAnnotationsOfTypeAsync(EnumSet.allOf(AnnotationType.class))
                .subscribe(annotation -> {
                    annotation.setFlags(flags);
                    getPdfFragment().notifyAnnotationHasChanged(annotation);
                });
    }

    /** Toggles single annotation flag. */
    private void toggleAnnotationFlag(@NonNull Annotation annotation, @NonNull final AnnotationFlags flag) {
        setFlagOnAnnotation(annotation, flag, !annotation.hasFlag(flag));
    }
}
