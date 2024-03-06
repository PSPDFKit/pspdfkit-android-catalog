/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.pspdfkit.document.providers.ContentResolverDataProvider;
import com.pspdfkit.document.sharing.DocumentSharingProvider;
import com.pspdfkit.document.sharing.DocumentSharingProviderProcessor;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * This activity showcases how to use {@link DocumentSharingProvider} for exposing shared images via
 * system clipboard.
 *
 * <p>This activity handles image share intents. We use the following intent filter in
 * AndroidManifest.xml:
 *
 * <pre>{@code
 * <intent-filter>
 *    <action android:name="android.intent.action.SEND"/>
 *    <category android:name="android.intent.category.DEFAULT"/>
 *    <data android:mimeType="image/*"/>
 * </intent-filter>
 * }</pre>
 */
public class CopyToClipboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (action != null && type != null && action.equals(Intent.ACTION_SEND) && type.startsWith("image/")) {
            copySharedImageToSystemClipboard(intent);
        }
    }

    private void copySharedImageToSystemClipboard(@NonNull Intent intent) {
        // We support only image files at the moment.
        final String mimeType = intent.getType();
        if (mimeType == null || !mimeType.startsWith("image/")) {
            showMessageAndFinish("File is not an image.");
            return;
        }

        // Extract image uri from share intent.
        Uri imageUri = null;
        if (intent.getData() != null) {
            imageUri = intent.getData();
        } else if (intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
            imageUri = intent.getClipData().getItemAt(0).getUri();
        }
        if (imageUri == null) {
            showMessageAndFinish("Image could not be copied to clipboard.");
            return;
        }

        // Show indeterminate progress dialog while the file is being prepared.
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("Copying image to clipboard...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        // Use DocumentSharingProviderProcessor helper to prepare file for sharing in
        // DocumentSharingProvider.
        DocumentSharingProviderProcessor.prepareFileForSharing(this, new ContentResolverDataProvider(imageUri), null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((uri, throwable) -> {
                    final ClipboardManager clipboardManager =
                            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboardManager != null && uri != null) {
                        // Expose the share uri via system clipboard.
                        ClipData primaryClip = new ClipData(null, new String[] {mimeType}, new ClipData.Item(uri));
                        try {
                            clipboardManager.setPrimaryClip(primaryClip);
                            showMessageAndFinish("Image copied to clipboard.");
                        } catch (Exception e) {
                            showMessageAndFinish("Image could not be copied to clipboard.");
                        }
                        return;
                    }
                    showMessageAndFinish("Image could not be copied to clipboard.");
                });
    }

    private void showMessageAndFinish(@NonNull String errorString) {
        Toast.makeText(this, errorString, Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Controls whether this activity component is enabled. This setting will override enabled state
     * specified in manifest. Defaults to enabled.
     *
     * @param context Context to use.
     * @param isEnabled If {@code true}, handling for implicit intents for copying image to
     *     clipboard is enabled.
     */
    public static void setEnabled(@NonNull Context context, boolean isEnabled) {
        PackageManager packageManager = context.getPackageManager();
        packageManager.setComponentEnabledSetting(
                new ComponentName(context, CopyToClipboardActivity.class),
                isEnabled
                        ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * Check whether this activity component is enabled.
     *
     * @param context Context to use.
     */
    public static boolean isEnabled(@NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        int componentEnabledSetting =
                packageManager.getComponentEnabledSetting(new ComponentName(context, CopyToClipboardActivity.class));
        return componentEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                || componentEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }
}
