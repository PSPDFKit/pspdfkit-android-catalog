/*
 *   Copyright © 2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfFragment

/** An example to show using multiple [PdfFragment]s in a [ViewPager2] */
class ViewPager2Example(context: Context) : SdkExample(context, R.string.viewPager2ExampleTitle, R.string.viewPager2ExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        context.startActivity(Intent(context, ViewPager2Activity::class.java))
    }
}

class ViewPager2Activity : AppCompatActivity() {

    private var selectedPage = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val adapter = PdfFragmentAdapter(this@ViewPager2Activity)
        setContentView(R.layout.activity_view_pager)
        val pager = findViewById<ViewPager2>(R.id.view_pager).apply {
            this.adapter = adapter
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
}

/** Minimal [FragmentStateAdapter] to provide [PdfFragment] for each page. */
class PdfFragmentAdapter(activity: ViewPager2Activity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int) = PdfFragment.newInstance(Uri.parse(getPdfPath(position)), PdfConfiguration.Builder().build())

    // Load PDFs from assets.
    private fun getPdfPath(position: Int) = "file:///android_asset/${when (position) {
        0 -> "Quickstart"
        1 -> "Scientific-paper"
        2 -> "Teacher"
        else -> "The-Cosmic-Context-for-Life"
    }}.pdf"
}
