/*
 *   Copyright © 2014-2024 PSPDFKit GmbH. All rights reserved.
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
import com.pspdfkit.catalog.examples.java.activities.SplitDocumentActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfFragment;

/**
 * This example shows how to use two {@link PdfFragment} instances inside a single custom activity.
 */
public class SplitDocumentExample extends SdkExample {

    public SplitDocumentExample(final Context context) {
        super(
                context.getString(R.string.splitDocumentExampleTitle),
                context.getString(R.string.splitDocumentExampleDescription));
    }

    @Override
    public void launchExample(@NonNull Context context, @NonNull PdfActivityConfiguration.Builder configuration) {
        final Intent intent = new Intent(context, SplitDocumentActivity.class);
        context.startActivity(intent);
    }
}
