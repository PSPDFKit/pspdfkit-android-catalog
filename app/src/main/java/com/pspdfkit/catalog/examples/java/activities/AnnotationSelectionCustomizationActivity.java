/*
 *   Copyright © 2014-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController;
import com.pspdfkit.ui.special_mode.manager.AnnotationManager;

/** Shows how to use {@link AnnotationSelectionController} to control annotation selection. */
public class AnnotationSelectionCustomizationActivity extends PdfActivity
        implements AnnotationManager.OnAnnotationSelectedListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register annotation selection listener.
        getPdfFragment().addOnAnnotationSelectedListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getPdfFragment().removeOnAnnotationSelectedListener(this);
    }

    @Override
    public boolean onPrepareAnnotationSelection(
            @NonNull AnnotationSelectionController controller,
            @NonNull Annotation annotation,
            boolean annotationCreated) {
        switch (annotation.getType()) {
            case STAMP:
                // Allow dragging and resizing stamp annotations only when being created. Afterwards
                // they are fixed in place.
                if (!annotationCreated) {
                    // Disable resizing and dragging for selected stamp annotation.
                    controller.setResizeEnabled(false);
                    controller.setDraggingEnabled(false);
                }
                break;
            case INK:
                // Keep aspect ratio when resizing ink annotations.
                controller.setKeepAspectRatioEnabled(true);
                break;
            case FREETEXT:
                // Prevent selection for free-text annotations that are not being created.
                return annotationCreated;
            default:
        }
        // Return true here to proceed with selection. Returning false will prevent the selection.
        return true;
    }

    @Override
    public void onAnnotationSelected(@NonNull Annotation annotation, boolean annotationCreated) {
        // Nothing to do here.
    }
}
