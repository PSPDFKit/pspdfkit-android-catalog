/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.SdkExample.Companion.WELCOME_DOC
import com.pspdfkit.catalog.examples.kotlin.ViewPager2Activity.Companion.selectedPage
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfUiFragmentBuilder

/**
 * Simple example showing how to use PdfUiFragment with ViewPager2 and TabLayout.
 * This demonstrates a basic setup with 4 PDF documents in a swipeable tab interface.
 */
class SimplePdfViewPagerExample(context: Context) :
    SdkExample(context, R.string.simplePdfViewPagerExampleTitle, R.string.simplePdfViewPagerExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        context.startActivity(Intent(context, SimplePdfViewPagerActivity::class.java))
    }
}

/**
 * Activity that hosts a ViewPager2 with 4 PdfUiFragments and a TabLayout for navigation.
 */
class SimplePdfViewPagerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_pdf_view_pager)

        // Setup TabLayout with ViewPager2
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val rootView = this.findViewById<View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                topMargin = insets.top
                rightMargin = insets.right
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    // Update the globally tracked selected page
                    selectedPage = position
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    // Refresh the current fragment when scrolling stops
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        refresh(selectedPage)
                    }
                }
            },
        )

        // Setup ViewPager2
        val adapter = SimplePdfPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = "PDF ${position + 1}"
        }.attach()
    }
}

/**
 * Adapter that provides PdfUiFragment instances for each page.
 */
class SimplePdfPagerAdapter(private val fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    // List of PDF files to display
    private val pdfFiles =
        listOf(
            WELCOME_DOC,
            "Scientific-paper.pdf",
            "Teacher.pdf",
            "The-Cosmic-Context-for-Life.pdf",
        )

    override fun getItemCount(): Int = pdfFiles.size

    override fun createFragment(position: Int): Fragment {
        // Get the PDF URI from assets
        val pdfUri: Uri = "file:///android_asset/${pdfFiles[position]}".toUri()

        val activityConfiguration =
            PdfActivityConfiguration
                .Builder(fragmentActivity.applicationContext)
                // Content editing is disabled because PDFs loaded from assets (file:///android_asset/)
                // are read-only. To enable content editing, PDFs must be extracted from assets to
                // writable storage using ExtractAssetTask before loading them.
                .contentEditingEnabled(false)
                .build()

        // Build and return PdfUiFragment
        return PdfUiFragmentBuilder
            .fromUri(fragmentActivity, pdfUri)
            .configuration(activityConfiguration)
            .build()
    }
}
