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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pspdfkit.ai.createAiAssistant
import com.pspdfkit.ai.showAiAssistant
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.SdkExample.Companion.WELCOME_DOC
import com.pspdfkit.catalog.examples.kotlin.AiAssistantComposeActivity.Companion.PREFERENCES_NAME
import com.pspdfkit.catalog.examples.kotlin.AiAssistantComposeActivity.Companion.PREF_AI_IP_ADDRESS
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.document.providers.getDataProviderFromDocumentSource
import com.pspdfkit.jetpack.compose.interactors.DefaultListeners
import com.pspdfkit.jetpack.compose.interactors.DocumentState
import com.pspdfkit.jetpack.compose.interactors.getDefaultDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.ui.DocumentDescriptor
import io.nutrient.domain.ai.AiAssistant
import io.nutrient.domain.ai.AiAssistantProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * An example that demonstrates how to implement AI Assistant with multiple documents for the DocumentView in a Compose way.
 */
class AiAssistantMultiDocComposeExample(context: Context) : SdkExample(
    context,
    R.string.jetpackAiAssistantMultiDocExampleTitle,
    R.string.jetpackAiAssistantMultiDocExampleDescription
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val intent = Intent(context, AiAssistantMultiDocComposeActivity::class.java)
        context.startActivity(intent)
    }
}

class AiAssistantMultiDocComposeActivity : AppCompatActivity(), AiAssistantProvider {
    val assetFiles = listOf(WELCOME_DOC, "Scientific-paper.pdf", "Teacher.pdf", "The-Cosmic-Context-for-Life.pdf")
    val documentDescriptors = assetFiles.map { DocumentDescriptor.fromDataProviders(listOf(AssetDataProvider(it)), listOf(), listOf()) }
    private lateinit var activityConfiguration: PdfActivityConfiguration
    private val sessionId = AiAssistantMultiDocComposeActivity::class.java.simpleName
    private var ipAddressValue: String? = null
    var documentStateMap = mutableMapOf<Int, DocumentState>()

    private val _currentDocumentIndex = MutableStateFlow(0)
    val currentDocumentIndex: StateFlow<Int> = _currentDocumentIndex.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        ipAddressValue = preferences.getString(PREF_AI_IP_ADDRESS, "") ?: ""

        setContent {
            var toolbarVisibility by remember { mutableStateOf(true) }
            activityConfiguration = PdfActivityConfiguration.Builder(LocalContext.current)
                .setAiAssistantEnabled(true)
                .defaultToolbarEnabled(false)
                .scrollDirection(PageScrollDirection.VERTICAL)
                .theme(R.style.PSPDFCatalog_AIAssistantDialog)
                .themeDark(R.style.PSPDFCatalog_AIAssistantDialog_Dark)
                .build()

            CatalogTheme {
                val currentIndex by currentDocumentIndex.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    val pagerState = rememberPagerState(currentIndex) { documentDescriptors.size }
                    LaunchedEffect(currentIndex) {
                        pagerState.animateScrollToPage(currentIndex)
                    }
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        beyondViewportPageCount = documentDescriptors.size,
                        modifier = Modifier
                            .fillMaxSize()
                    ) { page ->
                        if (page < documentDescriptors.size) {
                            key(page) {
                                val dataProvider = remember(page) {
                                    documentDescriptors[page].documentSource.getDataProviderFromDocumentSource()
                                }
                                // Create or retrieve the DocumentState for the current page.
                                val documentState = documentStateMap[page] ?: rememberDocumentState(dataProvider, activityConfiguration).also { documentStateMap.put(page, it) }
                                Box(modifier = Modifier.fillMaxSize()) {
                                    DocumentView(
                                        modifier = Modifier.fillMaxSize(),
                                        documentState = documentState,
                                        documentManager = getDefaultDocumentManager(
                                            uiListener = DefaultListeners.uiListeners(
                                                onImmersiveModeEnabled = { toolbarVisibility = it }
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }

                    CustomToolbar(
                        onClick = {
                            showAiAssistant(this@AiAssistantMultiDocComposeActivity)
                        },
                        toolbarVisibility = toolbarVisibility,
                        documentDescriptors = documentDescriptors,
                        currentIndex
                    ) {
                        _currentDocumentIndex.value = it
                    }
                }
            }
        }
    }
    var assistant: AiAssistant? = null

    override fun getAiAssistant(): AiAssistant {
        return assistant ?: run {
            createAiAssistant(
                context = this@AiAssistantMultiDocComposeActivity,
                documentsDescriptors = documentDescriptors,
                serverUrl = "http://$ipAddressValue:4000",
                sessionId = sessionId,
                jwtToken = { documentIds ->
                    JwtGenerator.generateJwtToken(
                        this@AiAssistantMultiDocComposeActivity,
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
        _currentDocumentIndex.value = documentIndex
        documentStateMap[documentIndex]?.documentConnection?.highlight(pageIndex, documentRect)
    }
}

/**
 * A custom toolbar component for document viewer with animated visibility.
 *
 * @param onClick Callback for AI Assistant button click
 * @param toolbarVisibility Whether the toolbar should be visible
 * @param documentDescriptors List of [DocumentDescriptor] to display titles in the toolbar
 * @param currentIndex The index of the currently selected document
 * @param onPagerIndexChange Callback to change the pager index when a tab is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomToolbar(
    onClick: () -> Unit,
    toolbarVisibility: Boolean,
    documentDescriptors: List<DocumentDescriptor>,
    currentIndex: Int,
    onPagerIndexChange: suspend (Int) -> Unit = {}
) {
    val localDensity = LocalDensity.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Animate toolbar appearance/disappearance with slide, expand and fade effects
    AnimatedVisibility(
        visible = toolbarVisibility,
        enter = slideInVertically { with(localDensity) { -40.dp.roundToPx() } } +
            expandVertically(expandFrom = Alignment.Top) +
            fadeIn(initialAlpha = 0.3f),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        Column {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = documentDescriptors[currentIndex].getTitle(context),
                            color = Color.White,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onClick
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ai_assistant),
                            contentDescription = "AI Assistant",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            TabRow(
                selectedTabIndex = currentIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                documentDescriptors.forEachIndexed { index, descriptor ->
                    Tab(
                        selected = currentIndex == index,
                        onClick = {
                            coroutineScope.launch {
                                onPagerIndexChange.invoke(index)
                            }
                        },
                        text = {
                            Text(
                                text = descriptor.getTitle(context),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    }
}
