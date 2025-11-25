package com.pspdfkit.catalog.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val FREEZE_DETECTOR_ENABLED: Boolean = false

fun startFreezeDetectorIfEnabled() {
    if (!FREEZE_DETECTOR_ENABLED) return

    val handler = Handler(Looper.getMainLooper())
    val mainThread = Looper.getMainLooper().thread

    Thread {
        while (true) {
            val latch = CountDownLatch(1)
            handler.post { latch.countDown() }
            if (!latch.await(10, TimeUnit.SECONDS)) {
                Log.e("FreezeDetector", "⚠️ UI thread unresponsive! Stack trace:")
                mainThread.stackTrace.forEach { Log.e("FreezeDetector", it.toString()) }
            }
            Thread.sleep(500)
        }
    }.start()
}
