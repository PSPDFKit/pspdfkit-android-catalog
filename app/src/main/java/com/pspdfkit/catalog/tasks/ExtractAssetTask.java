/*
 *   Copyright © 2014-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.pspdfkit.catalog.BuildConfig;
import com.pspdfkit.catalog.utils.StringUtils;
import com.pspdfkit.document.download.DownloadJob;
import com.pspdfkit.document.download.DownloadRequest;
import com.pspdfkit.document.download.source.AssetDownloadSource;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.io.FilenameFilter;

/**
 * Helper class for asynchronously pulling a PDF document from the app's assets into the internal
 * device storage.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class ExtractAssetTask {
    private static final String PREFS_NAME = "PSPDFCatalog.ExtractAssetTask";

    /**
     * The catalog app keeps track of the last used version code. Whenever this version code
     * changes, it will automatically clean up all extracted assets, to make sure the newest version
     * of assets is correctly copied to the device.
     */
    private static final String PREF_LAST_USED_VERSION = "PSPDFCatalog.LAST_USED_VERSION";

    /** Filter for listing files that have the ".pdf" file extension. */
    private static final FilenameFilter PDF_FILTER =
            (dir, filename) -> filename.length() > 4 && ".pdf".equals(filename.substring(filename.length() - 4));

    /**
     * Extracts the file at {@code assetPath} from the app's assets into the private app directory.
     *
     * @param assetPath Path pointing to a file inside the app's assets.
     * @param exampleTitle Title of the activity. This is used to keep separate copies of assets for
     *     every example.
     * @param context Context used to retrieve the referenced file from the app's assets.
     * @param listener A listener notified of extraction completion.
     */
    public static void extract(
            @NonNull final String assetPath,
            @NonNull final String exampleTitle,
            @NonNull final Context context,
            @Nullable final OnDocumentExtractedListener listener) {
        extract(assetPath, exampleTitle, context, false, listener);
    }

    /**
     * Extracts the file at {@code assetPath} from the app's assets into the private app directory.
     *
     * @param assetPath Path pointing to a file inside the app's assets.
     * @param exampleTitle Title of the activity. This is used to keep separate copies of assets for
     *     every example.
     * @param context Context used to retrieve the referenced file from the app's assets.
     * @param overwriteExisting Whether an existing file in the private app directory should be
     *     overwritten.
     * @param listener A listener notified of extraction completion.
     */
    public static void extract(
            @NonNull final String assetPath,
            @NonNull final String exampleTitle,
            @NonNull final Context context,
            final boolean overwriteExisting,
            @Nullable final OnDocumentExtractedListener listener) {
        extract(assetPath, exampleTitle, context, overwriteExisting, null, listener);
    }

    /**
     * Extracts the file at {@code assetPath} from the app's assets into the private app directory.
     *
     * @param assetPath Path pointing to a file inside the app's assets.
     * @param exampleTitle Title of the activity. This is used to keep separate copies of assets for
     *     every example.
     * @param context Context used to retrieve the referenced file from the app's assets.
     * @param overwriteExisting Whether an existing file in the private app directory should be
     *     overwritten.
     * @param fileExtension An optional file extension that should be used for the extracted file
     *     (otherwise the file will have a random extension).
     * @param listener A listener notified of extraction completion.
     */
    public static void extract(
            @NonNull final String assetPath,
            @NonNull final String exampleTitle,
            @NonNull final Context context,
            final boolean overwriteExisting,
            @Nullable final String fileExtension,
            @Nullable final OnDocumentExtractedListener listener) {
        extractAsync(assetPath, exampleTitle, context, overwriteExisting, fileExtension)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess((file) -> {
                    if (listener != null) {
                        listener.onDocumentExtracted(file);
                    }
                })
                .subscribe();
    }

    /**
     * Extracts the file at {@code assetPath} from the app's assets into the private app directory.
     *
     * @param assetPath Path pointing to a file inside the app's assets.
     * @param exampleTitle Title of the activity. This is used to keep separate copies of assets for
     *     every example.
     * @param context Context used to retrieve the referenced file from the app's assets.
     * @param overwriteExisting Whether an existing file in the private app directory should be
     *     overwritten.
     * @param fileExtension An optional file extension that should be used for the extracted file
     *     (otherwise the file will have a random extension).
     * @return Single emitting the extracted file.
     */
    @NonNull
    public static Single<File> extractAsync(
            @NonNull final String assetPath,
            @NonNull final String exampleTitle,
            @NonNull final Context context,
            final boolean overwriteExisting,
            @Nullable final String fileExtension) {
        return Single.<File>create((emitter) -> {
                    cleanUpOldAssets(context);
                    final File outputFile;
                    if (fileExtension != null) {
                        outputFile = new File(
                                context.getFilesDir(),
                                assetPath + "_" + StringUtils.sha1(exampleTitle) + "." + fileExtension);
                    } else {
                        outputFile = new File(context.getFilesDir(), assetPath + "_" + StringUtils.sha1(exampleTitle));
                    }
                    final DownloadRequest request = new DownloadRequest.Builder(context)
                            .source(new AssetDownloadSource(context, assetPath))
                            .outputFile(outputFile)
                            .overwriteExisting(overwriteExisting)
                            .build();
                    final DownloadJob job = DownloadJob.startDownload(request);
                    job.setProgressListener(new DownloadJob.ProgressListenerAdapter() {
                        @Override
                        public void onComplete(@NonNull File output) {
                            emitter.onSuccess(output);
                        }

                        @Override
                        public void onError(@NonNull Throwable exception) {
                            super.onError(exception);
                            emitter.tryOnError(exception);
                        }
                    });
                    emitter.setCancellable(job::cancel);
                })
                .subscribeOn(Schedulers.io());
    }

    /** Checks if all extracted assets are still up-to-date, and if not, cleans up those assets. */
    private static void cleanUpOldAssets(@NonNull final Context context) {
        final SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final int lastUsedVersionCode = preferences.getInt(PREF_LAST_USED_VERSION, -1);

        // If the last catalog version that was used while extracting is older than the current
        // version
        // we clean up all extracted assets.
        if (lastUsedVersionCode < BuildConfig.VERSION_CODE) {
            final File outputDir = context.getFilesDir();
            if (outputDir.exists()) {
                for (File document : outputDir.listFiles(PDF_FILTER)) {
                    document.delete();
                }
            }
        }

        preferences
                .edit()
                .putInt(PREF_LAST_USED_VERSION, BuildConfig.VERSION_CODE)
                .apply();
    }

    /** Listens for document extraction events. */
    public interface OnDocumentExtractedListener {
        /**
         * Called when there is a document extraction events.
         *
         * @param documentFile extracted {@link File}
         */
        void onDocumentExtracted(@NonNull File documentFile);
    }
}
