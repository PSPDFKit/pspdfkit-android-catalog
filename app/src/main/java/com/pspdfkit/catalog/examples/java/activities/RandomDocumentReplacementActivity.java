/*
 *   Copyright © 2014-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfActivity;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class RandomDocumentReplacementActivity extends PdfActivity {

    private final Random random = new Random(System.nanoTime());
    private final List<String> assetNames =
            Arrays.asList(SdkExample.QUICK_START_GUIDE, "Classbook.pdf", "Aviation.pdf", "Annotations.pdf");
    private String displayedAssetName;
    private boolean isResumed = false;

    @Override
    protected void onPause() {
        super.onPause();
        isResumed = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isResumed = true;
    }

    @UiThread
    @Override
    public void onDocumentLoaded(@NonNull PdfDocument document) {
        // Random timeout from 0 to 6 seconds.
        final int loadDocumentTimeout = random.nextInt(6000);

        // Select some different document than what is currently shown.
        String randomAsset;
        do randomAsset = assetNames.get(random.nextInt(assetNames.size()));
        while (randomAsset.equals(displayedAssetName));
        displayedAssetName = randomAsset;

        // Schedule the document reload.
        new Handler()
                .postDelayed(
                        () -> ExtractAssetTask.extract(
                                displayedAssetName,
                                getString(R.string.randomDocumentReplacementExampleTitle),
                                RandomDocumentReplacementActivity.this,
                                documentFile -> {
                                    // Setting a document is only allowed while the activity
                                    // is running (i.e. not paused).
                                    if (isResumed) {
                                        Toast.makeText(
                                                        RandomDocumentReplacementActivity.this,
                                                        String.format(
                                                                Locale.getDefault(),
                                                                "Loaded %s after %d ms.",
                                                                displayedAssetName,
                                                                loadDocumentTimeout),
                                                        Toast.LENGTH_SHORT)
                                                .show();

                                        setDocumentFromUri(Uri.fromFile(documentFile), null);
                                    }
                                }),
                        loadDocumentTimeout);
    }
}
