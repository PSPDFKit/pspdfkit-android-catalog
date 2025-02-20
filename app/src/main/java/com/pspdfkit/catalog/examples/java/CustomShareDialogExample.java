/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
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
import com.pspdfkit.catalog.examples.java.activities.CustomShareDialogActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/** Shows you how to create a custom document share dialog. */
public class CustomShareDialogExample extends SdkExample {

    public CustomShareDialogExample(Context context) {
        super(
                context.getString(R.string.customShareDialogExampleTitle),
                context.getString(R.string.customShareDialogExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // We use a custom utility class to extract the example document from the assets.
        extract(QUICK_START_GUIDE, getTitle(), context, documentFile -> {
            // To start the CustomShareDialogActivity create a launch intent using the
            // builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(CustomShareDialogActivity.class)
                    .build();

            // Start the CustomShareDialogActivity for the extracted document.
            context.startActivity(intent);
        });
    }
}
