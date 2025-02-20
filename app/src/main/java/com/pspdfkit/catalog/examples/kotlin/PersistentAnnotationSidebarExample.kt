/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationProvider
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.kotlin.PersistentAnnotationSidebarActivity.Companion.EXTRA_CONFIGURATION
import com.pspdfkit.catalog.examples.kotlin.PersistentAnnotationSidebarActivity.Companion.EXTRA_URI
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.sharing.ShareFeatures
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.listeners.SimpleDocumentListener
import com.pspdfkit.ui.PdfUiFragment
import com.pspdfkit.ui.PdfUiFragmentBuilder
import com.pspdfkit.utils.getSupportParcelable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.util.EnumSet

class PersistentAnnotationSidebarExample(context: Context) : SdkExample(
    context.getString(R.string.annotationSidebarExampleTitle),
    context.getString(R.string.annotationSidebarExampleDescription)
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We don't need to show it in the outline since we will build our own UI for this.
        configuration.disableAnnotationList()

        // We also disable the rest of the outline to make a bit more space in the toolbar.
        configuration.disableOutline()
        configuration.disableBookmarkList()
        configuration.disableDocumentInfoView()

        // We also hide the share option to make a bit more space.
        configuration.setEnabledShareFeatures(EnumSet.noneOf(ShareFeatures::class.java))
        configuration.disablePrinting()

        configuration.useImmersiveMode(false)

        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            val intent = Intent(context, PersistentAnnotationSidebarActivity::class.java)
            intent.putExtra(EXTRA_URI, Uri.fromFile(documentFile))
            intent.putExtra(EXTRA_CONFIGURATION, configuration.build())
            context.startActivity(intent)
        }
    }
}

class PersistentAnnotationSidebarActivity : AppCompatActivity() {

    /** The adapter we use for our recycler view. */
    private val annotationRecyclerAdapter = AnnotationRecyclerAdapter(this)

    /** View that is shown in the side bar when no annotations are in the document. */
    private lateinit var noAnnotationsView: View

    /** The currently displayed PdfUiFragment. */
    private lateinit var pdfUiFragment: PdfUiFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // We need to load our layout first.
        setContentView(R.layout.activity_persistent_sidebar)
        noAnnotationsView = findViewById(R.id.noAnnotationsView)
        val recyclerView: RecyclerView = findViewById(R.id.annotationList)
        recyclerView.adapter = annotationRecyclerAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        annotationRecyclerAdapter.annotationRecyclerAdapterListener = object : AnnotationRecyclerAdapter.AnnotationRecyclerAdapterListener {
            override fun onAnnotationClicked(annotation: Annotation) {
                // When an annotation is clicked we scroll to the page containing it.
                // And also select the annotation for editing if possible.
                pdfUiFragment.setPageIndex(annotation.pageIndex, true)
                pdfUiFragment.pdfFragment?.setSelectedAnnotation(annotation)
            }

            override fun onAnnotationsLoaded(annotations: List<Annotation>) {
                // We want to show a short description of what's going on if there are no annotations.
                if (annotations.isEmpty()) {
                    noAnnotationsView.visibility = View.VISIBLE
                } else {
                    noAnnotationsView.visibility = View.GONE
                }
            }
        }

        // Finally we can setup our PDF fragment.
        obtainPdfFragment()
    }

    /** This adds or retrieves the [PdfUiFragment] we use to display the PDF. */
    private fun obtainPdfFragment() {
        // We either grab the existing fragment or add a new one.
        pdfUiFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? PdfUiFragment
            // There is no existing fragment, create a new one.
            ?: PdfUiFragmentBuilder.fromUri(this, intent.extras!!.getSupportParcelable(EXTRA_URI, Uri::class.java))
                .configuration(intent.extras!!.getSupportParcelable(EXTRA_CONFIGURATION, PdfActivityConfiguration::class.java))
                .build()
                .apply {
                    // After creation we actually add it to the fragment manager.
                    supportFragmentManager.beginTransaction().add(R.id.fragmentContainer, this, FRAGMENT_TAG).commit()
                }
    }

    override fun onStart() {
        super.onStart()
        // We need to be notified when the document was loaded.
        pdfUiFragment.pdfFragment?.addDocumentListener(object : SimpleDocumentListener() {
            override fun onDocumentLoaded(document: PdfDocument) {
                // When the document is loaded clear the previous annotations.
                annotationRecyclerAdapter.clear()

                // We need to set the current document so we can load the annotations.
                annotationRecyclerAdapter.currentDocument = document

                // We need to be aware of any change to the annotations so we can keep our list updated.
                document.annotationProvider.addOnAnnotationUpdatedListener(object : AnnotationProvider.OnAnnotationUpdatedListener {
                    override fun onAnnotationCreated(annotation: Annotation) {
                        annotationRecyclerAdapter.refreshAnnotationsForPage(annotation.pageIndex)
                    }

                    override fun onAnnotationUpdated(annotation: Annotation) {
                        annotationRecyclerAdapter.refreshAnnotationsForPage(annotation.pageIndex)
                    }

                    override fun onAnnotationRemoved(annotation: Annotation) {
                        annotationRecyclerAdapter.refreshAnnotationsForPage(annotation.pageIndex)
                    }

                    override fun onAnnotationZOrderChanged(
                        pageIndex: Int,
                        oldOrder: MutableList<Annotation>,
                        newOrder: MutableList<Annotation>
                    ) {
                        annotationRecyclerAdapter.refreshAnnotationsForPage(pageIndex)
                    }
                })

                // We also need to initialize the list of annotations to begin with.
                // This is a bit ineffective since we refresh the RecyclerView adapter for each page but this is fine for our small example.
                for (pageIndex in 0 until document.pageCount) {
                    annotationRecyclerAdapter.refreshAnnotationsForPage(pageIndex)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // This will cancel all running operations.
        annotationRecyclerAdapter.clear()
    }

    companion object {
        /** Tag we give to our PdfUiFragment. */
        const val FRAGMENT_TAG = "PersistentAnnotationSidebarActivity.Fragment"
        const val EXTRA_URI = "PersistentAnnotationSidebarActivity.DocumentUri"
        const val EXTRA_CONFIGURATION = "PersistentAnnotationSidebarActivity.PdfConfiguration"
    }
}

class AnnotationRecyclerAdapter(private val context: Context) : RecyclerView.Adapter<AnnotationRecyclerAdapterViewHolder>() {

    /** We keep a list of all annotations we display for easy access. */
    private val displayedItems = mutableListOf<Annotation>()

    /** We keep a list of annotations per page so we can update only single pages easily. */
    private val annotationsPerPage = mutableMapOf<Int, List<Annotation>>()

    /** It's good practice to keep track of running RxJava operations so they can be disposed of when exiting the activity. */
    private val loadingDisposables = mutableMapOf<Int, Disposable>()

    var currentDocument: PdfDocument? = null
    var annotationRecyclerAdapterListener: AnnotationRecyclerAdapterListener? = null

    // We only list certain annotation types.
    private val listedAnnotationTypes = EnumSet.allOf(AnnotationType::class.java).apply {
        // We don't want to clutter the list with widget or link annotations.
        remove(AnnotationType.WIDGET)
        remove(AnnotationType.LINK)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnotationRecyclerAdapterViewHolder {
        val root = LayoutInflater.from(context).inflate(R.layout.item_annotation, parent, false)
        return AnnotationRecyclerAdapterViewHolder(root)
    }

    override fun getItemCount(): Int = displayedItems.size

    override fun onBindViewHolder(holder: AnnotationRecyclerAdapterViewHolder, position: Int) {
        val item = displayedItems[position]
        // In the top text view we display whatever information we can get on the annotation.
        holder.titleView.text = item.contents ?: item.name ?: item.uuid

        // In the bottom text view we display the annotation type.
        holder.infoView.text = item.type.toString()
        holder.itemView.setOnClickListener {
            annotationRecyclerAdapterListener?.onAnnotationClicked(item)
        }
    }

    /** Removes all currently loaded annotations, and clears the state */
    fun clear() {
        displayedItems.clear()
        annotationsPerPage.clear()
        for (disposable in loadingDisposables.values) {
            disposable.dispose()
        }
        loadingDisposables.clear()
        currentDocument = null
    }

    /** Reloads the list of annotations for the given page. */
    fun refreshAnnotationsForPage(pageIndex: Int) {
        // If no document is set we don't to anything.
        val document = currentDocument ?: return

        // Cancel any already running loading operation for this page.
        loadingDisposables[pageIndex]?.dispose()

        // We grab the annotations for the current page index.
        // This operates on a background scheduler so we have to explicitly observe it on the main thread.
        loadingDisposables[pageIndex] = document.annotationProvider.getAllAnnotationsOfTypeAsync(listedAnnotationTypes, pageIndex, 1)
            .toList()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { annotations ->
                // Now that we have the annotations we need to store them.
                annotationsPerPage[pageIndex] = annotations
                // Afterwards we update our final list for displaying.
                refreshDisplayedItems()
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshDisplayedItems() {
        val document = currentDocument ?: return
        displayedItems.clear()
        for (pageIndex in 0 until document.pageCount) {
            // We add all pages we already loaded here.
            val items = annotationsPerPage[pageIndex]
            if (items != null) {
                displayedItems.addAll(items)
            }
        }

        // We notify the listener so we can update the visibility of our empty view.
        annotationRecyclerAdapterListener?.onAnnotationsLoaded(displayedItems)

        notifyDataSetChanged()
    }

    interface AnnotationRecyclerAdapterListener {
        fun onAnnotationClicked(annotation: Annotation)

        fun onAnnotationsLoaded(annotations: List<Annotation>)
    }
}

class AnnotationRecyclerAdapterViewHolder(root: View) : RecyclerView.ViewHolder(root) {
    val titleView: TextView = root.findViewById(R.id.annotation_list_item_title)
    val infoView: TextView = root.findViewById(R.id.annotation_list_item_info)
}
