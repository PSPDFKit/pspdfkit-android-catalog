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
import com.pspdfkit.catalog.examples.java.activities.CustomStampAnnotationsActivity;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/**
 * Showcases how customize the `StampPickerDialog` with custom set of stamps.
 */
public class CustomStampAnnotationsExample extends SdkExample {

    public CustomStampAnnotationsExample(@NonNull final Context context) {
        super(
                context.getString(R.string.annotationCustomStampAnnotationExampleTitle),
                context.getString(R.string.annotationCustomStampAnnotationExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // Extract the document from the assets. The launched activity will add annotations to that
        // document.
        ExtractAssetTask.extract(QUICK_START_GUIDE, getTitle(), context, documentFile -> {
            // To start the CustomStampAnnotationsActivity create a launch intent using the
            // builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(CustomStampAnnotationsActivity.class)
                    .build();

            // Start the CustomStampAnnotationsActivity for the extracted document.
            context.startActivity(intent);
        });
    }
}
