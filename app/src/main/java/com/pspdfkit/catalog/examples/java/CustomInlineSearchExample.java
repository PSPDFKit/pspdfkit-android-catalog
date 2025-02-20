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
import com.pspdfkit.catalog.examples.java.activities.CustomInlineSearchExampleActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import com.pspdfkit.ui.search.PdfSearchViewInline;

/**
 * This example takes the {@link PdfSearchViewInline} and places it inside a custom layout. To do so
 * it uses a custom activity {@link CustomInlineSearchExampleActivity} which will manage creation of
 * the new layout as well as showing and hiding of the inline search.
 */
public class CustomInlineSearchExample extends SdkExample {

    public CustomInlineSearchExample(@NonNull final Context context) {
        super(
                context.getString(R.string.customInlineSearchExampleTitle),
                context.getString(R.string.customInlineSearchExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // This example requires inline search, so override any catalog configuration.
        // To change the location of the PdfSearchViewInline as well as the way it is integrated,
        // you have to use a custom activity.
        configuration.setSearchType(PdfActivityConfiguration.SEARCH_INLINE).enableSearch();

        // Load the example document from the assets and launch the activity.
        extract(QUICK_START_GUIDE, getTitle(), context, documentFile -> {
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(CustomInlineSearchExampleActivity.class)
                    .build();

            // Start the CustomInlineSearchExampleActivity for the extracted document.
            context.startActivity(intent);
        });
    }
}
