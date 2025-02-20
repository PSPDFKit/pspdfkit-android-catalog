/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.utils;

import android.os.Debug;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.IOException;

public class HeapDumpingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final String HPROF_DUMP_BASENAME = "LeakingApp.dalvik-hprof";
    private static final String LOG_TAG = "PSPDFKit";
    private final String dataDir;

    @Nullable
    private final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    public HeapDumpingUncaughtExceptionHandler(@NonNull String dataDir) {
        this.dataDir = dataDir;
        this.defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        String absPath = new File(dataDir, HPROF_DUMP_BASENAME).getAbsolutePath();
        if (ex.getClass().equals(OutOfMemoryError.class)) {
            try {
                Log.e(LOG_TAG, "OOM, dumping heap to " + absPath);
                Debug.dumpHprofData(absPath);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to dump heap.", e);
            }
        }

        if (defaultUncaughtExceptionHandler != null) {
            defaultUncaughtExceptionHandler.uncaughtException(thread, ex);
        } else {
            Log.e(LOG_TAG, "Uncaught exception!", ex);
        }
    }
}
