/*
 *   Copyright © 2016-2024 PSPDFKit GmbH. All rights reserved.
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
import com.pspdfkit.catalog.examples.java.activities.CustomSearchUiActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.search.PdfSearchViewInline;

/**
 * This example takes the {@link PdfSearchViewInline} and places it inside a custom layout. To do so
 * it uses a custom activity {@link CustomSearchUiActivity} which will manage creation of the new
 * layout as well as showing and hiding of the inline search.
 */
public class CustomSearchUiExample extends SdkExample {

    public CustomSearchUiExample(@NonNull final Context context) {
        super(
                context.getString(R.string.customSearchUiExampleTitle),
                context.getString(R.string.customSearchUiExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // Load the example document from the assets and launch the activity.
        extract(QUICK_START_GUIDE, getTitle(), context, documentFile -> {
            final Intent intent = new Intent(context, CustomSearchUiActivity.class);
            intent.putExtra(CustomSearchUiActivity.EXTRA_URI, Uri.fromFile(documentFile));
            context.startActivity(intent);
        });
    }
}
