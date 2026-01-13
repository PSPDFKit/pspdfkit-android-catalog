/*
 *   Copyright © 2014-2025 PSPDFKit GmbH. All rights reserved.
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
import com.pspdfkit.catalog.examples.java.activities.CustomAnnotationInspectorActivity;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/** Showcases how to create custom annotation inspector from scratch. */
public class CustomAnnotationInspectorExample extends SdkExample {

    public CustomAnnotationInspectorExample(@NonNull final Context context) {
        super(
                context,
                R.string.annotationCustomInspectorExampleTitle,
                R.string.annotationCustomInspectorExampleDescription);
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // Extract the document from the assets. The launched activity will add annotations to that
        // document.
        ExtractAssetTask.extract(WELCOME_DOC, getTitle(), context, documentFile -> {
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(CustomAnnotationInspectorActivity.class)
                    .build();

            // Start the CustomAnnotationInspectorActivity for the extracted document.
            context.startActivity(intent);
        });
    }
}
