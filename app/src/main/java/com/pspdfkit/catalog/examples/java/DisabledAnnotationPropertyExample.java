/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.pspdfkit.annotations.configuration.AnnotationConfiguration;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.examples.java.activities.DisabledAnnotationPropertyActivity;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/**
 * Showcases how to change {@link AnnotationConfiguration} in order to disable annotation note
 * option in the annotation editing toolbar.
 */
public class DisabledAnnotationPropertyExample extends SdkExample {
    public DisabledAnnotationPropertyExample(@NonNull final Context context) {
        super(
                context.getString(R.string.disabledAnnotationPropertyExampleTitle),
                context.getString(R.string.disabledAnnotationPropertyExampleDescription));
    }

    @Override
    public void launchExample(@NonNull Context context, @NonNull PdfActivityConfiguration.Builder configuration) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, getTitle(), context, documentFile -> {
            // To start the CustomAnnotationDefaultsManagerActivity we create a launch
            // intent using the builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(DisabledAnnotationPropertyActivity.class)
                    .build();

            context.startActivity(intent);
        });
    }
}
