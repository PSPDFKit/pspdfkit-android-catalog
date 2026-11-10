/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.compose.toolbar.state.ToolbarIcon
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.compose.toolbar.state.ToolbarItem.ItemEdge
import com.pspdfkit.compose.toolbar.state.ToolbarItem.OverflowBehavior
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.signatures.Signature
import com.pspdfkit.signatures.listeners.OnSignaturePickedListener
import com.pspdfkit.signatures.storage.DatabaseSignatureStorage
import com.pspdfkit.signatures.storage.SignatureStorage
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.signatures.ElectronicSignatureFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val INITIAL_PAGES = 9_101

// Size of the initials box, and its distance from the page corner, in PDF points.
private const val INITIALS_WIDTH = 72f
private const val INITIALS_HEIGHT = 36f
private const val INITIALS_MARGIN = 24f

/**
 * Shows how initialling a contract ("Paraphieren") can be built from the electronic signature APIs.
 *
 * A toolbar button asks for the user's initials once with the built-in signature dialog, then
 * places them in the bottom-right corner of every page, or of every page but the last, which is
 * where a contract is usually signed in full. Initials are kept in their own signature storage, so
 * they are offered again next time without mixing with full signatures.
 */
class InitialPagesExample(context: Context) :
    SdkExample(context, R.string.initialPagesExampleTitle, R.string.initialPagesExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile: File ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(InitialPagesActivity::class.java)
                .build()
            context.startActivity(intent)
        }
    }
}

class InitialPagesActivity :
    PdfActivity(),
    OnSignaturePickedListener {

    private val initialsStorage: SignatureStorage by lazy { DatabaseSignatureStorage.withName(this, "initials") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setToolbarItemsProvider { defaultItems ->
            defaultItems +
                ToolbarItem.Button(
                    id = INITIAL_PAGES,
                    actionId = "initial_pages",
                    contentDescription = getString(R.string.initialPagesAction),
                    icon = ToolbarIcon.Resource(com.pspdfkit.R.drawable.pspdf__ic_signature),
                    position = ItemEdge.End,
                    overflowBehavior = OverflowBehavior.Never,
                )
        }
        setOnToolbarItemClickListener { id ->
            if (id == INITIAL_PAGES) {
                ElectronicSignatureFragment.show(supportFragmentManager, this, null, initialsStorage)
                true
            } else {
                false
            }
        }

        // Reattach after a configuration change while the dialog is showing.
        ElectronicSignatureFragment.restore(supportFragmentManager, this, initialsStorage)
    }

    override fun onSignaturePicked(signature: Signature) {
        val pageCount = document?.pageCount ?: return
        val choices = arrayOf(
            getString(R.string.initialPagesAllPages, pageCount),
            getString(R.string.initialPagesAllButLast, pageCount - 1),
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.initialPagesWhichPages)
            .setItems(choices) { _, which ->
                val pages = if (which == 0) 0 until pageCount else 0 until pageCount - 1
                initialPages(signature, pages)
            }
            .show()
    }

    override fun onDismiss() = Unit

    private fun initialPages(signature: Signature, pages: IntRange) {
        val document = document ?: return
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                for (pageIndex in pages) {
                    val pageSize = document.getPageSize(pageIndex)
                    // PDF coordinates: the origin is bottom-left, so `top` is the larger y value.
                    val corner = RectF(
                        pageSize.width - INITIALS_MARGIN - INITIALS_WIDTH,
                        INITIALS_MARGIN + INITIALS_HEIGHT,
                        pageSize.width - INITIALS_MARGIN,
                        INITIALS_MARGIN,
                    )
                    document.annotationProvider.addAnnotationToPage(signature.toAnnotation(document, pageIndex, corner))
                }
            }
            Toast.makeText(
                this@InitialPagesActivity,
                resources.getQuantityString(R.plurals.initialPagesDone, pages.count(), pages.count()),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}
