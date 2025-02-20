/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
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
import com.pspdfkit.catalog.examples.java.activities.IndexedFullTextSearchActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;

public class IndexedFullTextSearchExample extends SdkExample {

    public IndexedFullTextSearchExample(@NonNull final Context context) {
        super(
                context.getString(R.string.fullTextSearchIndexingExampleTitle),
                context.getString(R.string.fullTextSearchIndexingExampleDescription));
    }

    @Override
    public void launchExample(@NonNull Context context, @NonNull PdfActivityConfiguration.Builder configuration) {
        context.startActivity(new Intent(context, IndexedFullTextSearchActivity.class));
    }
}
