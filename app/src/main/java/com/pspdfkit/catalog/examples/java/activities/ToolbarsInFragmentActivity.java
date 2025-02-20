/*
 *   Copyright © 2014-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.pspdfkit.catalog.R;
import com.pspdfkit.configuration.PdfConfiguration;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.inspector.PropertyInspectorCoordinatorLayout;
import com.pspdfkit.ui.inspector.annotation.AnnotationCreationInspectorController;
import com.pspdfkit.ui.inspector.annotation.AnnotationEditingInspectorController;
import com.pspdfkit.ui.inspector.annotation.DefaultAnnotationCreationInspectorController;
import com.pspdfkit.ui.inspector.annotation.DefaultAnnotationEditingInspectorController;
import com.pspdfkit.ui.special_mode.controller.AnnotationCreationController;
import com.pspdfkit.ui.special_mode.controller.AnnotationEditingController;
import com.pspdfkit.ui.special_mode.controller.TextSelectionController;
import com.pspdfkit.ui.special_mode.manager.AnnotationManager;
import com.pspdfkit.ui.special_mode.manager.TextSelectionManager;
import com.pspdfkit.ui.toolbar.AnnotationCreationToolbar;
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbar;
import com.pspdfkit.ui.toolbar.TextSelectionToolbar;
import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout;

/**
 * This example shows how to incorporate {@link ContextualToolbar}s into the custom activity using
 * {@link PdfFragment} with all the animations and dragging managed by the {@link
 * ToolbarCoordinatorLayout}.
 */
public class ToolbarsInFragmentActivity extends AppCompatActivity
        implements AnnotationManager.OnAnnotationCreationModeChangeListener,
                AnnotationManager.OnAnnotationEditingModeChangeListener,
                TextSelectionManager.OnTextSelectionModeChangeListener {

    public static final String EXTRA_URI = "ToolbarsInFragmentActivity.DocumentUri";
    public static final String EXTRA_CONFIGURATION = "ToolbarsInFragmentActivity.PdfConfiguration";

    private PdfFragment fragment;
    private ToolbarCoordinatorLayout toolbarCoordinatorLayout;
    private Button annotationCreationButton;

    private AnnotationCreationToolbar annotationCreationToolbar;
    private TextSelectionToolbar textSelectionToolbar;
    private AnnotationEditingToolbar annotationEditingToolbar;

    private boolean annotationCreationActive = false;

    private PropertyInspectorCoordinatorLayout inspectorCoordinatorLayout;
    private AnnotationEditingInspectorController annotationEditingInspectorController;
    private AnnotationCreationInspectorController annotationCreationInspectorController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annotation_toolbar_fragment);
        setSupportActionBar(null);

        toolbarCoordinatorLayout = findViewById(R.id.toolbarCoordinatorLayout);

        annotationCreationToolbar = new AnnotationCreationToolbar(this);
        textSelectionToolbar = new TextSelectionToolbar(this);
        annotationEditingToolbar = new AnnotationEditingToolbar(this);

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
                fragment.enterAnnotationCreationMode();
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

        fragment.addOnAnnotationCreationModeChangeListener(this);
        fragment.addOnAnnotationEditingModeChangeListener(this);
        fragment.addOnTextSelectionModeChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fragment.removeOnAnnotationCreationModeChangeListener(this);
        fragment.removeOnAnnotationEditingModeChangeListener(this);
        fragment.removeOnTextSelectionModeChangeListener(this);
    }

    @Override
    public void onEnterAnnotationCreationMode(@NonNull AnnotationCreationController controller) {
        // When entering the annotation creation mode we bind the creation inspector to the provided
        // controller.
        // Controller handles request for toggling annotation inspector.
        annotationCreationInspectorController.bindAnnotationCreationController(controller);

        // When entering the annotation creation mode we bind the toolbar to the provided
        // controller, and
        // issue the coordinator layout to animate the toolbar in place.
        // Whenever the user presses an action, the toolbar forwards this command to the controller.
        // Instead of using the `AnnotationEditingToolbar` you could use a custom UI that operates
        // on the controller.
        // Same principle is used on all other toolbars.
        annotationCreationToolbar.bindController(controller);
        toolbarCoordinatorLayout.displayContextualToolbar(annotationCreationToolbar, true);
        annotationCreationActive = true;
        updateButtonText();
    }

    @Override
    public void onChangeAnnotationCreationMode(@NonNull AnnotationCreationController controller) {
        // Nothing to be done here, if toolbar is bound to the controller it will pick up the
        // changes.
    }

    @Override
    public void onExitAnnotationCreationMode(@NonNull AnnotationCreationController controller) {
        // Once we're done with editing, unbind the controller from the toolbar, and remove it from
        // the
        // toolbar coordinator layout (with animation in this case).
        // Same principle is used on all other toolbars.
        toolbarCoordinatorLayout.removeContextualToolbar(true);
        annotationCreationToolbar.unbindController();
        annotationCreationActive = false;

        // Also unbind the annotation creation controller from the inspector controller.
        annotationCreationInspectorController.unbindAnnotationCreationController();

        updateButtonText();
    }

    @Override
    public void onEnterAnnotationEditingMode(@NonNull AnnotationEditingController controller) {
        annotationEditingInspectorController.bindAnnotationEditingController(controller);

        annotationEditingToolbar.bindController(controller);
        toolbarCoordinatorLayout.displayContextualToolbar(annotationEditingToolbar, true);
    }

    @Override
    public void onChangeAnnotationEditingMode(@NonNull AnnotationEditingController controller) {
        // Nothing to be done here, if toolbar is bound to the controller it will pick up the
        // changes.
    }

    @Override
    public void onExitAnnotationEditingMode(@NonNull AnnotationEditingController controller) {
        toolbarCoordinatorLayout.removeContextualToolbar(true);
        annotationEditingToolbar.unbindController();

        annotationEditingInspectorController.unbindAnnotationEditingController();
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
