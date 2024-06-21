/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.ui.CustomAnnotationEditingToolbarGroupingRule;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.rendering.AnnotationOverlayRenderStrategy;
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem;
import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Showcases how to enable overlay mode for annotations. */
public class AnnotationOverlayActivity extends PdfActivity
        implements ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener {

    /** Holds list of annotations that should be extracted to overlay. */
    @NonNull
    private final List<Annotation> overlaidAnnotations = new ArrayList<>();

    /** Current strategy used for rendering annotations in overlay. */
    @NonNull
    private AnnotationOverlayRenderStrategy.Strategy currentOverlayRenderingStrategy =
            AnnotationOverlayRenderStrategy.Strategy.AP_STREAM_RENDERING;

    /** Flag indicating whether annotation overlay is enabled. */
    private boolean annotationOverlayEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We'll set overlay render strategy that just returns currently configured strategy for all
        // annotations.
        requirePdfFragment().setAnnotationOverlayRenderStrategy(annotation -> currentOverlayRenderingStrategy);

        // We'll enable overlay for all supported annotation types immediately after activity
        // creation.
        enableOverlayForSupportedAnnotationTypes();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.toggle_annotation_overlay) {
            if (annotationOverlayEnabled) {
                disableOverlayForAllAnnotationTypes();
            } else {
                enableOverlayForSupportedAnnotationTypes();
            }
            invalidateOptionsMenu();
            return true;
        } else if (itemId == R.id.fire_low_memory_notification) { // We fire low memory notification manually
            // to showcase how annotation overlay mode
            // behaves when system is low on memory.
            requirePdfFragment().onLowMemory();
            return true;
        } else if (itemId == R.id.toggle_overlay_rendering_strategy) {
            // Toggle the current overlay rendering strategy.
            if (currentOverlayRenderingStrategy == AnnotationOverlayRenderStrategy.Strategy.AP_STREAM_RENDERING) {
                currentOverlayRenderingStrategy = AnnotationOverlayRenderStrategy.Strategy.PLATFORM_RENDERING;
            } else {
                currentOverlayRenderingStrategy = AnnotationOverlayRenderStrategy.Strategy.AP_STREAM_RENDERING;
            }
            // Invalidate options to change button text to current state.
            invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void disableOverlayForAllAnnotationTypes() {
        requirePdfFragment().setOverlaidAnnotationTypes(EnumSet.noneOf(AnnotationType.class));
        annotationOverlayEnabled = false;
    }

    private void enableOverlayForSupportedAnnotationTypes() {
        // Passing all annotation types enables overlay mode for all types that support overlay.
        requirePdfFragment().setOverlaidAnnotationTypes(EnumSet.allOf(AnnotationType.class));
        annotationOverlayEnabled = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register toolbar listener so we can add custom buttons for enabling/disabling overlay for
        // selected annotations.
        setOnContextualToolbarLifecycleListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setOnContextualToolbarLifecycleListener(null);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (annotationOverlayEnabled) {
            menu.add(0, R.id.toggle_annotation_overlay, 0, "Disable annotation overlay");
            // Toggling overlay strategy does not make sense when annotation overlay is disabled.
            if (currentOverlayRenderingStrategy == AnnotationOverlayRenderStrategy.Strategy.PLATFORM_RENDERING) {
                menu.add(0, R.id.toggle_overlay_rendering_strategy, 0, "Use AP stream rendering");
            } else {
                menu.add(0, R.id.toggle_overlay_rendering_strategy, 0, "Use platform rendering");
            }
        } else {
            menu.add(0, R.id.toggle_annotation_overlay, 0, "Enable annotation overlay");
        }
        menu.add(0, R.id.fire_low_memory_notification, 0, "Fire low memory notification");
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        List<Annotation> selectedAnnotations = requirePdfFragment().getSelectedAnnotations();
        if (selectedAnnotations.size() != 1) return;

        if (overlaidAnnotations.contains(selectedAnnotations.get(0))) {
            menu.add(0, R.id.toggle_annotation_overlay, 0, "Disable overlay");
        } else {
            menu.add(0, R.id.toggle_annotation_overlay, 0, "Enable overlay");
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (requirePdfFragment().getSelectedAnnotations().size() != 1) {
            return super.onContextItemSelected(item);
        }
        final Annotation annotation =
                requirePdfFragment().getSelectedAnnotations().get(0);

        final int itemId = item.getItemId();
        if (itemId == R.id.toggle_annotation_overlay) {
            if (overlaidAnnotations.contains(annotation)) {
                overlaidAnnotations.remove(annotation);
                requirePdfFragment().setOverlaidAnnotations(overlaidAnnotations);
            } else {
                overlaidAnnotations.add(annotation);
                requirePdfFragment().setOverlaidAnnotations(overlaidAnnotations);
            }
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onPrepareContextualToolbar(@NonNull ContextualToolbar toolbar) {
        // Add item to annotation editing toolbar that shows our context menu for toggling
        // overlay for selected annotation if the annotation overlay is disabled.
        if (annotationOverlayEnabled) return;
        if (toolbar instanceof AnnotationEditingToolbar) {
            if (requirePdfFragment().getSelectedAnnotations().size() != 1) return;

            // Set custom grouping rule for our extended editing toolbar.
            toolbar.setMenuItemGroupingRule(new CustomAnnotationEditingToolbarGroupingRule(this));

            // Get the existing menu items so we can add our item later.
            final List<ContextualToolbarMenuItem> menuItems = ((AnnotationEditingToolbar) toolbar).getMenuItems();

            // Create custom menu item.
            final ContextualToolbarMenuItem customItem = ContextualToolbarMenuItem.createSingleItem(
                    this,
                    R.id.pspdf_menu_custom,
                    ContextCompat.getDrawable(this, R.drawable.ic_settings),
                    "Annotation Overlay",
                    Color.WHITE,
                    Color.WHITE,
                    ContextualToolbarMenuItem.Position.END,
                    false);
            // Registers a context menu to be shown for the custom item.
            registerForContextMenu(customItem);

            // Add the custom item to our toolbar.
            menuItems.add(customItem);
            toolbar.setMenuItems(menuItems);

            // Add a click listener so we can handle clicks on custom item.
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
    public void onDisplayContextualToolbar(@NonNull ContextualToolbar toolbar) {
        // no-op
    }

    @Override
    public void onRemoveContextualToolbar(@NonNull ContextualToolbar toolbar) {
        // no-op
    }
}
