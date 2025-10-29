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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.pspdfkit.ai.showAiAssistant
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.SdkExample.Companion.WELCOME_DOC
import com.pspdfkit.catalog.examples.kotlin.AiAssistantComposeActivity.Companion.PREFERENCES_NAME
import com.pspdfkit.catalog.examples.kotlin.AiAssistantComposeActivity.Companion.PREF_AI_IP_ADDRESS
import com.pspdfkit.catalog.examples.kotlin.AiAssistantViewPagerActivity.Companion.EXTRA_CONFIGURATION
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.document.providers.getDataProviderFromDocumentSource
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.utils.getSupportParcelableExtra
import io.nutrient.domain.ai.AiAssistant
import io.nutrient.domain.ai.AiAssistantProvider

/**
 * Shows how to implement AI Assistant chat supporting multiple documents analysis and interaction inside a ViewPager.
 */
class AiAssistantViewPagerExample(context: Context) : SdkExample(context, R.string.aiAssistantViewPagerExampleTitle, R.string.aiAssistantViewPagerExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val intent = Intent(context, AiAssistantViewPagerActivity::class.java)
        configuration.setAiAssistantEnabled(true)
        intent.putExtra(
            EXTRA_CONFIGURATION,
            configuration.build().configuration
        )

        context.startActivity(intent)
    }
}

class AiAssistantViewPagerActivity : AppCompatActivity(), AiAssistantProvider {

    private lateinit var pager: ViewPager2
    private lateinit var fragmentAdapter: ViewPagerPdfFragmentAdapter
    private var selectedPage = 0
    private val sessionId = AiAssistantViewPagerActivity::class.java.simpleName
    var ipAddressValue = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_pager_ai_assistant)
        findViewById<Toolbar>(R.id.main_toolbar).let {
            it.setOnMenuItemClickListener { _ ->
                showAiAssistant(this@AiAssistantViewPagerActivity)
                false
            }
        }
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        ipAddressValue = preferences.getString(PREF_AI_IP_ADDRESS, "") ?: ""
        fragmentAdapter = ViewPagerPdfFragmentAdapter(this@AiAssistantViewPagerActivity, documentDescriptors)
        pager = findViewById<ViewPager2>(R.id.view_pager).apply {
            this.adapter = fragmentAdapter
            isUserInputEnabled = false
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    selectedPage = position // Save the selected page.
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    // Refresh the fragment when the user stops scrolling.
                    if (state == 0) supportFragmentManager.findFragmentByTag("f$selectedPage")?.let { if (it is PdfFragment) it.refreshPages() }
                }
            })
        }

        TabLayoutMediator(findViewById(R.id.tab_layout), pager) { tab, position -> tab.text = "Tab ${position + 1}" }.attach()
    }

    companion object {
        val assetFiles = listOf(WELCOME_DOC, "Scientific-paper.pdf", "Teacher.pdf", "The-Cosmic-Context-for-Life.pdf")
        internal const val EXTRA_CONFIGURATION = "Nutrient.AIAssistantViewPagerActivity.configuration"
    }
    var assistant: AiAssistant? = null

    override fun getAiAssistant(): AiAssistant {
        return assistant ?: initialiseAiAssistant().also {
            assistant = it
        }
    }

    val documentDescriptors = assetFiles.map {
        DocumentDescriptor.fromDataProviders(listOf(AssetDataProvider(it)), listOf(), listOf())
    }

    fun initialiseAiAssistant(): AiAssistant {
        return com.pspdfkit.ai.createAiAssistant(
            context = this,
            documentsDescriptors = documentDescriptors,
            serverUrl = "http://$ipAddressValue:4000",
            sessionId = sessionId,
            jwtToken = { documentIds ->
                JwtGenerator.generateJwtToken(
                    this@AiAssistantViewPagerActivity,
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
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun navigateTo(
        documentRect: List<RectF>,
        pageIndex: Int,
        documentIndex: Int
    ) {
        val fragment = fragmentAdapter.fragments[documentIndex]
        pager.setCurrentItem(documentIndex, true)
        fragment.highlight(this@AiAssistantViewPagerActivity, documentRect, pageIndex)
    }
}

/** Minimal [FragmentStateAdapter] to provide [PdfFragment] for each page. */
class ViewPagerPdfFragmentAdapter(val activity: AiAssistantViewPagerActivity, descriptors: List<DocumentDescriptor>) : FragmentStateAdapter(activity) {

    val configuration = activity.intent.getSupportParcelableExtra(EXTRA_CONFIGURATION, PdfConfiguration::class.java) ?: throw IllegalStateException("Activity Intent was missing configuration extra!")

    override fun getItemCount(): Int = 4
    val fragments = descriptors.map {
        val dataProvider = it.documentSource.getDataProviderFromDocumentSource()
        PdfFragment.newInstance(dataProvider, null, configuration)
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}
