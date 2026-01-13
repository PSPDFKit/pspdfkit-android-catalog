/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

@file:Suppress("DEPRECATION")

package com.pspdfkit.catalog.examples.kotlin.instant.activities

import android.graphics.RectF
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.pspdfkit.ai.createAiAssistantForInstant
import com.pspdfkit.catalog.examples.kotlin.instant.api.InstantExampleDocumentDescriptor
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.catalog.utils.JwtGenerator
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.jetpack.compose.interactors.DocumentState
import com.pspdfkit.jetpack.compose.interactors.getDefaultInstantDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberInstantDocumentState
import com.pspdfkit.jetpack.compose.views.InstantDocumentView
import io.nutrient.domain.ai.AiAssistant
import io.nutrient.domain.ai.AiAssistantProvider

/**
 * An example activity that demonstrates how to use the InstantDocumentView composable to display an Instant document.
 */

class InstantComposeExampleActivity : AppCompatActivity(), AiAssistantProvider {
    private lateinit var documentState: DocumentState
    private lateinit var aiAssistantInstance: AiAssistant

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Extract the PSPDFKit bundle holding all internal extras.
        val extra = intent.extras ?: throw IllegalArgumentException("Intent extras are required")
        val instantDocumentSource: InstantExampleDocumentDescriptor = extra.getParcelable(DOCUMENT_DESCRIPTOR)
            ?: throw IllegalArgumentException("InstantExampleDocumentDescriptor is required")
        val configuration: PdfActivityConfiguration = extra.getParcelable(CONFIGURATION) ?: PdfActivityConfiguration.Builder(this).build()
        // only initialise if AI Assistant is enabled
        if (configuration.configuration.isAiAssistantEnabled) aiAssistantInstance = createAiAssistantInstance(instantDocumentSource)
        setContent {
            CatalogTheme {
                documentState = rememberInstantDocumentState(
                    serverUrl = instantDocumentSource.serverUrl,
                    jwt = instantDocumentSource.jwt,
                    configuration = configuration
                )
                @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
                Scaffold(
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.onPrimary)
                        .navigationBarsPadding(),
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                documentState.documentConnection.save()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "save")
                        }
                    }
                ) {
                    InstantDocumentView(
                        documentState = documentState,
                        modifier = Modifier.fillMaxSize(),
                        instantDocumentManager = getDefaultInstantDocumentManager()
                    )
                }
            }
        }
    }
    val sessionId = "my-session-id"

    override fun getAiAssistant(): AiAssistant = aiAssistantInstance

    fun createAiAssistantInstance(documentDescriptor: InstantExampleDocumentDescriptor) = createAiAssistantForInstant(
        this,
        documentDescriptor.serverUrl,
        listOf(documentDescriptor.jwt),
        "http://192.168.1.6:4000",
        sessionId
    ) { instantDocumentIds ->
        JwtGenerator.generateJwtToken(
            this@InstantComposeExampleActivity,
            claims = mapOf(
                "document_ids" to instantDocumentIds,
                "session_ids" to listOf(sessionId),
                "request_limit" to mapOf(
                    "requests" to 160,
                    "time_period_s" to 1000 * 60 * 10
                )
            )
        )
    }

    override fun navigateTo(documentRect: List<RectF>, pageIndex: Int, documentIndex: Int) {
        documentState.documentConnection.apply {
            setPageIndex(pageIndex)
            highlight(pageIndex, documentRect)
        }
    }

    companion object {
        const val DOCUMENT_DESCRIPTOR = "InstantComposeExampleActivity.DOCUMENT_DESCRIPTOR"
        const val CONFIGURATION = "InstantComposeExampleActivity.CONFIGURATION"
    }
}
