/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.examples.java.activities.FilterableThumbnailGridActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.document.providers.AssetDataProvider;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/**
 * Example on how to display filters in the thumbnail grid. Shows how to combine PDF processor and
 * customizability of the thumbnail grid to create filters.
 */
public class FilterableThumbnailGridExample extends SdkExample {

    public FilterableThumbnailGridExample(@NonNull Context context) {
        super(
                context.getString(R.string.filterableThumbnailGridExampleTitle),
                context.getString(R.string.filterableThumbnailGridExampleDescription));
    }

    @Override
    public void launchExample(@NonNull Context context, @NonNull PdfActivityConfiguration.Builder configuration) {
        final Intent intent = PdfActivityIntentBuilder.fromDataProvider(
                        context, new AssetDataProvider(QUICK_START_GUIDE))
                .configuration(configuration.build())
                .activityClass(FilterableThumbnailGridActivity.class)
                .build();

        context.startActivity(intent);
    }
}
