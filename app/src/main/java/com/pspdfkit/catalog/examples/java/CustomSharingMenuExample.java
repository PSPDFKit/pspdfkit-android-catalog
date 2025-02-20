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
import com.pspdfkit.catalog.examples.java.activities.CustomSharingMenuActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

public class CustomSharingMenuExample extends SdkExample {

    public CustomSharingMenuExample(Context context) {
        super(
                context.getString(R.string.documentSharingMenuExampleTitle),
                context.getString(R.string.documentSharingMenuExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // This example uses a custom activity which shows how to implement custom sharing.

        // For sake of simplicity, deactivate actions.
        configuration.disableAnnotationList().disableSearch().disableOutline().hideThumbnailGrid();

        // First extract the document from the assets.
        extract(ANNOTATIONS_EXAMPLE, getTitle(), context, documentFile -> {
            // To start CustomSharingMenuActivity create a launch intent using this builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(CustomSharingMenuActivity.class)
                    .build();

            // Start the CustomSharingMenuActivity for the extracted document.
            context.startActivity(intent);
        });
    }
}
