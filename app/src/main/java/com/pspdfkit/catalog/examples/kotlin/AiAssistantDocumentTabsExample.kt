/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import JwtGenerator
import android.content.Context
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.SdkExample.Companion.WELCOME_DOC
import com.pspdfkit.catalog.examples.kotlin.AiAssistantComposeActivity.Companion.PREFERENCES_NAME
import com.pspdfkit.catalog.examples.kotlin.AiAssistantComposeActivity.Companion.PREF_AI_IP_ADDRESS
import com.pspdfkit.catalog.tasks.ExtractAssetTask.extractAsync
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.nutrient.domain.ai.AiAssistant
import io.nutrient.domain.ai.AiAssistantProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Shows how to implement AI Assistant chat supporting multiple tabbed documents analysis and interaction.
 */
class AiAssistantDocumentTabsExample(context: Context) : SdkExample(context, R.string.aiAssistantTabbedDocumentExampleTitle, R.string.aiAssistantTabbedDocumentExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        configuration.setAiAssistantEnabled(true)

        Observable.fromIterable(AiAssistantDocumentTabsActivity.assetFiles)
            .flatMapSingle { assetName -> extractAsync(assetName, assetName, context, false, null) }
            .map { file ->
                DocumentDescriptor.fromUri(Uri.fromFile(file))
            }
            .toList()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { documentDescriptors ->
                launchExampleActivity(context, documentDescriptors, configuration)
            }.subscribe()
    }

    private fun launchExampleActivity(context: Context, documentDescriptors: List<DocumentDescriptor>, configuration: PdfActivityConfiguration.Builder) {
        val intentBuilder = if (documentDescriptors.isEmpty()) {
            PdfActivityIntentBuilder.emptyActivity(context)
        } else {
            PdfActivityIntentBuilder.fromDocumentDescriptor(context, *documentDescriptors.toTypedArray())
        }
        intentBuilder
            .configuration(configuration.build())
            .activityClass(AiAssistantDocumentTabsActivity::class.java)

        context.startActivity(intentBuilder.build())
    }
}

class AiAssistantDocumentTabsActivity : PdfActivity(), AiAssistantProvider {

    var ipAddressValue = ""
    private val sessionId = AiAssistantDocumentTabsActivity::class.java.simpleName

    companion object {
        val assetFiles = listOf("Classbook.pdf", WELCOME_DOC, "Aviation.pdf", "Annotations.pdf")
    }

    var assistant: AiAssistant? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        ipAddressValue = preferences.getString(PREF_AI_IP_ADDRESS, "") ?: ""
        assistant = getAiAssistant()
        CoroutineScope(Dispatchers.IO).launch {
            // ingesting the documents as soon as the activity is created
            assistant?.initialize(false)
        }
    }

    fun getAiAssistantInstance() = com.pspdfkit.ai.createAiAssistant(
        context = this,
        documentsDescriptors = documentCoordinator.documents,
        serverUrl = "http://$ipAddressValue:4000",
        sessionId = sessionId,
        jwtToken = { documentIds ->
            JwtGenerator.generateJwtToken(
                this@AiAssistantDocumentTabsActivity,
                claims = mapOf(
                    "document_ids" to documentIds,
                    "session_ids" to listOf(sessionId),
                    "request_limit" to mapOf(
                        "requests" to 160,
                        "time_period_s" to 1000 * 60 * 10
                    )
                )
            )
        }
    )

    override fun getAiAssistant(): AiAssistant {
        return assistant ?: getAiAssistantInstance().also {
            assistant = it
        }
    }

    override fun navigateTo(
        documentRect: List<RectF>,
        pageIndex: Int,
        documentIndex: Int
    ) {
        val descriptor = documentCoordinator.getDocuments()[documentIndex]
        documentCoordinator.setVisibleDocument(descriptor)
        pdfFragment?.highlight(this@AiAssistantDocumentTabsActivity, documentRect, pageIndex)
    }
}
