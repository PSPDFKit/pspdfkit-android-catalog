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
import com.pspdfkit.catalog.examples.java.activities.CustomElectronicSignatureActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.signatures.ElectronicSignatureFragment;

/**
 * Showcases how to use {@link ElectronicSignatureFragment} to implement custom ink signature flow.
 *
 * This example adds a button to enable and disable signature creation when interacting with a
 * signature form element.
 */
public class CustomElectronicSignatureExample extends SdkExample {
    public CustomElectronicSignatureExample(@NonNull final Context context) {
        super(
                context.getString(R.string.customElectronicSignatureExampleTitle),
                context.getString(R.string.customElectronicSignatureExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        extract(QUICK_START_GUIDE, getTitle(), context, documentFile -> {
            final Intent intent = new Intent(context, CustomElectronicSignatureActivity.class);
            intent.putExtra(CustomElectronicSignatureActivity.EXTRA_URI, Uri.fromFile(documentFile));
            context.startActivity(intent);
        });
    }
}
