/*
 *   Copyright © 2014-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.PdfConfiguration;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.annotations.OnAnnotatingModeChangeListener;
import com.pspdfkit.ui.inspector.PropertyInspectorCoordinatorLayout;
import com.pspdfkit.ui.inspector.annotation.AnnotatingInspectorController;
import com.pspdfkit.ui.inspector.annotation.DefaultAnnotationCreationInspectorController;
import com.pspdfkit.ui.inspector.annotation.DefaultAnnotationEditingInspectorController;
import com.pspdfkit.ui.special_mode.controller.AnnotatingController;
import com.pspdfkit.ui.special_mode.controller.TextSelectionController;
import com.pspdfkit.ui.special_mode.manager.TextSelectionManager;
import com.pspdfkit.ui.toolbar.AnnotationToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbar;
import com.pspdfkit.ui.toolbar.TextSelectionToolbar;
import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout;

/**
 * This example shows how to use {@link ContextualToolbar}s withing the custom activity that uses
 * {@link PdfFragment}.
 */
public class ToolbarsInFragmentExample extends SdkExample {

    public ToolbarsInFragmentExample(@NonNull Context context) {
        super(context, R.string.toolbarsInFragmentExampleTitle, R.string.toolbarsInFragmentExampleDescription);
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        ExtractAssetTask.extract(WELCOME_DOC, getTitle(), context, documentFile -> {
            final Intent intent = new Intent(context, ToolbarsInFragmentActivity.class);
            intent.putExtra(ToolbarsInFragmentActivity.EXTRA_URI, Uri.fromFile(documentFile));
            intent.putExtra(
                    ToolbarsInFragmentActivity.EXTRA_CONFIGURATION,
                    configuration.build().getConfiguration());
            context.startActivity(intent);
        });
    }

    /**
     * This example shows how to incorporate {@link ContextualToolbar}s into the custom activity using
     * {@link PdfFragment} with all the animations and dragging managed by the {@link
     * ToolbarCoordinatorLayout}.
     */
    public static class ToolbarsInFragmentActivity extends AppCompatActivity
            implements OnAnnotatingModeChangeListener, TextSelectionManager.OnTextSelectionModeChangeListener {

        public static final String EXTRA_URI = "ToolbarsInFragmentActivity.DocumentUri";
        public static final String EXTRA_CONFIGURATION = "ToolbarsInFragmentActivity.PdfConfiguration";

        private PdfFragment fragment;
        private ToolbarCoordinatorLayout toolbarCoordinatorLayout;
        private Button annotationCreationButton;

        private AnnotationToolbar annotationToolbar;
        private TextSelectionToolbar textSelectionToolbar;

        private boolean annotationCreationActive = false;

        private PropertyInspectorCoordinatorLayout inspectorCoordinatorLayout;
        private AnnotatingInspectorController annotationEditingInspectorController;
        private AnnotatingInspectorController annotationCreationInspectorController;

        // Track state for the unified listener
        private boolean creationToolbarShown = false;
        private boolean editingInspectorBound = false;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_annotation_toolbar_fragment);
            setSupportActionBar(null);

            toolbarCoordinatorLayout = findViewById(R.id.toolbarCoordinatorLayout);

            annotationToolbar = new AnnotationToolbar(this);
            textSelectionToolbar = new TextSelectionToolbar(this);

            // Use this if you want to use annotation inspector with annotation creation and editing
            // toolbars.
            inspectorCoordinatorLayout = findViewById(R.id.inspectorCoordinatorLayout);
            annotationEditingInspectorController =
                    new DefaultAnnotationEditingInspectorController(this, inspectorCoordinatorLayout);
            annotationCreationInspectorController =
                    new DefaultAnnotationCreationInspectorController(this, inspectorCoordinatorLayout);

            // The actual document Uri is provided with the launching intent. You can simply change that
            // inside the ToolbarsInFragmentExample class.
            // This is a check that the example is not accidentally launched without a document Uri.
            final Uri uri = getIntent().getParcelableExtra(EXTRA_URI);
            if (uri == null) {
                showNoDocumentUriDialog();
                return;
            }

            // PdfFragment configuration is provided with the launching intent.
            PdfConfiguration configuration = getIntent().getParcelableExtra(EXTRA_CONFIGURATION);
            if (configuration == null) {
                configuration = new PdfConfiguration.Builder().build();
            }

            initPdfFragment(uri, configuration);
            initAnnotationCreationButton();
        }

        private void showNoDocumentUriDialog() {
            new AlertDialog.Builder(this)
                    .setTitle("Could not start example.")
                    .setMessage("No document Uri was provided with the launching intent.")
                    .setNegativeButton("Leave example", (dialog, which) -> dialog.dismiss())
                    .setOnDismissListener(dialog -> finish())
                    .show();
        }

        private void initAnnotationCreationButton() {
            annotationCreationButton = findViewById(R.id.openAnnotationEditing);
            annotationCreationButton.setOnClickListener(v -> {
                if (annotationCreationActive) {
                    fragment.exitCurrentlyActiveMode();
                } else {
                    fragment.enterAnnotatingMode();
                }
            });

            updateButtonText();
        }

        private void initPdfFragment(@NonNull Uri uri, @NonNull PdfConfiguration configuration) {
            fragment = (PdfFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (fragment == null) {
                fragment = PdfFragment.newInstance(uri, configuration);
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.fragmentContainer, fragment)
                        .commit();
            }

            // Use the new unified listener for annotation mode changes
            fragment.addOnAnnotatingModeChangeListener(this);
            fragment.addOnTextSelectionModeChangeListener(this);
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            fragment.removeOnAnnotatingModeChangeListener(this);
            fragment.removeOnTextSelectionModeChangeListener(this);
        }

        @Override
        public void onEnterAnnotatingMode(@NonNull AnnotatingController controller) {
            // Check if we need to show creation toolbar (annotation tool is active)
            if (controller.getActiveAnnotationTool() != null && !creationToolbarShown) {
                // When entering the annotation creation mode we bind the creation inspector to the provided
                // controller.
                // Controller handles request for toggling annotation inspector.
                annotationCreationInspectorController.bindController(controller);

                // When entering the annotation creation mode we bind the toolbar to the provided
                // controller, and issue the coordinator layout to animate the toolbar in place.
                // Whenever the user presses an action, the toolbar forwards this command to the controller.
                // Instead of using the `AnnotationToolbar` you could use a custom UI that operates
                // on the controller.
                annotationToolbar.bindController(controller);
                toolbarCoordinatorLayout.displayContextualToolbar(annotationToolbar, true);
                creationToolbarShown = true;
                annotationCreationActive = true;
                updateButtonText();
            }

            // Check if we need to bind editing inspector (annotations are selected)
            if (controller.hasCurrentlySelectedAnnotations() && !editingInspectorBound) {
                annotationEditingInspectorController.bindController(controller);
                editingInspectorBound = true;
            }
        }

        @Override
        public void onChangeAnnotatingMode(@NonNull AnnotatingController controller) {
            // Handle state changes - check if editing mode was entered or exited
            if (controller.hasCurrentlySelectedAnnotations() && !editingInspectorBound) {
                annotationEditingInspectorController.bindController(controller);
                editingInspectorBound = true;
            } else if (!controller.hasCurrentlySelectedAnnotations() && editingInspectorBound) {
                annotationEditingInspectorController.unbindController();
                editingInspectorBound = false;
            }
        }

        @Override
        public void onExitAnnotatingMode(@NonNull AnnotatingController controller) {
            // Check if creation mode exited (no active tool)
            if (controller.getActiveAnnotationTool() == null && creationToolbarShown) {
                // Once we're done with editing, unbind the controller from the toolbar, and remove it from
                // the toolbar coordinator layout (with animation in this case).
                toolbarCoordinatorLayout.removeContextualToolbar(true);
                annotationToolbar.unbindController();
                annotationCreationInspectorController.unbindController();
                creationToolbarShown = false;
                annotationCreationActive = false;
                updateButtonText();
            }

            // Check if editing mode exited (no selected annotations)
            if (!controller.hasCurrentlySelectedAnnotations() && editingInspectorBound) {
                annotationEditingInspectorController.unbindController();
                editingInspectorBound = false;
            }
        }

        @Override
        public void onEnterTextSelectionMode(@NonNull TextSelectionController controller) {
            textSelectionToolbar.bindController(controller);
            toolbarCoordinatorLayout.displayContextualToolbar(textSelectionToolbar, true);
        }

        @Override
        public void onExitTextSelectionMode(@NonNull TextSelectionController controller) {
            toolbarCoordinatorLayout.removeContextualToolbar(true);
            textSelectionToolbar.unbindController();
        }

        private void updateButtonText() {
            annotationCreationButton.setText(annotationCreationActive ? R.string.close_editor : R.string.open_editor);
        }
    }
}
