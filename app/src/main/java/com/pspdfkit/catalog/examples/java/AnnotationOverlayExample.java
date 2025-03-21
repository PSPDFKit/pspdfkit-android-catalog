/*
 *   Copyright © 2018-2025 PSPDFKit GmbH. All rights reserved.
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
import com.pspdfkit.catalog.examples.java.activities.AnnotationOverlayActivity;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/** Showcases how to display annotations in overlay mode. */
public class AnnotationOverlayExample extends SdkExample {

    public AnnotationOverlayExample(@NonNull final Context context) {
        super(
                context.getString(R.string.annotationOverlayExampleTitle),
                context.getString(R.string.annotationOverlayExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // Extract the document from the assets.
        ExtractAssetTask.extract(ANNOTATIONS_EXAMPLE, getTitle(), context, documentFile -> {
            // To start the example create a launch intent using the builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(AnnotationOverlayActivity.class)
                    .build();

            context.startActivity(intent);
        });
    }
}
