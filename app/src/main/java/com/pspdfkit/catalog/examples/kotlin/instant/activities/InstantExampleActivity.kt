/*
 *   Copyright © 2020-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.instant.activities

import android.content.Intent
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.pspdfkit.ai.createAiAssistantForInstant
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.examples.kotlin.instant.api.InstantExampleDocumentDescriptor
import com.pspdfkit.catalog.examples.kotlin.instant.api.WebPreviewClient
import com.pspdfkit.catalog.utils.JwtGenerator
import com.pspdfkit.document.sharing.DocumentSharingIntentHelper
import com.pspdfkit.instant.client.InstantClient
import com.pspdfkit.instant.document.InstantPdfDocument
import com.pspdfkit.instant.exceptions.InstantErrorCode
import com.pspdfkit.instant.exceptions.InstantException
import com.pspdfkit.instant.listeners.InstantDocumentListener
import com.pspdfkit.instant.ui.InstantPdfActivity
import com.pspdfkit.instant.ui.InstantPdfActivityIntentBuilder
import com.pspdfkit.ui.actionmenu.ActionMenu
import com.pspdfkit.ui.actionmenu.ActionMenuItem
import com.pspdfkit.ui.actionmenu.ActionMenuListener
import com.pspdfkit.ui.actionmenu.FixedActionMenuItem
import com.pspdfkit.ui.actionmenu.SharingMenu
import com.pspdfkit.utils.getSupportParcelableExtra
import io.nutrient.domain.ai.AiAssistant
import io.nutrient.domain.ai.AiAssistantProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Extends [InstantPdfActivity] with sharing actions and a "reset & reopen" flow.
 *
 * The reset action clears local Instant storage and reopens the document to recover from
 * server-side document recreation (new annotation IDs) and similar sync conflicts.
 * This is meant to be used with the example server implementation:
 * https://github.com/PSPDFKit/pspdfkit-server-example-nodejs
 */
open class InstantExampleActivity :
    InstantPdfActivity(),
    ActionMenuListener,
    AiAssistantProvider {
    /** Descriptor for the displayed document.  */
    private lateinit var documentDescriptor: InstantExampleDocumentDescriptor
    private lateinit var aiAssistantInstance: AiAssistant

    /** Menu with collaborate sharing actions.  */
    private lateinit var collaborateMenu: SharingMenu

    /** Main toolbar icons color.  */
    private var mainToolbarIconsColor = 0

    // Used to fetch a fresh descriptor/JWT from the web preview server before reopening.
    private val webPreviewClient = WebPreviewClient()
    private var resetJob: Job? = null
    private var resetTriggeredForSyncError = false
    private var resetCacheDialog: AlertDialog? = null
    private var progressDialog: AlertDialog? = null

    private val instantErrorListener =
        object : InstantDocumentListener {
            override fun onSyncError(instantDocument: InstantPdfDocument, error: InstantException) {
                if (error.errorCode == InstantErrorCode.INVALID_REQUEST && !resetTriggeredForSyncError) {
                    resetTriggeredForSyncError = true
                    showResetCacheDialog()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val documentDescriptor: InstantExampleDocumentDescriptor? =
            intent.getSupportParcelableExtra(
                InstantExampleExtras.DOCUMENT_DESCRIPTOR,
                InstantExampleDocumentDescriptor::class.java,
            )
        checkNotNull(documentDescriptor) {
            "InstantExampleActivity was not initialized with proper arguments: Missing document descriptor extra!"
        }
        this.documentDescriptor = documentDescriptor

        val a =
            theme.obtainStyledAttributes(
                null,
                com.pspdfkit.R.styleable.pspdf__ActionBarIcons,
                com.pspdfkit.R.attr.pspdf__actionBarIconsStyle,
                com.pspdfkit.R.style.PSPDFKit_ActionBarIcons,
            )
        mainToolbarIconsColor =
            a.getColor(
                com.pspdfkit.R.styleable.pspdf__ActionBarIcons_pspdf__iconsColor,
                ContextCompat.getColor(this, com.pspdfkit.R.color.pspdf__onPrimary),
            )
        a.recycle()
        // only initialise if AI Assistant is enabled
        if (configuration.configuration.isAiAssistantEnabled) aiAssistantInstance = createAiAssistantInstance()
        initCollaborateMenu()
        // Hook into Instant sync errors so we can trigger recovery in this example.
        getPdfFragment().addInstantDocumentListener(instantErrorListener)
    }

    override fun onStop() {
        super.onStop()
        resetJob?.cancel()
        resetJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        getPdfFragment().removeInstantDocumentListener(instantErrorListener)
    }

    private fun initCollaborateMenu() {
        collaborateMenu = SharingMenu(this, null)
        collaborateMenu.setTitle(getString(R.string.instant_collaborate))

        collaborateMenu.addMenuItem(
            FixedActionMenuItem(this, R.id.open_in_browser, R.drawable.ic_open_in_browser, R.string.instant_open_in_browser),
        )
        collaborateMenu.addMenuItem(
            FixedActionMenuItem(
                this,
                R.id.share_document_link,
                com.pspdfkit.R.drawable.pspdf__ic_open_in,
                R.string.instant_share_document_link,
            ),
        )
        collaborateMenu.addMenuItem(
            FixedActionMenuItem(
                this,
                R.id.instant_reset_cache,
                com.pspdfkit.R.drawable.pspdf__ic_status_clear,
                R.string.instant_reset_cache,
            ),
        )

        collaborateMenu.addActionMenuListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        val collaborateMenuItem = menu.add(0, R.id.instant_collaborate, 0, getString(R.string.instant_collaborate))
        collaborateMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        val collaborateMenuItem = menu.findItem(R.id.instant_collaborate)
        collaborateMenuItem.isEnabled = document != null

        collaborateMenuItem.icon =
            ContextCompat.getDrawable(this, R.drawable.ic_collaborate)?.apply {
                DrawableCompat.setTint(this, mainToolbarIconsColor)
                alpha = if (document != null) 255 else 128
            }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.instant_collaborate) {
            collaborateMenu.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareActionMenu(actionMenu: ActionMenu): Boolean = true

    override fun onDisplayActionMenu(actionMenu: ActionMenu) {}

    override fun onRemoveActionMenu(actionMenu: ActionMenu) {}

    override fun onActionMenuItemLongClicked(actionMenu: ActionMenu, menuItem: ActionMenuItem): Boolean = false

    override fun onActionMenuItemClicked(actionMenu: ActionMenu, menuItem: ActionMenuItem): Boolean {
        if (menuItem.itemId == R.id.share_document_link) {
            // Sharing link to the Instant document.
            showShareTextMenu(R.string.instant_share_document_link, documentDescriptor.webUrl)
            return true
        } else if (menuItem.itemId == R.id.open_in_browser) {
            // Opens Instant document link in the web browser.
            showOpenInBrowserMenu()
            return true
        } else if (menuItem.itemId == R.id.instant_reset_cache) {
            showResetCacheDialog()
            return true
        }
        return false
    }

    private fun showResetCacheDialog() {
        resetCacheDialog?.dismiss()
        collaborateMenu.dismiss()
        resetCacheDialog =
            AlertDialog
                .Builder(this)
                .setTitle(R.string.instant_reset_cache_title)
                .setMessage(R.string.instant_reset_cache_message)
                .setPositiveButton(R.string.instant_reset_cache_action) { _, _ ->
                    resetCacheAndReopen()
                }.setNegativeButton(android.R.string.cancel) { _, _ ->
                    resetTriggeredForSyncError = false
                }.setOnDismissListener {
                    resetCacheDialog = null
                    // Reset flag if dismissed without starting reset operation
                    if (resetJob == null && !isFinishing) {
                        resetTriggeredForSyncError = false
                    }
                }.show()
    }

    /**
     * Clears local Instant storage and relaunches the activity with a fresh descriptor/JWT.
     *
     * This is intended to recover from server-side document recreation where annotation IDs
     * no longer match the cached local state.
     */
    private fun resetCacheAndReopen() {
        // Best-effort: refresh the descriptor/JWT, wipe local storage, and relaunch the activity.
        val instantDocument = document
        val serverUrl = instantDocument?.instantClient?.serverUrl ?: documentDescriptor.serverUrl
        val documentId = instantDocument?.instantDocumentDescriptor?.documentId
        resetJob?.cancel()
        showProgressDialog()
        resetJob =
            lifecycleScope.launch {
                try {
                    val newDescriptor =
                        withContext(Dispatchers.IO) {
                            webPreviewClient.getDocument(documentDescriptor.webUrl)
                        }
                    withContext(Dispatchers.IO) {
                        val client = InstantClient.create(this@InstantExampleActivity, serverUrl)
                        if (documentId != null) {
                            client.removeLocalStorageForDocument(documentId)
                        } else {
                            client.removeLocalStorage()
                        }
                    }
                    dismissProgressDialog()
                    Toast.makeText(this@InstantExampleActivity, R.string.toast_cache_cleared, Toast.LENGTH_SHORT).show()
                    val intent =
                        InstantPdfActivityIntentBuilder
                            .fromInstantDocument(
                                this@InstantExampleActivity,
                                newDescriptor.serverUrl,
                                newDescriptor.jwt,
                            ).configuration(configuration)
                            .activityClass(InstantExampleActivity::class.java)
                            .build()
                    intent.putExtra(InstantExampleExtras.DOCUMENT_DESCRIPTOR, newDescriptor)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    // Don't catch Error subclasses
                    dismissProgressDialog()
                    Log.e("InstantExample", "Failed to reset cache and reopen", e)
                    Toast
                        .makeText(
                            this@InstantExampleActivity,
                            R.string.instant_error_something_went_wrong,
                            Toast.LENGTH_LONG,
                        ).show()
                    resetTriggeredForSyncError = false // Allow retry
                }
            }
    }

    private fun showProgressDialog() {
        progressDialog?.dismiss()
        progressDialog =
            AlertDialog
                .Builder(this)
                .setView(
                    android.widget.ProgressBar(this).apply {
                        isIndeterminate = true
                        setPadding(0, 48, 0, 48)
                    },
                ).setMessage(R.string.instant_resetting_cache)
                .setCancelable(false)
                .show()
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showOpenInBrowserMenu() {
        val shareIntent = Intent(Intent.ACTION_VIEW, documentDescriptor.webUrl.toUri())
        val sharingMenu =
            SharingMenu(
                this,
            ) { shareTarget ->
                shareIntent.setPackage(shareTarget.packageName)
                startActivity(shareIntent)
            }
        sharingMenu.setTitle(R.string.instant_open_in_browser)
        sharingMenu.setShareIntents(listOf(shareIntent))

        collaborateMenu.dismiss()
        sharingMenu.show()
    }

    private fun showShareTextMenu(@StringRes titleRes: Int, textToShare: String) {
        val shareIntent = DocumentSharingIntentHelper.getShareTextIntent(textToShare)
        val sharingMenu =
            SharingMenu(
                this,
            ) { shareTarget ->
                shareIntent.setPackage(shareTarget.packageName)
                startActivity(shareIntent)
            }
        sharingMenu.setTitle(titleRes)
        sharingMenu.setShareIntents(listOf(shareIntent))

        collaborateMenu.dismiss()
        sharingMenu.show()
    }

    val sessionId = "my-session-id"

    override fun getAiAssistant(): AiAssistant = aiAssistantInstance

    fun createAiAssistantInstance() = createAiAssistantForInstant(
        this,
        documentDescriptor.serverUrl,
        listOf(documentDescriptor.jwt),
        "http://192.168.1.6:4000",
        sessionId,
    ) { instantDocumentIds ->
        JwtGenerator.generateJwtToken(
            this@InstantExampleActivity,
            claims =
            mapOf(
                "document_ids" to instantDocumentIds,
                "session_ids" to listOf(sessionId),
                "request_limit" to
                    mapOf(
                        "requests" to 160,
                        "time_period_s" to 1000 * 60 * 10,
                    ),
            ),
        )
    }

    override fun navigateTo(documentRect: List<RectF>, pageIndex: Int, documentIndex: Int) {
        getPdfFragment().apply {
            setPageIndex(pageIndex)
            highlight(this@InstantExampleActivity, documentRect, pageIndex)
        }
    }
}
