/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pspdfkit.ai.createAiAssistant
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.SdkExample.Companion.WELCOME_DOC
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.jetpack.compose.interactors.DefaultListeners
import com.pspdfkit.jetpack.compose.interactors.DocumentState
import com.pspdfkit.jetpack.compose.interactors.getDefaultDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfActivity
import io.nutrient.domain.ai.AiAssistant
import io.nutrient.domain.ai.AiAssistantProvider

/**
 * Shows how to implement AI Assistant for the DocumentView in a Compose way.
 */
class AiAssistantComposeExample(context: Context) : SdkExample(
    context,
    R.string.jetpackAiAssistantExampleTitle,
    R.string.jetpackAiAssistantExampleDescription
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val intent = Intent(context, AiAssistantComposeActivity::class.java)
        context.startActivity(intent)
    }
}

class AiAssistantComposeActivity : AppCompatActivity(), AiAssistantProvider {
    private lateinit var documentState: DocumentState
    private val sessionId = AiAssistantComposeActivity::class.java.simpleName
    private val assetProvider = AssetDataProvider(WELCOME_DOC)
    private var ipAddressValue: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        ipAddressValue = preferences.getString(PREF_AI_IP_ADDRESS, "") ?: ""

        setContent {
            var enabled by remember { mutableStateOf(false) }
            var toolbarVisibility by remember { mutableStateOf(true) }

            CatalogTheme {
                val activityConfiguration = PdfActivityConfiguration.Builder(LocalContext.current)
                    .setAiAssistantEnabled(true)
                    .defaultToolbarEnabled(false)
                    .theme(R.style.PSPDFCatalog_AIAssistantDialog)
                    .themeDark(R.style.PSPDFCatalog_AIAssistantDialog_Dark)
                    .build()

                documentState = rememberDocumentState(assetProvider, activityConfiguration)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    DocumentView(
                        documentState = documentState,
                        documentManager = getDefaultDocumentManager(
                            documentListener = DefaultListeners.documentListeners(
                                onDocumentLoaded = {
                                    enabled = true
                                }
                            ),
                            uiListener = DefaultListeners.uiListeners(
                                onImmersiveModeEnabled = { toolbarVisibility = it }
                            )
                        )
                    )
                }
                CustomToolbar(documentState, enabled, toolbarVisibility)
            }
        }
    }
    var assistant: AiAssistant? = null

    override fun getAiAssistant(): AiAssistant {
        val documentDescriptor = DocumentDescriptor.fromDataProviders(listOf(assetProvider), listOf(), listOf())

        return assistant ?: run {
            createAiAssistant(
                context = this@AiAssistantComposeActivity,
                documentsDescriptors = listOf(documentDescriptor),
                ipAddress = ipAddressValue.orEmpty(),
                sessionId = sessionId,
                jwtToken = { documentIds ->
                    JwtGenerator.generateJwtToken(
                        this@AiAssistantComposeActivity,
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
            ).also {
                assistant = it
            }
        }
    }

    override fun navigateTo(
        documentRect: List<RectF>,
        pageIndex: Int,
        documentIndex: Int
    ) {
        documentState.documentConnection.highlight(pageIndex, documentRect)
    }

    companion object {
        const val EXTRA_URI = "JetpackComposeImageActivity.DocumentUri"
        const val PREFERENCES_NAME = "Nutrient.AiAssistant"
        const val PREF_AI_IP_ADDRESS = "ai_ip_address"
    }
}

/**
 * A custom toolbar component for document viewer with animated visibility.
 *
 * @param documentState The current state of the document being viewed
 * @param enabled Whether the toolbar actions are enabled
 * @param toolbarVisibility Whether the toolbar should be visible
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomToolbar(
    documentState: DocumentState,
    enabled: Boolean,
    toolbarVisibility: Boolean
) {
    val localDensity = LocalDensity.current

    // Animate toolbar appearance/disappearance with slide, expand and fade effects
    AnimatedVisibility(
        visible = toolbarVisibility,
        enter = slideInVertically { with(localDensity) { -40.dp.roundToPx() } } +
            expandVertically(expandFrom = Alignment.Top) +
            fadeIn(initialAlpha = 0.3f),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        TopAppBar(
            title = {
                // Display document title or empty string if null
                Text(
                    text = documentState.getTitle().orEmpty(),
                    color = Color.White
                )
            },
            actions = {
                // AI Assistant action button
                IconButton(
                    onClick = {
                        documentState.toggleView(PdfActivity.MENU_OPTION_AI_ASSISTANT)
                    },
                    enabled = enabled
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ai_assistant),
                        contentDescription = "AI Assistant",
                        tint = Color.White
                    )
                }

                // Settings action button
                IconButton(
                    onClick = {
                        documentState.toggleView(PdfActivity.MENU_OPTION_SETTINGS)
                    },
                    enabled = enabled
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_topbar_settings),
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}
