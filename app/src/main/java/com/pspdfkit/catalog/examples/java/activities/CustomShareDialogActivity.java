/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.pspdfkit.catalog.R;
import com.pspdfkit.datastructures.Range;
import com.pspdfkit.document.printing.PrintOptions;
import com.pspdfkit.document.processor.PdfProcessorTask;
import com.pspdfkit.document.sharing.SharingOptions;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.dialog.BaseDocumentPrintDialog;
import com.pspdfkit.ui.dialog.BaseDocumentSharingDialog;
import com.pspdfkit.ui.dialog.DocumentSharingDialogConfiguration;
import com.pspdfkit.ui.dialog.DocumentSharingDialogFactory;
import java.util.Collections;

/**
 * This example shows how to create a custom share dialog using {@link
 * DocumentSharingDialogFactory}.
 */
public class CustomShareDialogActivity extends PdfActivity {

    /** A custom share dialog that always only shares the current page. */
    public static class CustomSharingDialog extends BaseDocumentSharingDialog {

        private DialogLayout dialogLayout;

        public CustomSharingDialog() {}

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            dialogLayout = new DialogLayout(getConfiguration(), getContext());

            dialogLayout.positiveButton.setOnClickListener(v -> {
                // Once the user is done making changes be sure to call the listener with
                // your SharingOptions.
                if (getListener() != null) {
                    getListener().onAccept(getSharingOptions());
                    dismiss();
                }
            });

            return dialogLayout.createDialog();
        }

        private SharingOptions getSharingOptions() {
            // If the checkbox is checked we flatten the annotations so they can't be edited in the
            // shared document.
            PdfProcessorTask.AnnotationProcessingMode annotationProcessingMode =
                    PdfProcessorTask.AnnotationProcessingMode.KEEP;
            if (dialogLayout.flattenAnnotations.isChecked()) {
                annotationProcessingMode = PdfProcessorTask.AnnotationProcessingMode.FLATTEN;
            }
            return new SharingOptions(
                    annotationProcessingMode,
                    // So only the current page is shared.
                    Collections.singletonList(new Range(getConfiguration().getCurrentPage(), 1)),
                    // The document title the user chose.
                    dialogLayout.documentNameEditText.getText().toString());
        }
    }

    /** A custom print dialog that always only prints the current page. */
    public static class CustomPrintDialog extends BaseDocumentPrintDialog {

        private DialogLayout dialogLayout;

        private PrintOptions getPrintOptions() {
            // If the checkbox is checked we print the annotations.
            return new PrintOptions(
                    dialogLayout.flattenAnnotations.isChecked(),
                    Collections.singletonList(new Range(getConfiguration().getCurrentPage(), 1)),
                    dialogLayout.documentNameEditText.getText().toString());
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            dialogLayout = new DialogLayout(getConfiguration(), getContext());

            dialogLayout.flattenAnnotations.setText(R.string.print_annotations);
            dialogLayout.positiveButton.setOnClickListener(v -> {
                // Once the user is done making changes be sure to call the listener with
                // your SharingOptions.
                if (getListener() != null) {
                    getListener().onAccept(getPrintOptions());
                    dismiss();
                }
            });

            return dialogLayout.createDialog();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Tell the activity about our dialog creators.
        setDocumentSharingDialogFactory(CustomSharingDialog::new);
        setDocumentPrintDialogFactory(CustomPrintDialog::new);

        // Tell the activity about our option providers.
        // By providing options we prevent the share/print dialog from appearing.
        setSharingOptionsProvider((document, currentPage) -> {
            if (currentPage == 2) {
                return new SharingOptions(PdfProcessorTask.AnnotationProcessingMode.DELETE);
            } else {
                // Returning null causes the the sharing dialog to appear.
                return null;
            }
        });
        setPrintOptionsProvider((document, currentPage) -> {
            if (currentPage == 2) {
                return new PrintOptions(true);
            } else {
                // Returning null causes the the printing dialog to appear.
                return null;
            }
        });
    }

    /** Wraps the UI so it can be used in both the sharing and printing dialog. */
    static class DialogLayout {
        DocumentSharingDialogConfiguration configuration;
        Context context;

        View root;
        EditText documentNameEditText;
        TextView positiveButton;
        CheckBox flattenAnnotations;

        DialogLayout(DocumentSharingDialogConfiguration configuration, Context context) {
            this.configuration = configuration;
            this.context = context;

            root = View.inflate(context, R.layout.custom_document_sharing_dialog, null);

            // EditText to enter the title of the printed document.
            documentNameEditText = root.findViewById(R.id.share_dialog_document_name);
            documentNameEditText.setText(configuration.getInitialDocumentName());
            documentNameEditText.clearFocus();

            // Changes the annotation processing behaviour.
            flattenAnnotations = root.findViewById(R.id.share_dialog_flatten_annotations);

            positiveButton = root.findViewById(R.id.share_button);
            // Use the button text from the passed configuration.
            positiveButton.setText(configuration.getPositiveButtonText());
        }

        /** Creates a dialog to be used in the custom sharing and printing dialogs. */
        Dialog createDialog() {
            return new AlertDialog.Builder(context)
                    .setCancelable(true)
                    // You also get the dialog title from the configuration.
                    .setTitle(configuration.getDialogTitle())
                    .setView(root)
                    .create();
        }
    }
}
