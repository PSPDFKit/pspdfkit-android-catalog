/*
 *   Copyright © 2014-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import static com.pspdfkit.catalog.utils.Utils.dpToPx;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.annotations.defaults.AnnotationPreferencesManager;
import com.pspdfkit.catalog.R;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.inspector.PropertyInspector;
import com.pspdfkit.ui.inspector.PropertyInspectorView;
import com.pspdfkit.ui.inspector.annotation.AnnotationCreationInspectorController;
import com.pspdfkit.ui.inspector.annotation.DefaultAnnotationEditingInspectorController;
import com.pspdfkit.ui.inspector.views.ColorPickerInspectorDetailView;
import com.pspdfkit.ui.inspector.views.PropertyInspectorDividerDecoration;
import com.pspdfkit.ui.inspector.views.SliderPickerInspectorView;
import com.pspdfkit.ui.special_mode.controller.AnnotationCreationController;
import com.pspdfkit.ui.special_mode.controller.AnnotationInspectorController;
import com.pspdfkit.ui.special_mode.controller.AnnotationTool;
import com.pspdfkit.ui.special_mode.controller.AnnotationToolVariant;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows how to implement custom {@link AnnotationInspectorController} and how to add custom views
 * to existing annotation inspector.
 */
public class CustomAnnotationInspectorActivity extends PdfActivity {

    /** Colors that are going to be available in annotation creation inspector's color picker. */
    private static final List<Integer> CREATION_PICKER_COLORS = List.of(
            Color.rgb(244, 67, 54), // RED
            Color.rgb(139, 195, 74), // LIGHT GREEN
            Color.rgb(33, 150, 243), // BLUE
            Color.rgb(252, 237, 140), // YELLOW
            Color.rgb(233, 30, 99), // PINK

            // GRAYSCALE below
            Color.rgb(255, 255, 255),
            Color.rgb(224, 224, 224),
            Color.rgb(158, 158, 158),
            Color.rgb(66, 66, 66),
            Color.rgb(0, 0, 0));

    /** Colors that are going to be displayed in annotation editing inspector's root layout. */
    private static final List<Integer> EDITING_PICKER_COLORS = List.of(
            Color.rgb(244, 67, 54), // RED
            Color.rgb(139, 195, 74), // LIGHT GREEN
            Color.rgb(33, 150, 243), // BLUE
            Color.rgb(252, 237, 140), // YELLOW
            Color.rgb(233, 30, 99)); // PINK

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create custom annotation creation inspector displaying custom UI in modal dialog.
        CustomAnnotationCreationInspector customAnnotationCreationInspector = new CustomAnnotationCreationInspector();

        // Add custom color picker view to annotation editing inspector.
        DefaultAnnotationEditingInspectorController customAnnotationEditingInspector =
                new DefaultAnnotationEditingInspectorController(this, getPropertyInspectorCoordinator()) {

                    @Override
                    public void onPreparePropertyInspector(@NonNull PropertyInspector inspector) {
                        // Prepare standard property inspector.
                        super.onPreparePropertyInspector(inspector);

                        // Add decoration to show dividers between inspector items.
                        inspector.addItemDecoration(new PropertyInspectorDividerDecoration(getContext()));

                        // Add custom color picker for the ink annotation.
                        if (getAnnotationEditingController() == null) return;
                        final List<Annotation> annotations =
                                getAnnotationEditingController().getCurrentlySelectedAnnotations();
                        if (annotations.isEmpty()) return;

                        for (Annotation annotation : annotations) {
                            if (annotation.getType() != AnnotationType.INK) return;
                        }

                        final var colorPickerInspectorDetailView = getColorPickerInspectorDetailView(annotations);
                        inspector.addInspectorView(colorPickerInspectorDetailView);
                    }

                    private @NonNull ColorPickerInspectorDetailView getColorPickerInspectorDetailView(
                            List<Annotation> annotations) {
                        final var detailView = new ColorPickerInspectorDetailView(
                                getContext(), EDITING_PICKER_COLORS, EDITING_PICKER_COLORS.get(0), false);
                        detailView.setShowSelectionIndicator(false);

                        detailView.setOnColorPickedListener((view, color) -> {
                            if (getAnnotationEditingController() != null) {
                                for (Annotation annotation : annotations) {
                                    annotation.setColor(color);
                                }
                            }
                        });
                        return detailView;
                    }
                };

        // Set custom annotation inspector controllers to the activity.
        setAnnotationCreationInspectorController(customAnnotationCreationInspector);
        setAnnotationEditingInspectorController(customAnnotationEditingInspector);

        // Restore inspectors state.
        if (savedInstanceState != null) {
            customAnnotationCreationInspector.onRestoreInstanceState(savedInstanceState);
            customAnnotationEditingInspector.onRestoreInstanceState(savedInstanceState);
        }
    }

    @NonNull
    private Context getContext() {
        return this;
    }

    /**
     * Custom implementation of the {@link AnnotationCreationInspectorController} showing simplified
     * annotation inspector in a dialog.
     */
    private class CustomAnnotationCreationInspector implements AnnotationCreationInspectorController {

        private static final String STATE_INSPECTOR_DIALOG_VISIBLE = "STATE_INSPECTOR_DIALOG_VISIBLE";

        @Nullable
        private AnnotationCreationController controller;

        @Nullable
        private AlertDialog dialog;

        @Nullable
        private Bundle restoredInstanceState;

        public void bindAnnotationCreationController(@NonNull AnnotationCreationController controller) {
            this.controller = controller;

            // Bind to annotation creation controller.
            controller.bindAnnotationInspectorController(this);

            // Restore saved state if available.
            onRestoreState();
        }

        public void unbindAnnotationCreationController() {
            cancel();
            if (controller != null) {
                controller.unbindAnnotationInspectorController();
                this.controller = null;
            }
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            outState.putBoolean(STATE_INSPECTOR_DIALOG_VISIBLE, isAnnotationInspectorVisible());
        }

        @Override
        public void onRestoreInstanceState(@NonNull Bundle savedState) {
            this.restoredInstanceState = savedState;
            onRestoreState();
        }

        @Override
        public void showAnnotationInspector(boolean animate) {
            // Create custom layout for your inspector.
            View inspectorRoot = getLayoutInflater().inflate(R.layout.custom_annotation_inspector, null);
            LinearLayout inspectorContainer = inspectorRoot.findViewById(R.id.inspectorContainer);

            // We are reusing our inspector views just for the sake of simplicity.
            List<PropertyInspectorView> inspectorViews = getInspectorViews();
            if (inspectorViews == null) {
                cancel();
                return;
            }
            for (PropertyInspectorView inspectorView : inspectorViews) {
                inspectorContainer.addView(inspectorView.getView());
            }

            // Create alert dialog with inspector layout set as content view.
            this.dialog = new AlertDialog.Builder(getContext())
                    .setView(inspectorRoot)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        }

        @Override
        public void hideAnnotationInspector(boolean animate) {
            if (dialog != null) {
                dialog.cancel();
            }
        }

        @Override
        public void toggleAnnotationInspector(boolean animate) {
            if (isAnnotationInspectorVisible()) {
                hideAnnotationInspector(animate);
            } else {
                showAnnotationInspector(animate);
            }
        }

        @Override
        public boolean isAnnotationInspectorVisible() {
            return dialog != null && dialog.isShowing();
        }

        @Override
        public boolean hasAnnotationInspector() {
            // Enable annotation inspector only for ink and free-text annotations.
            return controller != null
                    && controller.getActiveAnnotationTool() != null
                    && (controller.getActiveAnnotationTool() == AnnotationTool.INK
                            || controller.getActiveAnnotationTool() == AnnotationTool.FREETEXT);
        }

        public void cancel() {
            if (dialog != null) {
                dialog.cancel();
                dialog = null;
            }
        }

        @Nullable
        private List<PropertyInspectorView> getInspectorViews() {
            PdfFragment fragment = getPdfFragment();
            if (controller == null || fragment == null) return null;

            final AnnotationPreferencesManager annotationPreferences = fragment.getAnnotationPreferences();
            final AnnotationTool annotationTool = controller.getActiveAnnotationTool();
            final AnnotationToolVariant annotationToolVariant = controller.getActiveAnnotationToolVariant();
            if (annotationTool == null || annotationToolVariant == null) return null;

            List<PropertyInspectorView> inspectorViews = new ArrayList<>();

            // Create color picker.
            ColorPickerInspectorDetailView colorPicker = new ColorPickerInspectorDetailView(
                    getContext(), CREATION_PICKER_COLORS, controller.getColor(), false);

            colorPicker.setOnColorPickedListener((view, color) -> {
                annotationPreferences.setColor(annotationTool, annotationToolVariant, color);
                controller.setColor(color);
            });
            int padding = dpToPx(getContext(), 8);
            colorPicker.setPadding(padding, padding, padding, padding);
            inspectorViews.add(colorPicker);

            // Create thickness picker for ink annotations.
            if (annotationTool == AnnotationTool.INK) {
                SliderPickerInspectorView thicknessPicker = new SliderPickerInspectorView(
                        getContext(), "Thickness", "%d pt", 1, 20, (int) controller.getThickness(), (view, value) -> {
                            annotationPreferences.setThickness(annotationTool, annotationToolVariant, value);
                            controller.setThickness(value);
                        });
                inspectorViews.add(thicknessPicker);
            }

            // Create text size picker for free-text annotation.
            if (annotationTool == AnnotationTool.FREETEXT) {
                SliderPickerInspectorView textSizePicker = new SliderPickerInspectorView(
                        getContext(), "Text size", "%d pt", 10, 32, (int) controller.getTextSize(), (view, value) -> {
                            annotationPreferences.setTextSize(annotationTool, annotationToolVariant, value);
                            controller.setTextSize(value);
                        });
                inspectorViews.add(textSizePicker);
            }

            return inspectorViews;
        }

        private void onRestoreState() {
            // Restore state when bound to controller and having restored state.
            if (controller == null || restoredInstanceState == null) return;
            boolean isDialogVisible = restoredInstanceState.getBoolean(STATE_INSPECTOR_DIALOG_VISIBLE, false);
            if (isDialogVisible) {
                showAnnotationInspector(false);
            }
            restoredInstanceState = null;
        }
    }
}
