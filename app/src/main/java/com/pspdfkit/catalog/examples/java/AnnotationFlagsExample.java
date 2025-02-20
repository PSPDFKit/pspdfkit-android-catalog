/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
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
import com.pspdfkit.catalog.examples.java.activities.AnnotationFlagsActivity;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/**
 * Shows how to use {@link com.pspdfkit.annotations.AnnotationFlags} to modify annotation
 * characteristics.
 */
public class AnnotationFlagsExample extends SdkExample {

    public AnnotationFlagsExample(@NonNull final Context context) {
        super(
                context.getString(R.string.annotationFlagsExampleTitle),
                context.getString(R.string.annotationFlagsExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        configuration
                // Turn off saving, so we have the clean original document every time the example is
                // launched.
                .autosaveEnabled(false)
                // Notes are treated as if they had the NOZOOM flag set by default.
                // We override this behavior here by enabling NOZOOM flag handling for note
                // annotations.
                .setEnableNoteAnnotationNoZoomHandling(true);

        // Extract the document from the assets.
        ExtractAssetTask.extract(ANNOTATIONS_EXAMPLE, getTitle(), context, documentFile -> {
            // To start the AnnotationFlagsActivity create a launch intent using the
            // builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(AnnotationFlagsActivity.class)
                    .build();

            context.startActivity(intent);
        });
    }
}
