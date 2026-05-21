package com.pspdfkit.catalog.utils

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object FreezeDetector {
    private const val TAG = "FreezeDetector"

    /** How long the UI thread must be unresponsive before we start sampling. */
    private const val FREEZE_THRESHOLD_MS = 1_500L

    /** Interval between stack samples while the UI thread is still frozen. */
    private const val SAMPLE_INTERVAL_MS = 250L

    /**
     * Flush the current run to logcat at least this often, even if the top frame
     * hasn't changed. Ensures logs surface during indefinite freezes / deadlocks
     * where the stack is parked on a single frame the whole time.
     */
    private const val FLUSH_INTERVAL_MS = 1_000L

    /** Pause between freeze checks when the UI thread is healthy. */
    private const val IDLE_INTERVAL_MS = 500L

    private var worker: Thread? = null

    @Synchronized
    fun start() {
        if (worker != null) return
        val handler = Handler(Looper.getMainLooper())
        val mainThread = Looper.getMainLooper().thread
        val thread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                val latch = CountDownLatch(1)
                handler.post { latch.countDown() }
                try {
                    if (!latch.await(FREEZE_THRESHOLD_MS, TimeUnit.MILLISECONDS)) {
                        sampleUntilResponsive(latch, mainThread)
                    }
                    Thread.sleep(IDLE_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }.apply {
            name = "FreezeDetector"
            isDaemon = true
        }
        worker = thread
        thread.start()
    }

    @Synchronized
    fun stop() {
        worker?.interrupt()
        worker = null
    }

    @Throws(InterruptedException::class)
    private fun sampleUntilResponsive(latch: CountDownLatch, mainThread: Thread) {
        val freezeStart = SystemClock.elapsedRealtime() - FREEZE_THRESHOLD_MS
        Log.e(TAG, "⚠️ UI thread unresponsive for >${FREEZE_THRESHOLD_MS}ms — sampling…")

        // Stream samples to logcat as they happen. Consecutive samples sharing
        // the same top frame are coalesced into a single "run" that is flushed
        // when the top frame changes (or the freeze ends), so logs surface in
        // real time without spamming identical stacks.
        var runStartMs = SystemClock.elapsedRealtime()
        var runTopFrame: StackTraceElement? = null
        var runStack: Array<StackTraceElement> = emptyArray()
        var runCount = 0
        var sampleCount = 0

        fun flushRun(endTimeMs: Long) {
            if (runCount == 0) return
            Log.e(
                TAG,
                "── ${runStartMs - freezeStart}ms→${endTimeMs - freezeStart}ms" +
                    " ($runCount sample${if (runCount == 1) "" else "s"})",
            )
            runStack.forEach { Log.e(TAG, "    $it") }
        }

        while (!latch.await(SAMPLE_INTERVAL_MS, TimeUnit.MILLISECONDS)) {
            val now = SystemClock.elapsedRealtime()
            val stack = mainThread.stackTrace
            val top = stack.firstOrNull()
            sampleCount++

            if (runCount == 0) {
                runStartMs = now
                runTopFrame = top
                runStack = stack
                runCount = 1
            } else if (top == runTopFrame) {
                runCount++
                runStack = stack
                // Periodic heartbeat: flush even though the top frame hasn't
                // changed, so deadlocks parked on one frame still surface.
                if (now - runStartMs >= FLUSH_INTERVAL_MS) {
                    flushRun(now)
                    runStartMs = now
                    runCount = 0
                }
            } else {
                flushRun(now)
                runStartMs = now
                runTopFrame = top
                runStack = stack
                runCount = 1
            }
        }

        val recoveredAt = SystemClock.elapsedRealtime()
        flushRun(recoveredAt)
        Log.e(
            TAG,
            "✅ UI thread recovered after ${recoveredAt - freezeStart}ms ($sampleCount samples)",
        )
    }
}
