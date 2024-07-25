/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.LruCache
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.utils.Size
import com.pspdfkit.utils.getSupportParcelableExtra
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import io.reactivex.rxjava3.core.FlowableOnSubscribe
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.util.ArrayDeque
import java.util.Locale

/**
 * This example lists all documents found in the assets and presents them with their previews in a grid.
 */
class KioskExample(context: Context) : SdkExample(context, R.string.kioskExampleTitle, R.string.kioskExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val intent = Intent(context, KioskActivity::class.java)
        // Pass the configuration to our activity.
        intent.putExtra(KioskActivity.CONFIGURATION_ARG, configuration.build())
        context.startActivity(intent)
    }
}

/**
 * This activity displays all documents found in the assets folder of the app.
 */
class KioskActivity : AppCompatActivity() {

    private lateinit var configuration: PdfActivityConfiguration
    private var listAssetsDisposable: Disposable? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_kiosk)

        configuration = intent.getSupportParcelableExtra(CONFIGURATION_ARG, PdfActivityConfiguration::class.java)
            ?: throw NullPointerException("Extras bundle was missing configuration")

        val documentGrid = findViewById<GridView>(android.R.id.list)
        val documentAdapter: DocumentAdapter = DocumentAdapter(this)
        documentGrid.adapter = documentAdapter
        documentGrid.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            val dataProvider = documentAdapter.getItem(position)?.documentSource?.dataProvider
                ?: return@OnItemClickListener

            // Open the touched document.
            val intent = PdfActivityIntentBuilder.fromDataProvider(this@KioskActivity, dataProvider)
                .configuration(configuration)
                .build()
            startActivity(intent)
        }

        val progressBar = findViewById<ProgressBar>(android.R.id.progress)
        // Load the documents on a background thread.
        listAssetsDisposable = listAllAssets()
            // List assets on the background (I/O) thread.
            .subscribeOn(Schedulers.io())
            // Filter PDF files only.
            .filter { it.lowercase(Locale.getDefault()).endsWith(".pdf") }
            // The second observe on is necessary so opening the documents runs on a different thread as listing the assets.
            .observeOn(Schedulers.io())
            .flatMap { asset ->
                // Open the document with multithreaded rendering disabled (last parameter set to `false`).
                // This improves performance for single page (cover) rendering in most cases.
                PdfDocumentLoader.openDocumentAsync(this@KioskActivity, DocumentSource(AssetDataProvider(asset)), false)
                    .toFlowable()
                    .doOnError { throwable ->
                        // This example catches any error that happens while opening the document (e.g. if a password would be needed).
                        // If an exception is thrown, the document will not be shown.
                        Log.w(TAG, String.format("Could not open document '%s' from assets. See exception for reason.", asset), throwable)
                    }
                    .onErrorResumeNext { Flowable.empty() }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete { progressBar.visibility = View.GONE }
            .toSortedList { document: PdfDocument, document2: PdfDocument ->
                val title = document.title
                val title2 = document2.title
                return@toSortedList when {
                    document === document2 -> 0
                    title == null -> -1
                    title2 == null -> 1
                    else -> title.compareTo(title2, ignoreCase = true)
                }
            }
            .subscribe({ collection: List<PdfDocument> -> documentAdapter.addAll(collection) }) { throwable ->
                progressBar.visibility = View.GONE
                Log.e(TAG, "Error while trying to list all catalog app assets.", throwable)
                Toast.makeText(this@KioskActivity, "Error listing asset files - see logcat for detailed error message.", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()

        listAssetsDisposable?.dispose()
        listAssetsDisposable = null
    }

    /**
     * Lists all assets in the assets directory.
     *
     * @return A observable sending all file paths in the assets folder.
     */
    private fun listAllAssets(): Flowable<String> {
        return Flowable.create<String>(
            object : FlowableOnSubscribe<String> {
                override fun subscribe(emitter: FlowableEmitter<String>) {
                    try {
                        val pathsToCheck = ArrayDeque<String>()
                        pathsToCheck.addAll(assets.list("") ?: arrayOf())
                        while (!pathsToCheck.isEmpty()) {
                            val currentPath = pathsToCheck.poll() ?: break
                            val children = getChildren(currentPath)
                            if (children.isNullOrEmpty()) {
                                // This is just a file, tell our subscriber about it.
                                emitter.onNext(currentPath)
                            } else {
                                // Check all other sub paths.
                                for (child in children) {
                                    pathsToCheck.add(currentPath + File.separator + child)
                                }
                            }
                        }
                        emitter.onComplete()
                    } catch (e: IOException) {
                        emitter.onError(e)
                    }
                }

                @Throws(IOException::class)
                private fun getChildren(path: String): Array<String> {
                    // Since listing assets is really really slow we assume everything with a '.' in it is a file.
                    return if (path.contains(".")) {
                        arrayOf()
                    } else {
                        assets.list(path) ?: arrayOf()
                    }
                }
            },
            BackpressureStrategy.BUFFER
        )
    }

    private class ViewHolder(val view: View) {
        val itemPreviewImageView: ImageView = view.findViewById(R.id.itemPreviewImageView)
        val itemTitleView: TextView = view.findViewById(R.id.itemTileView)

        var previewRenderDisposable: Disposable? = null

        companion object {
            operator fun get(convertView: View?, parent: ViewGroup): ViewHolder {
                var view = convertView
                val holder: ViewHolder
                if (view != null) {
                    holder = view.tag as ViewHolder
                } else {
                    view = LayoutInflater.from(parent.context).inflate(R.layout.item_kiosk_item, parent, false)
                    holder = ViewHolder(view)
                    view.tag = holder
                }
                return holder
            }
        }
    }

    private inner class DocumentAdapter(context: Context) : ArrayAdapter<PdfDocument>(context, View.NO_ID) {
        private val previewImageCache: LruCache<String, Bitmap>
        private val previewImageSize: Size

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val holder: ViewHolder = ViewHolder[convertView, parent]
            holder.previewRenderDisposable?.dispose()

            val document = getItem(position) ?: throw IllegalStateException("Invalid position!")

            // We only want to render a new preview image if we don't already have one in the cache.
            val cachedPreview = previewImageCache[document.uid]
            holder.itemPreviewImageView.setImageBitmap(cachedPreview)
            if (cachedPreview == null) {
                // Calculate the size of the rendered preview image.
                val size = calculateBitmapSize(document, previewImageSize)

                // Render page to bitmap.
                holder.previewRenderDisposable = document.renderPageToBitmapAsync(
                    parent.context,
                    0,
                    size.width.toInt(),
                    size.height.toInt()
                ).observeOn(AndroidSchedulers.mainThread())
                    .subscribe { bitmap ->
                        holder.itemPreviewImageView.setImageBitmap(bitmap)
                        previewImageCache.put(document.uid, bitmap)
                    }
            }

            if (!TextUtils.isEmpty(document.title)) {
                holder.itemTitleView.text = document.title
            } else {
                holder.itemTitleView.text = resources.getText(com.pspdfkit.R.string.pspdf__activity_title_unnamed_document)
            }

            return holder.view
        }

        private fun calculateBitmapSize(document: PdfDocument, availableSpace: Size): Size {
            val pageSize = document.getPageSize(0)
            val ratio: Float
            ratio = if (pageSize.width > pageSize.height) {
                availableSpace.width / pageSize.width
            } else {
                availableSpace.height / pageSize.height
            }
            return Size(pageSize.width * ratio, pageSize.height * ratio)
        }

        init {
            previewImageCache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
                override fun sizeOf(key: String, value: Bitmap): Int {
                    // The cache size will be measured in kilobytes rather than number of items.
                    return value.byteCount / 1024
                }
            }
            previewImageSize = Size(
                context.resources.getDimensionPixelSize(R.dimen.kiosk_previewimage_width).toFloat(),
                context.resources.getDimensionPixelSize(R.dimen.kiosk_previewimage_height).toFloat()
            )
        }
    }

    companion object {
        const val CONFIGURATION_ARG = "configuration"
        private const val TAG = "Kiosk"
    }
}
