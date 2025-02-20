/*
 *   Copyright © 2014-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.PdfConfiguration;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.listeners.DocumentListener;
import com.pspdfkit.ui.PdfUiFragment;
import com.pspdfkit.ui.PdfUiFragmentBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SplitDocumentActivity extends AppCompatActivity implements ExtractAssetTask.OnDocumentExtractedListener {

    private List<DocumentListener> listeners = new ArrayList<>(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the custom activity layout which is going to hold our side-by-side fragments.
        setContentView(R.layout.activity_split_document);

        // This example uses a PDF document from the app's assets. Before loading it to the
        // fragments we extract it
        // to the internal device storage.
        ExtractAssetTask.extract(
                SdkExample.QUICK_START_GUIDE, getString(R.string.splitDocumentExampleTitle), this, this);
    }

    /**
     * This method is called after {@link ExtractAssetTask} has prepared the example's PDF document.
     * Here we set up our two fragments.
     */
    @Override
    public void onDocumentExtracted(@NonNull File documentFile) {
        // We create a plain configuration, providing our license, but without any specific
        // settings.
        final PdfConfiguration configuration = new PdfConfiguration.Builder().build();
        final Uri documentUri = Uri.fromFile(documentFile);

        setupFragment(R.id.fragmentContainer1, configuration, documentUri, "PdfViewer1");
        setupFragment(R.id.fragmentContainer2, configuration, documentUri, "PdfViewer2");
    }

    private void setupFragment(
            int fragmentContainerId,
            final PdfConfiguration configuration,
            Uri documentUri,
            @NonNull String pdfFragmentTag) {
        // Check if the fragment already exists in our layout, using the given fragmentContainerId.
        // This is the case if the
        // activity was recreated (e.g. due to an orientation change).
        PdfUiFragment fragment = (PdfUiFragment) getSupportFragmentManager().findFragmentById(fragmentContainerId);

        // If no fragment was found in the layout we create a new one and place it in the layout.
        if (fragment == null) {
            fragment = PdfUiFragmentBuilder.fromUri(this, documentUri)
                    .configuration(new PdfActivityConfiguration.Builder(this)
                            .configuration(configuration)
                            .build())
                    .pdfFragmentTag(pdfFragmentTag)
                    .build();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(fragmentContainerId, fragment)
                    .commit();
        }
    }
}
