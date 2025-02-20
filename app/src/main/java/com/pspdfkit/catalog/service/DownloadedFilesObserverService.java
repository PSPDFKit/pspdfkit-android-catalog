/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StrictMode;
import android.webkit.MimeTypeMap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Observes files in downloaded files and opens them in {@link PdfActivity}. */
public class DownloadedFilesObserverService extends Service {

    /** Starts the file observer service. */
    public static void startService(@NonNull final Context context) {
        final Intent downloadedFilesService = new Intent(context, DownloadedFilesObserverService.class);
        context.startService(downloadedFilesService);
    }

    /** Stops the file observer service. */
    public static void stopService(@NonNull final Context context) {
        final Intent downloadedFilesService = new Intent(context, DownloadedFilesObserverService.class);
        context.stopService(downloadedFilesService);
    }

    /**
     * Holds known files for the duration of the process lifetime. Used to prevent opening files
     * that were saved.
     */
    @NonNull
    private static final Set<Uri> ignoredFiles = new HashSet<>();

    /** Delay after last I/O operation before opening {@link PdfActivity}. */
    private static final long OPEN_ACTIVITY_DELAY_MS = 500;

    private static final String MIME_TYPE_PDF = "application/pdf";
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static final String MIME_TYPE_IMAGE_PREFIX = "image/";

    @NonNull
    private final AtomicBoolean isObserving = new AtomicBoolean(false);

    @Nullable
    private FileObserver fileObserver;

    @Nullable
    private Runnable openActivityRunnable;

    @NonNull
    Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public int onStartCommand(@NonNull final Intent intent, final int flags, final int startId) {
        if (isObserving.compareAndSet(false, true)) {
            final File observedDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            final StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
            try {
                // Ignore all existing files.
                final File[] files = observedDir.listFiles();
                if (files != null) {
                    for (final File file : files) {
                        if (file.isFile()) {
                            ignoredFiles.add(Uri.fromFile(file));
                        }
                    }
                }
            } finally {
                StrictMode.setThreadPolicy(oldPolicy);
            }

            // Start file observer for new files created in the downloads directory.
            fileObserver =
                    new FileObserver(
                            observedDir.getAbsolutePath(),
                            FileObserver.CLOSE_WRITE
                                    | FileObserver.MODIFY
                                    | FileObserver.MOVED_TO
                                    | FileObserver.DELETE) {

                        @Override
                        public void onEvent(final int event, final String path) {
                            if (path == null || !isObserving.get()) return;

                            final Context context = getApplicationContext();
                            if (context == null) return;

                            final Uri fileUri = Uri.fromFile(new File(observedDir, path));
                            if (ignoredFiles.contains(fileUri)) {
                                // Un-ignore deleted files. This can happen when the file is
                                // getting replaced by drag'n'dropping or pushing via adb.
                                if (event == DELETE) {
                                    ignoredFiles.remove(fileUri);
                                }
                                return;
                            }

                            if (!isSupportedFile(fileUri)) return;

                            if (openActivityRunnable != null) {
                                handler.removeCallbacks(openActivityRunnable);
                            }
                            openActivityRunnable = () -> {
                                // Skip launching the activity if the file is already
                                // ignored.
                                if (ignoredFiles.contains(fileUri)) return;

                                ignoredFiles.add(fileUri);

                                final PdfActivityIntentBuilder builder = isImageFile(fileUri)
                                        ? PdfActivityIntentBuilder.fromImageUri(context, fileUri)
                                        : PdfActivityIntentBuilder.fromUri(context, fileUri);

                                final Intent intent = builder.build();
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            };
                            handler.postDelayed(openActivityRunnable, OPEN_ACTIVITY_DELAY_MS);
                        }
                    };
            fileObserver.startWatching();
        }
        return Service.START_NOT_STICKY;
    }

    private boolean isSupportedFile(@NonNull final Uri fileUri) {
        final String mimeTypeForUri = getMimeTypeForUri(fileUri);
        return mimeTypeForUri.contains(MIME_TYPE_PDF) || mimeTypeForUri.startsWith(MIME_TYPE_IMAGE_PREFIX);
    }

    private boolean isImageFile(@NonNull final Uri fileUri) {
        final String mimeTypeForUri = getMimeTypeForUri(fileUri);
        return mimeTypeForUri.startsWith("image/");
    }

    @NonNull
    private static String getMimeTypeForUri(@NonNull final Uri fileUri) {
        final String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
        final String mimeType = fileExtension == null
                ? DEFAULT_MIME_TYPE
                : MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

        return mimeType != null ? mimeType : DEFAULT_MIME_TYPE;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fileObserver != null) {
            isObserving.set(false);
            fileObserver.stopWatching();
        }
        if (openActivityRunnable != null) {
            handler.removeCallbacks(openActivityRunnable);
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        // For communication return IBinder implementation, not needed in this service.
        return null;
    }
}
