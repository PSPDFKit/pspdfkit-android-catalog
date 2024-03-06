/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.pspdfkit.catalog.R;
import com.pspdfkit.configuration.PdfConfiguration;
import com.pspdfkit.forms.ComboBoxFormElement;
import com.pspdfkit.forms.ListBoxFormElement;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.forms.FormEditingBar;
import com.pspdfkit.ui.inspector.PropertyInspectorCoordinatorLayout;
import com.pspdfkit.ui.inspector.forms.FormEditingInspectorController;
import com.pspdfkit.ui.special_mode.controller.FormEditingController;
import com.pspdfkit.ui.special_mode.manager.FormManager;

/**
 * This example shows how to incorporate form editing UI into the custom activity using {@link
 * PdfFragment}. This includes 2 UI components:
 *
 * <ul>
 *   <li>Form editing bar - helps with form filling, provides buttons for navigating between form
 *       elements when editing forms. Usually displayed above soft-keyboard or at the bottom of the
 *       screen.
 *   <li>Form editing inspector - bottom sheet showing options of choice form elements ({@link
 *       ComboBoxFormElement} and {@link ListBoxFormElement})
 * </ul>
 */
public class FormEditingInFragmentActivity extends AppCompatActivity
        implements FormManager.OnFormElementEditingModeChangeListener {

    public static final String EXTRA_URI = "FormEditingInFragmentActivity.DocumentUri";
    public static final String EXTRA_CONFIGURATION = "FormEditingInFragmentActivity.PdfConfiguration";

    /** Fragment that displays the PDF document. */
    private PdfFragment fragment;

    /**
     * Coordinator for property inspectors displayed in bottom sheet. Needs to be part of the
     * activity layout.
     */
    private PropertyInspectorCoordinatorLayout inspectorCoordinatorLayout;

    /**
     * Controller managing lifecycle of form editing inspector while form editing mode is active.
     */
    private FormEditingInspectorController formEditingInspectorController;

    /**
     * Auxiliary view that provides buttons for navigating between form fields in tab order,
     * clearing form field and finishing form editing.
     */
    private FormEditingBar formEditingBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_editing_fragment);
        setSupportActionBar(null);

        // Use this if you want to use form inspector when editing form fields.
        inspectorCoordinatorLayout = findViewById(R.id.inspectorCoordinatorLayout);
        formEditingInspectorController = new FormEditingInspectorController(this, inspectorCoordinatorLayout);

        // Use this if you want to use form editing bar at the bottom of the screen.
        formEditingBar = findViewById(R.id.formEditingBar);

        // The actual document Uri is provided with the launching intent.
        // This is a check that the example is not accidentally launched without a document Uri.
        final Uri uri = getIntent().getParcelableExtra(EXTRA_URI);
        if (uri == null) {
            finish();
            return;
        }

        // PdfFragment configuration is provided with the launching intent.
        PdfConfiguration configuration = getIntent().getParcelableExtra(EXTRA_CONFIGURATION);
        if (configuration == null) {
            configuration = new PdfConfiguration.Builder().build();
        }

        // Initialize PdfFragment now
        initPdfFragment(uri, configuration);
    }

    private void initPdfFragment(@NonNull Uri uri, @NonNull PdfConfiguration configuration) {
        // Initialize PdfFragment.
        fragment = (PdfFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (fragment == null) {
            fragment = PdfFragment.newInstance(uri, configuration);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .commit();
        }

        // Register listener for form element editing mode changes. We'll use this to bind our form
        // editing UI to form editing controller.
        fragment.addOnFormElementEditingModeChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fragment.removeOnFormElementEditingModeChangeListener(this);
    }

    @Override
    public void onEnterFormElementEditingMode(@NonNull FormEditingController controller) {
        // When entering the form editing mode we need to bind required UI components to provided
        // form editing controller.
        // These will animate form inspector and form editing bar into view if necessary.
        formEditingInspectorController.bindFormEditingController(controller);
        formEditingBar.bindController(controller);
    }

    @Override
    public void onChangeFormElementEditingMode(@NonNull FormEditingController controller) {
        // Nothing to be done here, bound form editing bar and form editing inspector controller
        // will pick up the changes.
    }

    @Override
    public void onExitFormElementEditingMode(@NonNull FormEditingController controller) {
        // Once we're done with form editing, unbind the editing controller
        // from the UI, this will animate form editing bar and inspector from view.
        formEditingInspectorController.unbindFormEditingController();
        formEditingBar.unbindController();
    }
}
