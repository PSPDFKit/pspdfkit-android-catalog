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
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pspdfkit.catalog.R;
import com.pspdfkit.document.processor.PdfProcessorTask;
import com.pspdfkit.document.sharing.DefaultDocumentSharingController;
import com.pspdfkit.document.sharing.DocumentSharingIntentHelper;
import com.pspdfkit.document.sharing.DocumentSharingManager;
import com.pspdfkit.document.sharing.ShareTarget;
import com.pspdfkit.document.sharing.SharingOptions;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.actionmenu.ActionMenu;
import com.pspdfkit.ui.actionmenu.ActionMenuItem;
import com.pspdfkit.ui.actionmenu.ActionMenuListener;
import com.pspdfkit.ui.actionmenu.FixedActionMenuItem;
import com.pspdfkit.ui.actionmenu.SharingMenu;
import com.pspdfkit.ui.actionmenu.SimpleActionMenuListener;
import java.util.Collections;

/**
 * This activity showcases how to add custom sharing action to sharing menu displayed in a {@link
 * BottomSheetDialog}.
 */
public class CustomSharingMenuActivity extends PdfActivity implements ActionMenuListener {

    private static final String STATE_SHOWING_MAIL_TO_SHARING_MENU = "STATE_SHOWING_MAIL_TO_SHARING_MENU";

    @Nullable
    private SharingMenu mailToActionMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set listener for sharing menu.
        setSharingActionMenuListener(this);

        // Restore "Mail to" sharing menu if shown before orientation change.
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(STATE_SHOWING_MAIL_TO_SHARING_MENU, false)) {
                showMailToSharingMenu();
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_SHOWING_MAIL_TO_SHARING_MENU, mailToActionMenu != null);
    }

    /** Override onPrepareActionMenu(ActionMenu) to modify sharing menu contents. */
    @Override
    public boolean onPrepareActionMenu(@NonNull ActionMenu actionMenu) {
        // Add fixed menu item to action menu.
        actionMenu.addMenuItem(
                new FixedActionMenuItem(this, R.id.item_send_mail, R.drawable.ic_custom_sharing, R.string.mail));
        // Make sure to return true, if you want sharing menu to display your actions.
        return true;
    }

    @Override
    public void onDisplayActionMenu(@NonNull ActionMenu actionMenu) {}

    @Override
    public void onRemoveActionMenu(@NonNull ActionMenu actionMenu) {}

    @Override
    public boolean onActionMenuItemClicked(@NonNull ActionMenu actionMenu, @NonNull ActionMenuItem menuItem) {
        if (menuItem.getItemId() == R.id.item_send_mail) {
            actionMenu.dismiss();
            // Show new sharing menu with "Mail to" action.
            showMailToSharingMenu();
            return true;
        }
        return false;
    }

    /** Override to handle click events for your custom sharing menu items. */
    @Override
    public boolean onActionMenuItemLongClicked(@NonNull ActionMenu actionMenu, @NonNull ActionMenuItem clickedItem) {
        return false;
    }

    private void showMailToSharingMenu() {
        mailToActionMenu = new SharingMenu(this, shareTarget -> {
            // Start sharing flow.
            DocumentSharingManager.shareDocument(
                    new MailToDocumentSharingController(CustomSharingMenuActivity.this, shareTarget),
                    getDocument(),
                    new SharingOptions(PdfProcessorTask.AnnotationProcessingMode.FLATTEN));
        });
        // Register listener for action menu to get notified when menu is hidden.
        mailToActionMenu.addActionMenuListener(new SimpleActionMenuListener() {
            @Override
            public void onRemoveActionMenu(@NonNull ActionMenu actionMenu) {
                mailToActionMenu = null;
            }
        });
        // Set menu title.
        mailToActionMenu.setTitle(R.string.mail_to);
        // Make sharing menu show apps that handle mail to action.
        mailToActionMenu.setShareIntents(Collections.singletonList(getMailToIntent(Uri.EMPTY)));
        // Show dialog.
        mailToActionMenu.show();
    }

    @NonNull
    private Intent getMailToIntent(@NonNull Uri shareUri) {
        Intent mailToIntent = ShareCompat.IntentBuilder.from(CustomSharingMenuActivity.this)
                // Use "application/pdf" as mime type.
                .setType(DocumentSharingIntentHelper.MIME_TYPE_PDF)
                // Add shared document uri as data stream.
                .addStream(shareUri)
                // Optionally specify initial email data - to, cc, subject, body text etc.
                .addEmailTo("mail@to.com")
                .setSubject("Subject")
                .setText("I'm email body.")
                // Build share intent.
                .getIntent();

        // Target email apps by changing the Intent action to SENDTO and
        // by limiting the Intent to apps that can handle mailto:// URIs.
        mailToIntent.setAction(Intent.ACTION_SENDTO);
        mailToIntent.setData(Uri.parse("mailto:"));

        return mailToIntent;
    }

    /**
     * Example implementation of a custom share controller that shares document to single {@link
     * ShareTarget}.
     */
    private class MailToDocumentSharingController extends DefaultDocumentSharingController {

        public MailToDocumentSharingController(@NonNull Context context, @NonNull ShareTarget shareTarget) {
            super(context, shareTarget);
        }

        @Override
        public void onDocumentPrepared(@NonNull Uri shareUri) {
            // Guard for null context. This can happen when sharing controller is detached
            // from it's owning activity (for example while changing orientation).
            if (getContext() == null) return;

            // Build share intent.
            final Intent shareIntent = getMailToIntent(shareUri);
            if (getShareTarget() != null) {
                // Set package to share target package.
                shareIntent.setPackage(getShareTarget().getPackageName());
                // Finally start share intent.
                getContext().startActivity(shareIntent);
            }
        }
    }
}
