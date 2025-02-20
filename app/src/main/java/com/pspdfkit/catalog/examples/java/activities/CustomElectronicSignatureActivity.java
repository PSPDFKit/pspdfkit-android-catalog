/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.catalog.R;
import com.pspdfkit.configuration.PdfConfiguration;
import com.pspdfkit.configuration.signatures.SignatureColorOptions;
import com.pspdfkit.configuration.signatures.SignatureCreationMode;
import com.pspdfkit.configuration.signatures.SignatureSavingStrategy;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.forms.FormElement;
import com.pspdfkit.forms.FormType;
import com.pspdfkit.listeners.SimpleDocumentListener;
import com.pspdfkit.signatures.Signature;
import com.pspdfkit.signatures.listeners.OnSignaturePickedListener;
import com.pspdfkit.signatures.storage.DatabaseSignatureStorage;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.signatures.ElectronicSignatureFragment;
import com.pspdfkit.ui.signatures.ElectronicSignatureOptions;
import com.pspdfkit.ui.special_mode.controller.AnnotationEditingController;
import com.pspdfkit.ui.special_mode.manager.AnnotationManager;
import com.pspdfkit.ui.special_mode.manager.FormManager;
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar;
import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout;
import java.util.Arrays;

/**
 * Showcases how to use {@link ElectronicSignatureFragment} to implement custom ink signature flow
 * in custom activities using {@link PdfFragment}.
 *
 * Uses a button to enable and disable signature creation when interacting with a signature form
 * element.
 */
public class CustomElectronicSignatureActivity extends AppCompatActivity
        implements AnnotationManager.OnAnnotationEditingModeChangeListener, OnSignaturePickedListener {

    public static final String EXTRA_URI = "CustomElectronicSignatureActivity.DocumentUri";

    private static final String STATE_TOUCHED_PAGE_INDEX = "STATE_TOUCHED_PAGE_INDEX";
    private static final String STATE_TOUCHED_POINT = "STATE_TOUCHED_POINT";

    private PdfFragment fragment;
    private ToolbarCoordinatorLayout toolbarCoordinatorLayout;
    private Button annotationCreationButton;

    private AnnotationEditingToolbar annotationEditingToolbar;

    private SimpleDocumentListener documentListener;

    private boolean annotationSignatureCreationActive;
    private PointF touchedPoint;
    private int touchedPageIndex;

    // Here we are also showing how to configure the customizable signature colors and available
    // signing tabs (`signatureCreationModes`).
    @NonNull
    private final ElectronicSignatureOptions signatureOptions = new ElectronicSignatureOptions(
            SignatureSavingStrategy.SAVE_IF_SELECTED,
            SignatureColorOptions.fromColorInt(Color.BLACK, Color.GRAY, Color.RED),
            Arrays.asList(SignatureCreationMode.DRAW, SignatureCreationMode.TYPE));

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_ink_signature);
        setSupportActionBar(null);

        toolbarCoordinatorLayout = findViewById(R.id.toolbarCoordinatorLayout);
        annotationEditingToolbar = new AnnotationEditingToolbar(this);

        // The actual document Uri is provided with the launching intent. You can simply change that
        // inside the CustomSearchUiExample class.
        // This is a check that the example is not accidentally launched without a document Uri.
        final Uri uri = getIntent().getParcelableExtra(EXTRA_URI);
        if (uri == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Could not start example.")
                    .setMessage("No document Uri was provided with the launching intent.")
                    .setNegativeButton("Leave example", (dialog, which) -> dialog.dismiss())
                    .setOnDismissListener(dialog -> finish())
                    .show();

            return;
        }

        fragment = (PdfFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (fragment == null) {
            fragment = PdfFragment.newInstance(uri, new PdfConfiguration.Builder().build());
            // Signature field is on page index 16.
            fragment.setPageIndex(16);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .commit();
        }

        // In this example we show how to create ink signatures by tapping on the page.
        // For this to work we need to handle page clicks in document listener.
        documentListener = new SimpleDocumentListener() {
            @Override
            public boolean onPageClick(
                    @NonNull PdfDocument document,
                    @IntRange(from = 0) final int pageIndex,
                    @Nullable MotionEvent event,
                    @Nullable final PointF pagePosition,
                    @Nullable Annotation clickedAnnotation) {
                if (annotationSignatureCreationActive && pagePosition != null) {

                    // Store touched page index and touch point.
                    touchedPageIndex = pageIndex;
                    touchedPoint = pagePosition;

                    // Show signature picker. This will add the signature picker
                    // fragment (if not added yet).
                    // Note that if you set SignatureSavingStrategy.SAVE_IF_SELECTED, you
                    // must provide a SignatureStorage.
                    ElectronicSignatureFragment.show(
                            getSupportFragmentManager(),
                            CustomElectronicSignatureActivity.this,
                            signatureOptions,
                            DatabaseSignatureStorage.withName(
                                    getBaseContext(), DatabaseSignatureStorage.SIGNATURE_DB_NAME));
                    return true;
                }
                return false;
            }
        };
        fragment.addDocumentListener(documentListener);

        // Register annotation editing mode change listener for showing editing toolbar when
        // entering annotation editing mode.
        fragment.addOnAnnotationEditingModeChangeListener(this);

        // For the sake of this example we toggle ink signature creation via simple button.
        annotationCreationButton = findViewById(R.id.createInkSignature);
        annotationCreationButton.setOnClickListener(
                v -> setSignatureCreationModeActive(!annotationSignatureCreationActive));
        updateButtonText();

        // Restore touched page index and touch point after configuration change.
        if (savedInstanceState != null) {
            touchedPageIndex = savedInstanceState.getInt(STATE_TOUCHED_PAGE_INDEX);
            touchedPoint = savedInstanceState.getParcelable(STATE_TOUCHED_POINT);
        }

        // Restore existing signature picker dialog fragment.
        ElectronicSignatureFragment.restore(getSupportFragmentManager(), this);

        fragment.addOnFormElementClickedListener(new FormManager.OnFormElementClickedListener() {
            @Override
            public boolean isFormElementClickable(@NonNull FormElement formElement) {
                // Returning false for signature fields prevents showing signature dialog
                // when clicking on the signature field.
                return formElement.getType() != FormType.SIGNATURE;
            }

            @Override
            public boolean onFormElementClicked(@NonNull FormElement formElement) {
                // This click event is not interesting to us. Return false to let PSPDFKit
                // handle this event.
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fragment.removeOnAnnotationEditingModeChangeListener(this);
        if (documentListener != null) {
            fragment.removeDocumentListener(documentListener);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save touched page index and touch point here.
        outState.putInt(STATE_TOUCHED_PAGE_INDEX, touchedPageIndex);
        outState.putParcelable(STATE_TOUCHED_POINT, touchedPoint);
    }

    private void setSignatureCreationModeActive(boolean isActive) {
        if (annotationSignatureCreationActive == isActive) return;
        annotationSignatureCreationActive = isActive;

        if (isActive) {
            // Leave editing mode when entering signature creation mode.
            fragment.exitCurrentlyActiveMode();
        } else {
            // Finish signature picker fragment when leaving signature creation mode.
            ElectronicSignatureFragment.dismiss(getSupportFragmentManager());
        }

        updateButtonText();
    }

    @Override
    public void onSignaturePicked(@NonNull final Signature signature) {
        // Leave signature creation mode after signature has been picked.
        setSignatureCreationModeActive(false);
        if (fragment.getDocument() == null || touchedPoint == null) return;

        // Create signature annotation from returned signature data.
        final Annotation signatureAnnotation =
                signature.toAnnotation(fragment.getDocument(), touchedPageIndex, touchedPoint);
        // Set annotation's creator as set in annotation preferences.
        if (fragment.getAnnotationPreferences().isAnnotationCreatorSet()) {
            signatureAnnotation.setCreator(fragment.getAnnotationPreferences().getAnnotationCreator());
        }
        // Add annotation to page and select it immediately for editing.
        fragment.addAnnotationToPage(signatureAnnotation, true);
    }

    @Override
    public void onDismiss() {
        // In this example we leave signature creation mode when dismissing signature picker.
        setSignatureCreationModeActive(false);
    }

    /**
     * Called when annotation editing mode has been entered.
     *
     * @param controller Controller for managing annotation editing.
     */
    @Override
    public void onEnterAnnotationEditingMode(@NonNull AnnotationEditingController controller) {
        annotationEditingToolbar.bindController(controller);
        toolbarCoordinatorLayout.displayContextualToolbar(annotationEditingToolbar, true);
    }

    /**
     * Called then annotation editing mode changes, meaning another annotation is being selected for
     * editing.
     *
     * @param controller Controller for managing annotation editing.
     */
    @Override
    public void onChangeAnnotationEditingMode(@NonNull AnnotationEditingController controller) {
        // Nothing to be done here, if toolbar is bound to the controller it will pick up the
        // changes.
    }

    /**
     * Called when annotation editing mode has been exited.
     *
     * @param controller Controller for managing annotation editing.
     */
    @Override
    public void onExitAnnotationEditingMode(@NonNull AnnotationEditingController controller) {
        toolbarCoordinatorLayout.removeContextualToolbar(true);
        annotationEditingToolbar.unbindController();
    }

    private void updateButtonText() {
        annotationCreationButton.setText(
                annotationSignatureCreationActive
                        ? R.string.signature_creation_enabled
                        : R.string.signature_creation_disabled);
    }
}
