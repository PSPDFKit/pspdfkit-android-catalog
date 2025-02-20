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
import com.pspdfkit.catalog.examples.java.activities.DocumentSharingExampleActivity;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.configuration.sharing.ShareFeatures;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

public class DocumentSharingExample extends SdkExample {
    public DocumentSharingExample(Context context) {
        super(
                context.getString(R.string.documentSharingExampleTitle),
                context.getString(R.string.documentSharingExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // For sake of simplicity, deactivate actions.
        configuration
                .annotationListEnabled(false)
                .searchEnabled(false)
                .outlineEnabled(false)
                .setEnabledShareFeatures(ShareFeatures.none())
                .printingEnabled(false)
                .thumbnailGridEnabled(false);

        // First extract the document from the assets.
        ExtractAssetTask.extract(ANNOTATIONS_EXAMPLE, getTitle(), context, documentFile -> {
            // To start the DocumentSharingExampleActivity create a launch intent using the
            // builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(DocumentSharingExampleActivity.class)
                    .build();

            // Start the DocumentSharingExampleActivity for the extracted document.
            context.startActivity(intent);
        });
    }
}
