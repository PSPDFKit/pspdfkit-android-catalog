/*
 *   Copyright © 2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.providers.UrlDataProvider
import com.pspdfkit.ui.PdfActivityIntentBuilder
import java.io.File
import java.net.URL

/**
 * This example illustrates how to make use of the [UrlDataProvider] to automatically download documents
 * from the internet and open them in PSPDFKit.
 */

class RemoteUrlExample(val context: Context) : SdkExample(context, R.string.remoteUrlDataProviderExampleTitle, R.string.remoteUrlDataProviderExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val intent = Intent(context, WebViewActivity::class.java)
        // make the catalogs default pdf configuration available to the webview activity
        intent.putExtra(WebViewActivity.EXTRA_CONFIG, configuration.build())
        context.startActivity(intent)
    }
}

class WebViewActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_CONFIG = "_config"
    }

    private var configuration: PdfActivityConfiguration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        extractConfiguration(savedInstanceState ?: intent.extras)

        setContent {
            // display a webview with a test page from our assets folder
            Webview(
                "file:///android_asset/url-dataprovider-example/testpage.html",
                object : WebViewClient() {

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        request?.url?.let { uri ->
                            // If a usable link was clicked, wrap the URL with a UrlDataProvider and open the document.
                            val scheme = uri.scheme
                            val uriString = uri.toString()
                            val uriFile = File(uriString)
                            val ext = uriFile.extension
                            when {
                                // we're only interested in http(s) links
                                !listOf("http", "https").contains(scheme) -> {
                                    null
                                }
                                ext.equals("pdf", ignoreCase = true) -> {
                                    // Using the URLDataProvider gives us more control over the download.
                                    // We can specify a target file to download the document to.
                                    // This avoids downloading the image everytime the URL is opened.
                                    PdfActivityIntentBuilder.fromDataProvider(
                                        this@WebViewActivity,
                                        UrlDataProvider(URL(uriString), File(filesDir, "myDocumentDownloads/${uriFile.name}"))
                                    )
                                }

                                listOf("jpg", "jpeg", "png").contains(ext) -> {
                                    // If we just pass along the URL, the nutrient library will recognize http/https scheme
                                    // and automatically download the file to the cache folder.
                                    // It will be downloaded everytime the URL is opened.
                                    PdfActivityIntentBuilder.fromImageUri(this@WebViewActivity, uri)
                                }

                                else -> null
                            }?.let { intentBuilder ->
                                configuration?.let {
                                    intentBuilder.configuration(it)
                                }

                                startActivity(intentBuilder.build())
                                return true
                            }
                        }
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                }
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_CONFIG, configuration)
    }

    private fun extractConfiguration(bundle: Bundle?) {
        if (bundle == null) return
        configuration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(EXTRA_CONFIG, PdfActivityConfiguration::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(EXTRA_CONFIG)
        }
    }

    /**
     * A composable function that wraps a WebView.
     */
    @Composable
    fun Webview(url: String, client: WebViewClient? = null) {
        AndroidView(
            factory = {
                WebView(it).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    if (client != null) {
                        webViewClient = client
                    }
                }
            },
            update = {
                it.loadUrl(url)
            }
        )
    }
}
