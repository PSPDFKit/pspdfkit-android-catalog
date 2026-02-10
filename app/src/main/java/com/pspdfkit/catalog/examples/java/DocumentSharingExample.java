/*
 *   Copyright © 2014-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.configuration.sharing.ShareFeatures;
import com.pspdfkit.datastructures.Range;
import com.pspdfkit.document.processor.PdfProcessorTask;
import com.pspdfkit.document.sharing.DefaultDocumentSharingController;
import com.pspdfkit.document.sharing.DocumentSharingIntentHelper;
import com.pspdfkit.document.sharing.DocumentSharingManager;
import com.pspdfkit.document.sharing.ShareAction;
import com.pspdfkit.document.sharing.SharingOptions;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import com.pspdfkit.ui.dialog.DocumentSharingDialog;
import com.pspdfkit.ui.dialog.DocumentSharingDialogConfiguration;
import java.util.EnumSet;
import java.util.List;

public class DocumentSharingExample extends SdkExample {
    public DocumentSharingExample(Context context) {
        super(context, R.string.documentSharingExampleTitle, R.string.documentSharingExampleDescription);
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // For sake of simplicity, deactivate actions.
        configuration
                .annotationListEnabled(false)
                .searchEnabled(false)
                .outlineEnabled(false)
                .setEnabledShareFeatures(EnumSet.noneOf(ShareFeatures.class))
                .printingEnabled(false)
                .thumbnailGridEnabled(false);

        // First extract the document from the assets.
        ExtractAssetTask.extract(ANNOTATIONS_EXAMPLE, getTitle(), context, documentFile -> {
            // To start the DocumentSharingExampleActivity create a launch intent using the
            // builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(DocumentSharingExampleActivity.class)
                    .build();

            // Start the DocumentSharingExampleActivity for the extracted document.
            context.startActivity(intent);
        });
    }

    /**
     * This activity showcases how to implement custom sharing actions. For sharing to work you need to
     * declare {@link com.pspdfkit.document.sharing.DocumentSharingProvider} in your {@code AndroidManifest.xml}.
     */
    public static class DocumentSharingExampleActivity extends PdfActivity {

        /** This example shows how to use {@link DocumentSharingDialog}. */
        private void customSharingDialog() {
            // Build dialog configuration.
            int pageIndex = Math.max(0, getPageIndex());
            final DocumentSharingDialogConfiguration configuration = new DocumentSharingDialogConfiguration.Builder(
                            this, getDocument(), pageIndex)
                    .dialogTitle("Custom sharing dialog title")
                    .positiveButtonText("View")
                    .build();

            // Show sharing dialog.
            DocumentSharingDialog.show(
                    // Provide fragment manager instance to create dialog.
                    getSupportFragmentManager(),
                    // Provide sharing dialog configuration.
                    configuration,
                    // Provide dialog result listener.
                    getCustomSharingDialogListener());
        }

        @NonNull
        private DocumentSharingDialog.SharingDialogListener getCustomSharingDialogListener() {
            return new DocumentSharingDialog.SharingDialogListener() {
                @Override
                public void onAccept(@NonNull SharingOptions shareOptions) {
                    DocumentSharingManager.shareDocument(
                            DocumentSharingExampleActivity.this,
                            getDocument(),
                            ShareAction.VIEW,
                            // Use share options configured in share dialog.
                            shareOptions);
                }

                @Override
                public void onDismiss() {}
            };
        }

        @Override
        protected void onResume() {
            super.onResume();

            // As sharing dialog listener can't be retained, restore it in dialog after rotation.
            DocumentSharingDialog.restore(getSupportFragmentManager(), getCustomSharingDialogListener());
        }

        /** Creates menu items that will trigger document processing. */
        @Override
        public boolean onCreateOptionsMenu(@NonNull Menu menu) {
            super.onCreateOptionsMenu(menu);

            // Inflate menu with custom actions showcasing custom sharing.
            getMenuInflater().inflate(R.menu.sharing_example, menu);

            return true;
        }

        /** Triggered by selecting an action from the overflow menu in the action bar. */
        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            boolean handled = false;

            final int itemId = item.getItemId();
            if (itemId == R.id.item_sharing_dialog) {
                customSharingDialog();
                handled = true;
            } else if (itemId == R.id.item_send_mail) {
                shareViaMail();
                handled = true;
            } else if (itemId == R.id.item_flatten_annotations) {
                shareWithFlattenedAnnotations();
                handled = true;
            } else if (itemId == R.id.item_extract_pages) {
                shareFromRange();
                handled = true;
            }
            return handled || super.onOptionsItemSelected(item);
        }

        /** This example shows how to implement custom {@link com.pspdfkit.document.sharing.DocumentSharingController}. */
        private void shareViaMail() {
            DocumentSharingManager.shareDocument(
                    new MailToDocumentSharingController(this),
                    getDocument(),
                    new SharingOptions(PdfProcessorTask.AnnotationProcessingMode.FLATTEN));
        }

        /** This example shows how to share only certain range of document pages. */
        private void shareFromRange() {
            // Create list of page ranges to extract.
            final List<Range> extractedPages =
                    SharingOptions.parsePageRange("3-5,11", getDocument().getPageCount());

            DocumentSharingManager.shareDocument(
                    this,
                    getDocument(),
                    // Use view action to target PDF viewers.
                    ShareAction.VIEW,
                    // Keep annotations intact and share only specified page ranges.
                    new SharingOptions(PdfProcessorTask.AnnotationProcessingMode.KEEP, extractedPages));
        }

        /** This example shows how to share whole document with flattened annotations. */
        private void shareWithFlattenedAnnotations() {
            DocumentSharingManager.shareDocument(
                    this,
                    getDocument(),
                    // Use view action to target PDF viewers.
                    ShareAction.VIEW,
                    // Flatten annotations.
                    new SharingOptions(PdfProcessorTask.AnnotationProcessingMode.FLATTEN));
        }

        /**
         * Example implementation of a custom share controller that shares document to mail apps only.
         * Note that standard {@link ShareAction#SEND} shares to all apps that accept PDF files - mail
         * apps, file managers, social network apps etc.
         */
        private class MailToDocumentSharingController extends DefaultDocumentSharingController {

            public MailToDocumentSharingController(@NonNull Context context) {
                super(context);
            }

            @Override
            public void onDocumentPrepared(@NonNull Uri shareUri) {
                Context context = getContext();
                if (context == null) return;

                // Build share intent.
                final Intent mailToIntent = ShareCompat.IntentBuilder.from(DocumentSharingExampleActivity.this)
                        // Use "application/pdf" as mime type.
                        .setType(DocumentSharingIntentHelper.MIME_TYPE_PDF)
                        // Add shared document uri as data stream.
                        .addStream(shareUri)
                        // Optionally specify initial email data - to, cc, subject, body text
                        // etc.
                        .addEmailTo("mail@to.com")
                        .setSubject("Subject")
                        .setText("I'm email body.")
                        // Build share intent.
                        .getIntent();

                // Target email apps by limiting the intent to apps that can handle mailto:// URIs.
                mailToIntent.setAction(Intent.ACTION_SENDTO);
                mailToIntent.setData(Uri.parse("mailto:"));

                // Start the chooser intent.
                // Optionally supply title that will be displayed in the chooser.
                context.startActivity(Intent.createChooser(mailToIntent, "Send email with"));
            }
        }
    }
}
