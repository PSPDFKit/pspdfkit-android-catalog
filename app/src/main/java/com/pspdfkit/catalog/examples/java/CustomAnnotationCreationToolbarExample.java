/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import static com.pspdfkit.catalog.tasks.ExtractAssetTask.extract;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.examples.java.activities.CustomAnnotationCreationToolbarActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/**
 * Example showing how to use custom annotation creation toolbar item and bind it to the annotation
 * tool and variant.
 */
public class CustomAnnotationCreationToolbarExample extends SdkExample {

    public CustomAnnotationCreationToolbarExample(@NonNull Context context) {
        super(
                context.getString(R.string.customAnnotationCreationToolbarExampleTitle),
                context.getString(R.string.customAnnotationCreationToolbarExampleDescription));
    }

    @Override
    public void launchExample(@NonNull Context context, @NonNull PdfActivityConfiguration.Builder configuration) {
        // We use a custom utility class to extract the example document from the assets.
        extract(QUICK_START_GUIDE, getTitle(), context, documentFile -> {
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(CustomAnnotationCreationToolbarActivity.class)
                    .build();
            context.startActivity(intent);
        });
    }
}
