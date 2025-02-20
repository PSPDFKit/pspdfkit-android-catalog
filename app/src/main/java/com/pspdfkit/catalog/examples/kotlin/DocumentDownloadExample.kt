/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.download.DownloadJob
import com.pspdfkit.document.download.DownloadProgressFragment
import com.pspdfkit.document.download.DownloadRequest
import com.pspdfkit.document.download.source.DownloadSource
import com.pspdfkit.ui.PdfActivityIntentBuilder
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection

/**
 * This is an example showing how to use the [DownloadJob] to download a PDF document from the web.
 */
class DocumentDownloadExample(context: Context) : SdkExample(context, R.string.documentDownloadExampleTitle, R.string.documentDownloadExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // The web download source is a custom DownloadSource implemented below.
        val source: WebDownloadSource = try {
            // Try to parse the URL pointing to the PDF document. If an error occurs, log it and leave the example.
            WebDownloadSource(URL("https://nutrient.io/downloads/case-study-box.pdf"))
        } catch (e: MalformedURLException) {
            Log.e(LOG_TAG, "Error while trying to parse the PDF Download URL.", e)
            return
        }

        // Build a download request based on various input parameters. Provide the web source pointing to the document.
        val request = DownloadRequest.Builder(context)
            .source(source)
            .outputFile(File(context.getDir("documents", Context.MODE_PRIVATE), "case-study-box.pdf"))
            .overwriteExisting(true)
            .build()

        // This will initiate the download.
        val job = DownloadJob.startDownload(request)
        job.setProgressListener(object : DownloadJob.ProgressListenerAdapter() {
            override fun onComplete(output: File) {
                val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(output))
                    .configuration(configuration.build())
                    .build()
                context.startActivity(intent)
            }

            override fun onError(exception: Throwable) {
                AlertDialog.Builder(context)
                    .setMessage("There was an error downloading the example PDF file. For further information see Logcat.")
                    .show()
            }
        })

        val fragment = DownloadProgressFragment()
        fragment.show((context as FragmentActivity).supportFragmentManager, "download-fragment")
        fragment.job = job
    }
}

/**
 * This download source can be used to download a PDF document from the web.
 */
private class WebDownloadSource constructor(private val documentURL: URL) : DownloadSource {
    /**
     * The open method needs to return an [InputStream] that will provide the complete document.
     */
    @Throws(IOException::class)
    override fun open(): InputStream {
        val connection = documentURL.openConnection() as HttpURLConnection
        connection.connect()
        return connection.inputStream
    }

    /**
     * If the length is available it can be returned here. This is optional, and can improve the reported download progress, since it will then contain
     * a percentage of download.
     */
    override fun getLength(): Long {
        var length = DownloadSource.UNKNOWN_DOWNLOAD_SIZE

        // We try to estimate the download size using the content length header.
        var urlConnection: URLConnection? = null
        try {
            urlConnection = documentURL.openConnection()
            val contentLength = urlConnection.contentLength
            if (contentLength != -1) {
                length = contentLength.toLong()
            }
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Error while trying to parse the PDF Download URL.", e)
        } finally {
            (urlConnection as? HttpURLConnection)?.disconnect()
        }
        return length
    }

    override fun toString(): String {
        return "WebDownloadSource{documentURL=$documentURL}"
    }
}

private const val LOG_TAG = "DocumentDownloadExample"
