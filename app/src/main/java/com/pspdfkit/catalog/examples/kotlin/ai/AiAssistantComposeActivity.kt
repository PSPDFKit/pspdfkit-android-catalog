/*
 *   Copyright © 2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.ai

import JwtGenerator
import android.content.Context
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample.Companion.WELCOME_DOC
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.jetpack.compose.interactors.DefaultListeners
import com.pspdfkit.jetpack.compose.interactors.DocumentState
import com.pspdfkit.jetpack.compose.interactors.getDefaultDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.ui.PdfActivity
import io.nutrient.data.models.AiAssistantConfiguration
import io.nutrient.domain.ai.standaloneAiAssistant

class AiAssistantComposeActivity : AppCompatActivity() {
    private val sessionId = "my-test-session-id"
    private val assetProvider = AssetDataProvider(WELCOME_DOC)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val preferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val ipAddressValue = preferences.getString(PREF_AI_IP_ADDRESS, "") ?: ""

        setContent {
            var enabled by remember { mutableStateOf(false) }
            var toolbarVisibility by remember { mutableStateOf(true) }

            var ipAddress by remember {
                // ip address of the server running the AI Assistant
                mutableStateOf(ipAddressValue)
            }

            var isIpAddressDialogVisible by remember {
                mutableStateOf(true)
            }

            CatalogTheme {
                val activityConfiguration = PdfActivityConfiguration.Builder(LocalContext.current)
                    .setAiAssistantEnabled(true)
                    .defaultToolbarEnabled(false)
                    .theme(R.style.PSPDFCatalog_AIAssistantDialog)
                    .themeDark(R.style.PSPDFCatalog_AIAssistantDialog_Dark)
                    .build()

                val documentState = rememberDocumentState(assetProvider, activityConfiguration)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    AnimatedVisibility(visible = isIpAddressDialogVisible) {
                        IpAddressDialog(
                            ipAddress = ipAddress,
                            onIpAddressChanged = {
                                if (it.isNotEmpty()) {
                                    ipAddress = it
                                }
                            },
                            onConfirm = {
                                preferences.edit().putString(PREF_AI_IP_ADDRESS, ipAddress).apply()
                                isIpAddressDialogVisible = false
                            },
                            onDismissRequest = {
                                isIpAddressDialogVisible = false
                            }
                        )
                    }
                    AnimatedVisibility(visible = isIpAddressDialogVisible.not()) {
                        DocumentView(
                            documentState = documentState,
                            documentManager = getDefaultDocumentManager(
                                documentListener = DefaultListeners.documentListeners(
                                    onDocumentLoaded = {
                                        println("AIA onDocumentLoaded createAiAssistant")
                                        it.setAiAssistant(
                                            createAiAssistant(
                                                pdfDocument = it,
                                                ipAddress = ipAddress
                                            )
                                        )
                                        enabled = true
                                    }
                                ),
                                uiListener = DefaultListeners.uiListeners(
                                    onImmersiveModeEnabled = { toolbarVisibility = it }
                                )
                            )
                        )
                        CustomToolbar(documentState, enabled, toolbarVisibility)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun createAiAssistant(
        pdfDocument: PdfDocument,
        ipAddress: String
    ) = standaloneAiAssistant(
        this@AiAssistantComposeActivity,
        AiAssistantConfiguration(
            "http://$ipAddress:4000",
            JwtGenerator.generateJwtToken(
                this@AiAssistantComposeActivity,
                claims = mapOf(
                    "document_ids" to listOf(pdfDocument.permanentId?.toHexString()),
                    "session_ids" to listOf(sessionId),
                    "request_limit" to mapOf("requests" to 160, "time_period_s" to 1000 * 60)
                )
            ),
            sessionId
        )
    )

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

@Composable
fun IpAddressDialog(
    modifier: Modifier = Modifier,
    ipAddress: String,
    onIpAddressChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                }) {
                    Text(stringResource(R.string.ai_assistant_dialog_confirm_button))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        text = stringResource(R.string.ai_assistant_dialog_text),
                        style = TextStyle(
                            fontWeight = FontWeight.W500,
                            fontSize = 17.sp
                        )
                    )

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = ipAddress,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.ai_assistant_dialog_textfield_placeholder)
                            )
                        },
                        onValueChange = onIpAddressChanged
                    )
                }
            }
        )
    }
}
