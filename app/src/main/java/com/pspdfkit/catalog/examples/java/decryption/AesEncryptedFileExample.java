/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.decryption;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

public class AesEncryptedFileExample extends SdkExample {

    private static final String ASSET_FILE_NAME = "A_encrypted.pdf";

    /**
     * This is 256B AES encryption key stored encoded as BASE64. In production apps this should be
     * secured! *
     */
    private static final String BASE64_ENCRYPTION_KEY = "EQQlw3SNbBwbxkSi1jwwib4B4XqesCVDZv9LftsmE1U=";

    public AesEncryptedFileExample(@NonNull Context context) {
        super(context.getString(R.string.aesExampleTitle), context.getString(R.string.aesExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        ExtractAssetTask.extract(ASSET_FILE_NAME, getTitle(), context, documentFile -> {
            AesDataProvider provider = new AesDataProvider(documentFile.getAbsolutePath(), BASE64_ENCRYPTION_KEY);
            Intent intent = PdfActivityIntentBuilder.fromDataProvider(context, provider)
                    .configuration(configuration.build())
                    .build();
            context.startActivity(intent);
        });
    }
}
