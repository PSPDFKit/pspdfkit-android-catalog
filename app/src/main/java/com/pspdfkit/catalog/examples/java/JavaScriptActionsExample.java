/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import static com.pspdfkit.catalog.tasks.ExtractAssetTask.extract;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.examples.java.activities.JavaScriptActionsActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/** This example shows how to create and execute JavaScript actions programmatically. */
public class JavaScriptActionsExample extends SdkExample {

    public JavaScriptActionsExample(@NonNull Context context) {
        super(
                context.getString(R.string.javaScriptActionExampleTitle),
                context.getString(R.string.javaScriptActionExampleDescription));
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        configuration
                // JavaScript is enabled by default. It can be disabled in configuration.
                .setJavaScriptEnabled(true)
                // Turn off saving, so we have the clean original document every time the example is
                // launched.
                .autosaveEnabled(false);

        extract(QUICK_START_GUIDE, getTitle(), context, documentFile -> {
            // Launch the custom example activity using the document and configuration.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(JavaScriptActionsActivity.class)
                    .build();
            context.startActivity(intent);
        });
    }
}
