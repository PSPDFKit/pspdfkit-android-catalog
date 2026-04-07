/*
 *   Copyright © 2018-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import com.pspdfkit.ui.annotations.OnAnnotationSelectedListener;
import com.pspdfkit.ui.rendering.AnnotationOverlayRenderStrategy;
import java.util.EnumSet;
import java.util.List;

/** Showcases how to display annotations in overlay mode. */
public class AnnotationOverlayExample extends SdkExample {

    public AnnotationOverlayExample(@NonNull final Context context) {
        super(context, R.string.annotationOverlayExampleTitle, R.string.annotationOverlayExampleDescription);
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // Use custom layout with FAB for per-annotation overlay toggle.
        configuration.layout(R.layout.activity_annotation_overlay);

        // Extract the document from the assets.
        ExtractAssetTask.extract(ANNOTATIONS_EXAMPLE, getTitle(), context, documentFile -> {
            // To start the example create a launch intent using the builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(AnnotationOverlayActivity.class)
                    .build();

            context.startActivity(intent);
        });
    }

    /** Showcases how to enable overlay mode for annotations. */
    public static class AnnotationOverlayActivity extends PdfActivity implements OnAnnotationSelectedListener {

        /**
         * Holds per-type overlay overrides. When overlay is globally off, these types are
         * included in overlay. When overlay is globally on, these types are excluded from overlay.
         */
        @NonNull
        private final EnumSet<AnnotationType> perTypeOverrides = EnumSet.noneOf(AnnotationType.class);

        /** Current strategy used for rendering annotations in overlay. */
        @NonNull
        private AnnotationOverlayRenderStrategy.Strategy currentOverlayRenderingStrategy =
                AnnotationOverlayRenderStrategy.Strategy.AP_STREAM_RENDERING;

        /** Flag indicating whether annotation overlay is enabled. */
        private boolean annotationOverlayEnabled = false;

        /** FAB for toggling overlay on selected annotations. */
        private FloatingActionButton overlayFab;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Set up the FAB for toggling overlay on selected annotation types.
            overlayFab = findViewById(R.id.fab_toggle_annotation_overlay);
            registerForContextMenu(overlayFab);
            overlayFab.setOnClickListener(View::showContextMenu);

            // Listen for annotation selection changes to show/hide the FAB.
            requirePdfFragment().addOnAnnotationSelectedListener(this);

            // We'll set overlay render strategy that just returns currently configured strategy for all
            // annotations.
            requirePdfFragment().setAnnotationOverlayRenderStrategy(annotation -> currentOverlayRenderingStrategy);

            // We'll enable overlay for all supported annotation types immediately after activity
            // creation.
            enableOverlayForSupportedAnnotationTypes();
            invalidateOptionsMenu();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            requirePdfFragment().removeOnAnnotationSelectedListener(this);
        }

        @Override
        public void onAnnotationSelected(@NonNull Annotation annotation, boolean annotationCreated) {
            overlayFab.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnnotationDeselected(@NonNull Annotation annotation, boolean reselected) {
            if (!reselected) {
                overlayFab.setVisibility(View.GONE);
            }
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
            annotationOverlayEnabled = false;
            perTypeOverrides.clear();
            applyOverlayTypes();
        }

        private void enableOverlayForSupportedAnnotationTypes() {
            annotationOverlayEnabled = true;
            perTypeOverrides.clear();
            applyOverlayTypes();
        }

        private void applyOverlayTypes() {
            final EnumSet<AnnotationType> overlayTypes;
            if (annotationOverlayEnabled) {
                // Start with all types, then remove manually excluded ones.
                overlayTypes = EnumSet.allOf(AnnotationType.class);
                overlayTypes.removeAll(perTypeOverrides);
            } else {
                // Start with none, then add manually included ones.
                overlayTypes = EnumSet.noneOf(AnnotationType.class);
                overlayTypes.addAll(perTypeOverrides);
            }
            requirePdfFragment().setOverlaidAnnotationTypes(overlayTypes);
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

            final AnnotationType annotationType = selectedAnnotations.get(0).getType();
            // The effective overlay state for this type is: globally on XOR overridden.
            final boolean effectivelyOverlaid = annotationOverlayEnabled ^ perTypeOverrides.contains(annotationType);
            menu.add(0, R.id.toggle_annotation_overlay, 0, effectivelyOverlaid ? "Disable overlay" : "Enable overlay");
        }

        @Override
        public boolean onContextItemSelected(@NonNull MenuItem item) {
            if (requirePdfFragment().getSelectedAnnotations().size() != 1) {
                return super.onContextItemSelected(item);
            }
            final Annotation annotation =
                    requirePdfFragment().getSelectedAnnotations().get(0);
            final AnnotationType annotationType = annotation.getType();

            final int itemId = item.getItemId();
            if (itemId == R.id.toggle_annotation_overlay) {
                if (perTypeOverrides.contains(annotationType)) {
                    perTypeOverrides.remove(annotationType);
                } else {
                    perTypeOverrides.add(annotationType);
                }
                applyOverlayTypes();
                return true;
            }
            return super.onContextItemSelected(item);
        }
    }
}
