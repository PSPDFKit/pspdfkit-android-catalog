/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.document.download.DownloadJob;
import com.pspdfkit.document.download.DownloadProgressFragment;
import com.pspdfkit.document.download.DownloadRequest;
import com.pspdfkit.document.download.Progress;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import java.io.File;
import java.util.Locale;

/**
 * This is an example showing how to use the {@link DownloadJob} to download a PDF document from the
 * web with a custom DownloadProgressFragment.
 */
public class CustomDocumentDownloadExample extends SdkExample {

    /** Our custom progress fragment, that displays the name of the currently downloading file. */
    public static class CustomDownloadProgressFragment extends DownloadProgressFragment {

        private TextView downloadDescription;
        private TextView downloadPercent;
        private TextView downloadSize;
        private ProgressBar progressBar;

        /** Formats the progress like "0.6/12 MB". */
        private static String formatProgressSize(Progress progress) {
            int unit = 1000;
            int exp;
            char pre;
            if (progress.totalBytes < unit) {
                exp = 0;
                pre = 'B';
            } else {
                exp = (int) (Math.log((double) progress.totalBytes) / Math.log((double) unit));
                pre = "KMGTPE".charAt(exp - 1);
            }
            return String.format(
                    Locale.getDefault(),
                    "%.1f/%.1f %sB",
                    progress.bytesReceived / Math.pow((double) unit, (double) exp),
                    progress.totalBytes / Math.pow((double) unit, (double) exp),
                    pre);
        }

        @NonNull
        @Override
        protected Dialog createDialog() {
            View root = LayoutInflater.from(getContext()).inflate(R.layout.custom_download_dialog, null);
            // Setup our views.
            downloadDescription = root.findViewById(R.id.downloadDescription);
            downloadPercent = root.findViewById(R.id.downloadPercent);
            downloadSize = root.findViewById(R.id.downloadSize);
            progressBar = root.findViewById(R.id.progressBar);

            downloadDescription.setText(getString(
                    R.string.dialog_download_description,
                    getJob().getOutputFile().getName()));
            return new AlertDialog.Builder(getContext()).setView(root).create();
        }

        @Override
        protected void updateProgress(@NonNull Progress progress) {
            double percent = ((double) progress.bytesReceived / (double) progress.totalBytes) * 100;
            // Update the displayed progress.
            downloadPercent.setText(String.format(Locale.getDefault(), "%.0f%%", percent));
            downloadSize.setText(formatProgressSize(progress));
            progressBar.setProgress((int) (progress.bytesReceived / 1024));
        }

        @Override
        protected void configureDialog(@NonNull Progress progress, boolean isIndeterminate) {
            if (!isIndeterminate) {
                // If the progress isn't indeterminate we can show the exact progress.
                downloadPercent.setVisibility(View.VISIBLE);
                downloadSize.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(false);
                progressBar.setMax((int) (progress.totalBytes / 1024));
            }
        }
    }

    public CustomDocumentDownloadExample(@NonNull final Context context) {
        super(
                context.getString(R.string.customDownloadDialogExampleTitle),
                context.getString(R.string.customDownloadDialogExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {

        // Build a download request based on various input parameters.
        final DownloadRequest request = new DownloadRequest.Builder(context)
                .uri("https://nutrient.io/downloads/case-study-box.pdf")
                .outputFile(new File(context.getDir("documents", Context.MODE_PRIVATE), "case-study-box.pdf"))
                .overwriteExisting(true)
                .build();

        // This will initiate the download.
        final DownloadJob job = DownloadJob.startDownload(request);
        final DownloadProgressFragment fragment = new CustomDownloadProgressFragment();
        fragment.show(((FragmentActivity) context).getSupportFragmentManager(), "download-fragment");
        fragment.setJob(job);

        job.setProgressListener(new DownloadJob.ProgressListenerAdapter() {
            @Override
            public void onComplete(@NonNull File output) {
                final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(output))
                        .configuration(configuration.build())
                        .build();

                context.startActivity(intent);
            }

            @Override
            public void onError(@NonNull Throwable exception) {
                new AlertDialog.Builder(context)
                        .setMessage(
                                "There was an error downloading the example PDF file. For further information see Logcat.")
                        .show();
            }
        });
    }
}
