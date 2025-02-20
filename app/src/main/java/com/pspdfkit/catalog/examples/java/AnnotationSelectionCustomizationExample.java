/*
 *   Copyright © 2014-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.examples.java.activities.AnnotationSelectionCustomizationActivity;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController;

/**
 * This example showcases how to use {@link AnnotationSelectionController} to dynamically modify
 * properties of current annotation selection.
 */
public class AnnotationSelectionCustomizationExample extends SdkExample {

    public AnnotationSelectionCustomizationExample(@NonNull final Context context) {
        super(
                context.getString(R.string.annotationSelectionControllerExampleTitle),
                context.getString(R.string.annotationSelectionControllerExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // Extract the document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, getTitle(), context, documentFile -> {
            // To start the AnnotationSelectionCustomizationActivity create a launch intent
            // using the builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(AnnotationSelectionCustomizationActivity.class)
                    .build();

            context.startActivity(intent);
        });
    }
}
