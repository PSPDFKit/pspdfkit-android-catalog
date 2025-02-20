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
import com.pspdfkit.catalog.examples.java.activities.ToolbarsInFragmentActivity;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.toolbar.ContextualToolbar;

/**
 * This example shows how to use {@link ContextualToolbar}s withing the custom activity that uses
 * {@link PdfFragment}.
 */
public class ToolbarsInFragmentExample extends SdkExample {

    public ToolbarsInFragmentExample(@NonNull Context context) {
        super(
                context.getString(R.string.toolbarsInFragmentExampleTitle),
                context.getString(R.string.toolbarsInFragmentExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        ExtractAssetTask.extract(QUICK_START_GUIDE, getTitle(), context, documentFile -> {
            final Intent intent = new Intent(context, ToolbarsInFragmentActivity.class);
            intent.putExtra(ToolbarsInFragmentActivity.EXTRA_URI, Uri.fromFile(documentFile));
            intent.putExtra(
                    ToolbarsInFragmentActivity.EXTRA_CONFIGURATION,
                    configuration.build().getConfiguration());
            context.startActivity(intent);
        });
    }
}
