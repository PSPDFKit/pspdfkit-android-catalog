/*
 *   Copyright © 2018-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import static com.pspdfkit.catalog.tasks.ExtractAssetTask.extract;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.document.DocumentSource;
import com.pspdfkit.document.PdfDocumentLoader;
import com.pspdfkit.document.editor.page.DialogNewPageFactory;
import com.pspdfkit.document.editor.page.NewPageDialog;
import com.pspdfkit.document.editor.page.PageTemplate;
import com.pspdfkit.document.providers.AssetDataProvider;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import com.pspdfkit.ui.PdfThumbnailGrid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CustomPageTemplatesExample extends SdkExample {
    public CustomPageTemplatesExample(Context context) {
        super(context, R.string.customPageTemplateExampleTitle, R.string.customPageTemplateExampleDescription);
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // Extract the document from the assets.
        extract(ANNOTATIONS_EXAMPLE, getTitle(), context, documentFile -> {
            // To start the example create a launch intent using the builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(CustomPageTemplateActivity.class)
                    .build();

            context.startActivity(intent);
        });
    }

    public static class CustomPageTemplateActivity extends PdfActivity {

        /** The additional page templates we want to add. */
        private List<PageTemplate> pageTemplates;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            pageTemplates = new ArrayList<>();
            // Document template.
            try {
                pageTemplates.add(new PageTemplate(
                        PdfDocumentLoader.openDocument(
                                this, new DocumentSource(new AssetDataProvider(SdkExample.WELCOME_DOC))),
                        8,
                        // The image should be 100x100dp.
                        getString(R.string.page_template_title),
                        ContextCompat.getDrawable(this, R.drawable.page_template_preview)));
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't open '" + SdkExample.WELCOME_DOC + "' file.", e);
            }

            PdfThumbnailGrid thumbnailGrid = getPSPDFKitViews().getThumbnailGridView();

            // Set a DialogNewPageFactory that knows about our custom page templates.
            thumbnailGrid.setNewPageFactory(new DialogNewPageFactory(getSupportFragmentManager(), null, pageTemplates));

            // If we displayed the dialog before restore it with our page templates.
            NewPageDialog.restore(
                    getSupportFragmentManager(), pageTemplates, thumbnailGrid.getDefaultNewPageDialogCallback());
        }
    }
}
