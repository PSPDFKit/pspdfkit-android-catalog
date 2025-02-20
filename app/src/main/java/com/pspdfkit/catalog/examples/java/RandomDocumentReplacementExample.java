/*
 *   Copyright © 2014-2024 PSPDFKit GmbH. All rights reserved.
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
import com.pspdfkit.catalog.examples.java.activities.RandomDocumentReplacementActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

public class RandomDocumentReplacementExample extends SdkExample {

    public RandomDocumentReplacementExample(Context context) {
        super(
                context.getString(R.string.randomDocumentReplacementExampleTitle),
                context.getString(R.string.randomDocumentReplacementExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // Launch the example activity with an initial document.
        final String initialAssetFile = QUICK_START_GUIDE;
        extract(initialAssetFile, getTitle(), context, documentFile -> {
            // Launch the custom example activity using the document and configuration.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(RandomDocumentReplacementActivity.class)
                    .build();

            // Start the RandomDocumentReplacementActivity for the extracted document.
            context.startActivity(intent);
        });
    }
}
