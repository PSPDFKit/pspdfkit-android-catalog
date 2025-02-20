/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
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
import com.pspdfkit.catalog.examples.java.activities.JavaScriptFormFillingActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/** Shows how to fill forms via JavaScript. */
public class JavaScriptFormFillingExample extends SdkExample {
    public JavaScriptFormFillingExample(@NonNull final Context context) {
        super(
                context.getString(R.string.formFillingJavaScriptExampleTitle),
                context.getString(R.string.formFillingJavaScriptExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        configuration
                // Turn off saving, so we have the clean original document every time the example is
                // launched.
                .autosaveEnabled(false)
                .formEditingEnabled(true)
                .build();

        // Extract the document from the assets.
        extract("Form_example.pdf", getTitle(), context, documentFile -> {
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(JavaScriptFormFillingActivity.class)
                    .build();

            context.startActivity(intent);
        });
    }
}
