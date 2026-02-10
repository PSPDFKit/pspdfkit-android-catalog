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
import androidx.annotation.NonNull;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import com.pspdfkit.ui.annotations.OnAnnotationSelectedListener;
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController;

/**
 * This example showcases how to use {@link AnnotationSelectionController} to dynamically modify
 * properties of current annotation selection.
 */
public class AnnotationSelectionCustomizationExample extends SdkExample {

    public AnnotationSelectionCustomizationExample(@NonNull final Context context) {
        super(
                context,
                R.string.annotationSelectionControllerExampleTitle,
                R.string.annotationSelectionControllerExampleDescription);
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // Extract the document from the assets.
        ExtractAssetTask.extract(WELCOME_DOC, getTitle(), context, documentFile -> {
            // To start the AnnotationSelectionCustomizationActivity create a launch intent
            // using the builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(AnnotationSelectionCustomizationActivity.class)
                    .build();

            context.startActivity(intent);
        });
    }

    /** Shows how to use {@link AnnotationSelectionController} to control annotation selection. */
    public static class AnnotationSelectionCustomizationActivity extends PdfActivity
            implements OnAnnotationSelectedListener {

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
}
