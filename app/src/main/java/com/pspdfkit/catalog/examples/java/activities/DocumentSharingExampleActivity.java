/*
 *   Copyright © 2014-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import com.pspdfkit.catalog.R;
import com.pspdfkit.datastructures.Range;
import com.pspdfkit.document.processor.PdfProcessorTask;
import com.pspdfkit.document.sharing.DefaultDocumentSharingController;
import com.pspdfkit.document.sharing.DocumentSharingController;
import com.pspdfkit.document.sharing.DocumentSharingIntentHelper;
import com.pspdfkit.document.sharing.DocumentSharingManager;
import com.pspdfkit.document.sharing.DocumentSharingProvider;
import com.pspdfkit.document.sharing.ShareAction;
import com.pspdfkit.document.sharing.SharingOptions;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.dialog.DocumentSharingDialog;
import com.pspdfkit.ui.dialog.DocumentSharingDialogConfiguration;
import java.util.List;

/**
 * This activity showcases how to implement custom sharing actions. For sharing to work you need to
 * declare {@link DocumentSharingProvider} in your {@code AndroidManifest.xml}.
 */
public class DocumentSharingExampleActivity extends PdfActivity {

    /** This example shows how to use {@link DocumentSharingDialog}. */
    private void customSharingDialog() {
        // Build dialog configuration.
        final DocumentSharingDialogConfiguration configuration = new DocumentSharingDialogConfiguration.Builder(
                        this, getDocument(), getPageIndex())
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

    /** This example shows how to implement custom {@link DocumentSharingController}. */
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
