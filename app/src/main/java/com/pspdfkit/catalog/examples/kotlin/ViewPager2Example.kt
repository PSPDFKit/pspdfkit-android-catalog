/*
 *   Copyright © 2024-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.SparseArray
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.SdkExample.Companion.WELCOME_DOC
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.listeners.DocumentListener
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.PdfUiFragment
import com.pspdfkit.ui.PdfUiFragmentBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DOCUMENT_LOAD_DELAY_MS = 400L // Delay to ensure document is fully loaded before restoring state

/**
 * An example demonstrating how to use multiple [PdfFragment]s within a [ViewPager2].
 * This example showcases:
 * - ViewPager2 integration with PDF fragments
 * - Theme switching (dark/light mode) with state preservation
 * - Fragment state management across configuration changes
 * - TabLayout integration for navigation
 */
class ViewPager2Example(context: Context) : SdkExample(context, R.string.viewPager2ExampleTitle, R.string.viewPager2ExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        context.startActivity(Intent(context, ViewPager2Activity::class.java))
    }
}

/**
 * Main activity that demonstrates ViewPager2 integration with PDF fragments.
 * Features:
 * - Displays multiple PDF documents in a swipeable ViewPager2
 * - Supports theme switching between light and dark modes
 * - Preserves fragment state across theme changes
 */
class ViewPager2Activity : AppCompatActivity() {
    /** ViewPager2 component for hosting PDF fragments */
    private lateinit var pager: ViewPager2

    /** Adapter for managing PDF fragments in the ViewPager2 */
    private var adapter: PdfFragmentAdapter? = null

    /** Coroutine scope for handling asynchronous operations */
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialise()
    }

    /**
     * Initializes the UI components after PDF files have been copied to local storage.
     * This method sets up the ViewPager2, adapter, TabLayout, and event listeners.
     */
    private fun initialise() {
        setContentView(R.layout.activity_view_pager)

        // Initialize ViewPager2
        pager = findViewById(R.id.view_pager)

        // Create a new adapter when initializing to ensure it uses the current theme
        // The callback restores fragment state after document loading
        adapter = PdfFragmentAdapter(this, true) { position ->
            scope.launch {
                // Small delay to ensure document is fully loaded before restoring state
                delay(DOCUMENT_LOAD_DELAY_MS)
                restoreState(position)
            }
        }
        pager.adapter = adapter

        // Disable user input to prevent manual swiping (navigation via tabs only)
        pager.isUserInputEnabled = false

        // Set current item to the selected page
        pager.setCurrentItem(selectedPage, false)

        // Setup TabLayout
        TabLayoutMediator(findViewById(R.id.tab_layout), pager) { tab, position ->
            tab.text = "Tab ${position + 1}"
        }.attach()

        // Setup page change callback to track current page and refresh when idle
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
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
        })

        // Setup UI controls (theme toggle button)
        setupButtons()
    }

    override fun onRestart() {
        super.onRestart()
        // Refresh the PDF source when activity restarts (e.g., returning from another app)
        updateSource(selectedPage)
    }

    /**
     * Sets up the theme toggle button with appropriate text and click behavior.
     * The button allows switching between light and dark modes while preserving state.
     */
    private fun setupButtons() {
        findViewById<TextView>(R.id.night_mode).apply {
            // Set button text based on current theme
            val targetMode = if (isDarkModeActive(context = this@ViewPager2Activity)) "Light" else "Dark"
            val labelText = "Change to $targetMode Mode"
            text = labelText

            setOnClickListener {
                // Save current fragment state before theme change to preserve user's position/zoom
                saveState(selectedPage)

                // Clean up current fragments to prevent memory leaks
                clearFragments()
                adapter = null

                // Toggle theme mode - this will recreate the activity
                AppCompatDelegate.setDefaultNightMode(
                    if (isDarkModeActive(context = this@ViewPager2Activity)) {
                        MODE_NIGHT_NO
                    } else {
                        MODE_NIGHT_YES
                    }
                )
                // Note: Activity will be recreated automatically when night mode changes
            }
        }
    }

    companion object {
        /** Tracks the currently selected page across activity recreations */
        var selectedPage = 0
    }
}

/**
 * FragmentStateAdapter that provides [PdfFragment] instances for each page in the ViewPager2.
 * This adapter:
 * - Creates 4 PDF fragments with different documents
 * - Applies appropriate theme configuration (dark/light)
 * - Handles document loading callbacks for state restoration
 *
 * @param context The FragmentActivity that hosts this adapter
 * @param onDocLoaded Callback invoked when a document finishes loading, receives the position
 */
class PdfFragmentAdapter(
    private val context: FragmentActivity,
    private val usePdfFragment: Boolean = true,
    private val onDocLoaded: (position: Int) -> Unit
) : FragmentStateAdapter(context) {

    /** Returns the total number of pages/fragments */
    override fun getItemCount(): Int = FileRepo.size

    /**
     * Creates a new PdfFragment for the specified position.
     * @param position The position in the ViewPager2 (0-3)
     * @return A configured PdfFragment with the appropriate PDF document and theme
     */
    override fun createFragment(position: Int): Fragment {
        PdfUiFragmentWrapper.callback = onDocLoaded
        val path = FileRepo.getPdfPath(position)

        return if (usePdfFragment) {
            PdfFragment.newInstance(
                path.toUri(),
                if (isDarkModeActive(context = context)) themeDark else themeLight
            ).apply {
                // Add listener to handle document loading completion
                addDocumentListener(object : DocumentListener {
                    override fun onDocumentLoaded(document: PdfDocument) {
                        super.onDocumentLoaded(document)
                        // Notify that this document has loaded so state can be restored
                        onDocLoaded.invoke(position)
                    }
                })
            }
        } else {
            PdfUiFragmentWrapper.paths[path] = position
            PdfUiFragmentBuilder.fromUri(
                context,
                path.toUri()
            )
                .configuration(
                    PdfActivityConfiguration.Builder(context)
                        .configuration(if (isDarkModeActive(context = context)) themeDark else themeLight)
                        .build()
                )
                .fragmentClass(PdfUiFragmentWrapper::class.java)
                .build()
        }
    }
}

class PdfUiFragmentWrapper : PdfUiFragment() {
    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)
        callback?.let { callback ->
            val position = extractPositionFromUri(document.documentSource.fileUri)
            callback(position)
        }
    }

    private fun extractPositionFromUri(uri: Uri?): Int {
        return paths[uri!!.path!!] ?: return 0
    }

    companion object {
        var callback: ((Int) -> Unit)? = null
        var paths = HashMap<String, Int>()
    }
}

/**
 * Helper object that manages the state of PDF fragments across different positions.
 * This singleton provides functionality to save and retrieve fragment states,
 * particularly useful for ViewPager implementations where fragments need to persist their state.
 */
object NutrientHelper {
    /**
     * Internal storage for fragment bundles indexed by position.
     * Uses SparseArray for efficient memory usage when dealing with sparse key-value pairs.
     */
    private val bundleList = SparseArray<Bundle>()

    /**
     * Saves the state bundle for a fragment at the specified position.
     * @param position The position index of the fragment in the ViewPager
     * @param bundle The Bundle containing the fragment's state data
     */
    fun saveState(position: Int, bundle: Bundle) { bundleList.put(position, bundle) }

    /**
     * Retrieves the saved state bundle for a fragment at the specified position.
     * @param position The position index of the fragment in the ViewPager
     * @return The saved Bundle, or null if no state exists for this position
     */
    fun getState(position: Int): Bundle? { return bundleList.get(position) }
}

/**
 * Base configuration for PDF viewer with vertical continuous scrolling.
 * This configuration is shared between dark and light themes.
 */
private val baseConfig = PdfConfiguration.Builder()
    .scrollMode(PageScrollMode.CONTINUOUS)
    .scrollDirection(PageScrollDirection.VERTICAL)

/**
 * PDF configuration for dark theme with inverted colors.
 */
val themeDark = baseConfig.invertColors(true).build()

/**
 * PDF configuration for light theme with normal colors.
 */
val themeLight = baseConfig.invertColors(false).build()

/**
 * Extension function to find a PDF fragment by its position in the ViewPager.
 * Fragments are tagged with "f{position}" pattern (e.g., "f0", "f1", etc.).
 *
 * @param position The position index of the fragment
 * @return The PdfFragment if found, null otherwise
 */
fun AppCompatActivity.findFragment(position: Int): PdfFragment? =
    supportFragmentManager.findFragmentByTag("f$position")?.let {
        when (it) {
            is PdfFragment -> it
            is PdfUiFragment -> it.pdfFragment
            else -> null
        }
    }

/**
 * Extension function to remove all Pdf(Ui) fragments from the activity's FragmentManager.
 * This is useful for cleanup operations or when resetting the ViewPager state.
 */
fun AppCompatActivity.clearFragments() {
    val fragmentManager = supportFragmentManager
    val pdfUiPredicate: (Fragment) -> Boolean = { it is PdfUiFragment }
    val pdfPredicate: (Fragment) -> Boolean = { it is PdfFragment }

    // check if we have any PdfUiFragments, if so, we want to remove those instead of PdfFragments
    val filterPredicate = if (fragmentManager.fragments.any(pdfUiPredicate)) pdfUiPredicate else pdfPredicate

    fragmentManager.fragments.filter(filterPredicate).forEach { fragment ->
        fragmentManager.beginTransaction().remove(fragment).commitNow()
    }
}

/**
 * Extension function to save the current state of a PDF fragment.
 * The state is stored in NutrientHelper for later retrieval.
 *
 * @param position The position index of the fragment whose state should be saved
 */
fun AppCompatActivity.saveState(position: Int) {
    findFragment(position)?.state?.let {
        NutrientHelper.saveState(position, it)
    }
}

/**
 * Extension function to update the PDF source for a fragment at the specified position.
 * This preserves the existing document source and reapplies it to the fragment.
 *
 * @param position The position index of the fragment to update
 */
fun AppCompatActivity.updateSource(position: Int) {
    val pdfFragment = findFragment(position) ?: return
    pdfFragment.document?.documentSource?.let { pdfFragment.setCustomPdfSource(it) }
}

/**
 * Extension function to restore the saved state for a PDF fragment.
 * The state is retrieved from NutrientHelper and applied to the fragment.
 *
 * @param position The position index of the fragment whose state should be restored
 */
fun AppCompatActivity.restoreState(position: Int) {
    val pdfFragment = findFragment(position) ?: return
    NutrientHelper.getState(position)?.let { pdfFragment.state = it }
}

/**
 * Extension function to refresh a PDF fragment.
 * This invalidates the fragment and its document cache, then refreshes the pages.
 * Useful for updating the display when the PDF content has changed.
 *
 * @param position The position index of the fragment to refresh
 */
fun AppCompatActivity.refresh(position: Int) {
    findFragment(position)?.apply {
        invalidate()
        document?.invalidateCache()
        refreshPages()
    }
}

/**
 * Determines whether dark mode is currently active by examining the system UI configuration.
 * This approach is more reliable than AppCompatDelegate.getDefaultNightMode() because it reflects
 * the actual applied theme rather than just the configured setting.
 *
 * @param context The context used to access system resources and configuration
 * @return true if dark mode is currently active, false otherwise
 */
fun isDarkModeActive(context: Context): Boolean {
    return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> true
        else -> false
    }
}

/**
 * Repository object that manages the list of PDF files used in the ViewPager2 example.
 * Provides methods to retrieve file names and their corresponding paths.
 */
object FileRepo {
    private val fileNames = listOf(
        WELCOME_DOC,
        "Scientific-paper.pdf",
        "Teacher.pdf",
        "The-Cosmic-Context-for-Life.pdf"
    )

    val size get() = fileNames.size

    fun getFileName(position: Int) = fileNames[position]

    fun getPdfPath(position: Int): String =
        "file:///android_asset/${getFileName(position)}"
}
