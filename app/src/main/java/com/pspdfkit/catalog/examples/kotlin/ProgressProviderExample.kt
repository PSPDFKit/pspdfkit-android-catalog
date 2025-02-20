/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.utils.StringUtils
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.download.DownloadJob
import com.pspdfkit.document.download.DownloadRequest
import com.pspdfkit.document.download.Progress
import com.pspdfkit.document.providers.DataProvider
import com.pspdfkit.document.providers.InputStreamDataProvider
import com.pspdfkit.document.providers.ProgressDataProvider
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.CountDownLatch

/**
 * This example shows how to create a custom data provider that loads a document from the web and
 * shows the progress in the [PdfActivity]. Furthermore, it implements [Parcelable] to allow using
 * the data provider with [PdfActivity].
 */
class ProgressProviderExample(context: Context) : SdkExample(context, R.string.progressProviderExampleTitle, R.string.progressProviderExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Create an instance of the custom data provider. See the implementation details below.
        val dataProvider: DataProvider = RemoteDataProvider("https://pspdfkit.com/downloads/case-study-box.pdf")

        // Start the activity using our custom data provider.
        val intent = PdfActivityIntentBuilder.fromDataProvider(context, dataProvider)
            .configuration(configuration.build())
            .build()
        context.startActivity(intent)
    }
}

/**
 * Custom data provider for loading a PDF document from the web. This provider derives from
 * [InputStreamDataProvider] which handles loading data from a stream object.
 *
 * @param url The url of the PDF document.
 */
private class RemoteDataProvider(
    /** The url where the PDF document is located.  */
    private val url: String
) : InputStreamDataProvider(), ProgressDataProvider, Parcelable {

    /** Responsible for downloading our PDF.  */
    private var downloadJob: DownloadJob? = null

    /** Used to wait until the download is done.  */
    private val downloadLatch = CountDownLatch(1)

    /** Used to notify the PdfFragment of download progress updates.  */
    private val progressSubject = PublishSubject.create<Double>()

    override fun observeProgress(): Flowable<Double> { // We can just return our PublishSubject.
        return progressSubject.toFlowable(BackpressureStrategy.LATEST)
    }

    @Throws(Exception::class)
    override fun openInputStream(): InputStream {
        val downloadJob = startDownloadIfNotRunning()

        // We need to wait until our download is finished.
        downloadLatch.await()

        return FileInputStream(downloadJob.outputFile)
    }

    override fun getSize(): Long {
        val downloadJob = startDownloadIfNotRunning()

        // We need to wait until our download is finished.
        try {
            downloadLatch.await()
        } catch (ex: InterruptedException) {
            return DataProvider.FILE_SIZE_UNKNOWN.toLong()
        }

        return downloadJob.outputFile.length()
    }

    override fun getUid(): String {
        return StringUtils.sha1(url)
    }

    override fun getTitle(): String? {
        return url
    }

    /**
     * Starts our download if it wasn't already started.
     */
    private fun startDownloadIfNotRunning(): DownloadJob {
        var downloadJob = this.downloadJob
        if (downloadJob == null) {
            try {
                // We delay starting the download so the progress bar will appear,
                // this is only required because our example file is so small.
                Thread.sleep(2000)
            } catch (ignored: InterruptedException) {
            }

            downloadJob = DownloadJob.startDownload(
                DownloadRequest.Builder(getContext())
                    .uri(url)
                    .outputFile(File(getContext().getDir("documents", Context.MODE_PRIVATE), "temp.pdf"))
                    .overwriteExisting(true)
                    .build()
            )

            downloadJob.setProgressListener(object : DownloadJob.ProgressListener {
                override fun onProgress(progress: Progress) {
                    // Notify our listeners about the download progress.
                    progressSubject.onNext(progress.bytesReceived.toDouble() / progress.totalBytes.toDouble())
                }

                override fun onComplete(output: File) {
                    progressSubject.onComplete()
                    downloadLatch.countDown()
                }

                override fun onError(exception: Throwable) {
                    progressSubject.onError(exception)
                    downloadLatch.countDown()
                }
            })
        }

        this.downloadJob = downloadJob
        return downloadJob as DownloadJob
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(url)
    }

    private constructor(input: Parcel) : this(input.readString()!!)

    companion object CREATOR : Parcelable.Creator<RemoteDataProvider> {
        override fun createFromParcel(parcel: Parcel): RemoteDataProvider {
            return RemoteDataProvider(parcel)
        }

        override fun newArray(size: Int): Array<RemoteDataProvider?> {
            return arrayOfNulls(size)
        }
    }
}
