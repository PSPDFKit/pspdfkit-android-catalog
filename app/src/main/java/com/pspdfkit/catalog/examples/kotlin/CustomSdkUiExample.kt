/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.pspdfkit.annotations.actions.GoToAction
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.page.PageLayoutMode
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.configuration.theming.ThemeMode
import com.pspdfkit.document.OutlineElement
import com.pspdfkit.document.PageBinding
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.preferences.PSPDFKitPreferences
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.nutrient.ui.settings.SettingsOptions
import io.nutrient.ui.settings.TIMEOUT_DEFAULT
import io.nutrient.ui.settings.TIMEOUT_INFINITE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

/**
 * Shows how to replace the SDK's settings, document info and outline UI with your own, driven by the
 * SDK's own data.
 *
 * Everything here uses public API. No SDK class had to be moved or made public. Each of the three
 * screens is a view over data you can already reach:
 *
 * - Settings is a view over the active [PdfActivityConfiguration], [PdfActivity.getScreenTimeout]
 *   and [PSPDFKitPreferences]. [SettingsOptions] is public and works as the carrier.
 * - Document info is a view over [PdfDocument.getPdfMetadata], [PdfDocument.getPageBinding] and a
 *   few document properties. Every metadata getter is nullable, so read them defensively.
 * - The outline is a view over [PdfDocument.getOutlineAsync], which emits public [OutlineElement]s.
 *
 * The toolbar items are intercepted with `setOnToolbarItemClickListener`, which the SDK consults
 * before its own default action. Returning `true` suppresses the built-in screen and leaves the
 * toolbar untouched, so there is no need to disable items and add your own.
 */
class CustomSdkUiExample(context: Context) :
    SdkExample(context, R.string.customSdkUiExampleTitle, R.string.customSdkUiExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder
                .fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(CustomSdkUiActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}

/** Which of our own panes is showing. */
private enum class CustomPane(val label: String) {
    Settings("Settings"),
    DocumentInfo("Document info"),
    Outline("Outline"),
}

/** The editable and read-only values our document info pane shows. */
data class CustomDocumentInfo(
    val title: String,
    val author: String,
    val subject: String,
    val keywords: String,
    val pageBinding: PageBinding,
    val isEditable: Boolean,
    val readOnlyRows: List<Pair<String, String>>,
)

/** One outline row, flattened out of the [OutlineElement] tree so it can be shown as a list. */
data class CustomOutlineRow(val title: String, val depth: Int, val pageLabel: String?, val pageIndex: Int?)

/**
 * A [PdfActivity] that keeps the standard settings, document info and outline toolbar items but
 * opens its own UI for each of them.
 */
class CustomSdkUiActivity : PdfActivity() {
    private var pane by mutableStateOf<CustomPane?>(null)
    private var outlineRows by mutableStateOf<List<CustomOutlineRow>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setOnToolbarItemClickListener { id ->
            when (id) {
                MENU_OPTION_SETTINGS -> {
                    pane = CustomPane.Settings
                    true
                }

                MENU_OPTION_DOCUMENT_INFO -> {
                    pane = CustomPane.DocumentInfo
                    true
                }

                MENU_OPTION_OUTLINE -> {
                    loadOutline()
                    pane = CustomPane.Outline
                    true
                }

                else -> false
            }
        }

        // Host our own Compose UI above the document. The activity is the lifecycle owner, so a
        // ComposeView added to the content view needs no further wiring.
        val content = findViewById<ViewGroup>(android.R.id.content)
        content.addView(
            ComposeView(this).apply {
                setContent {
                    MaterialTheme {
                        val current = pane
                        if (current != null) {
                            CustomSdkUiSheet(
                                initialPane = current,
                                settings = readSettings(),
                                documentInfo = document?.let(::readDocumentInfo),
                                outlineRows = outlineRows,
                                onDismiss = { pane = null },
                                onApplySettings = { changed ->
                                    pane = null
                                    applySettings(changed)
                                },
                                onSaveDocumentInfo = { edited ->
                                    pane = null
                                    saveDocumentInfo(edited)
                                },
                                onOutlineRowClick = { row ->
                                    pane = null
                                    row.pageIndex?.let { setPageIndex(it) }
                                },
                            )
                        }
                    }
                }
            },
        )
    }

    // Settings -------------------------------------------------------------------------------

    /** Reads the values the SDK's own settings dialog is opened with. */
    private fun readSettings(): SettingsOptions {
        val pdfConfiguration = configuration.configuration
        return SettingsOptions(
            scrollDirection = pdfConfiguration.scrollDirection,
            scrollMode = pdfConfiguration.scrollMode,
            layoutMode = pdfConfiguration.layoutMode,
            themeMode = pdfConfiguration.themeMode,
            screenTimeoutMillis = screenTimeout,
            visibleItems = configuration.settingsMenuItemShown,
            sepia = pdfConfiguration.isSepia,
        )
    }

    /** Applies changed settings back. */
    private fun applySettings(changed: SettingsOptions) {
        val preferences = PSPDFKitPreferences.get(this)
        screenTimeout = changed.screenTimeoutMillis

        // PdfConfiguration is a data class, so copy() is the supported way to derive one.
        // PdfConfiguration.Builder(existingConfiguration) is deprecated in favour of it.
        val pdfConfiguration = configuration.configuration.copy(
            scrollDirection = changed.scrollDirection,
            scrollMode = changed.scrollMode,
            layoutMode = changed.layoutMode,
            isSepia = changed.sepia,
        )

        // Changing the configuration recreates the document view, so do this last.
        configuration = PdfActivityConfiguration.Builder(configuration)
            .configuration(pdfConfiguration)
            .themeMode(changed.themeMode)
            .build()
    }

    // Document info --------------------------------------------------------------------------

    /** Reads the values the SDK's own document info screen shows. */
    private fun readDocumentInfo(document: PdfDocument): CustomDocumentInfo {
        val metadata = document.pdfMetadata
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        return CustomDocumentInfo(
            title = metadata.title ?: "",
            author = metadata.author ?: "",
            subject = metadata.subject ?: "",
            keywords = metadata.keywords?.joinToString(", ") ?: "",
            pageBinding = document.pageBinding,
            // A document that cannot be saved is shown, but not offered for editing.
            isEditable = document.isWritableAndCanSave,
            readOnlyRows = listOfNotNull(
                "Pages" to document.pageCount.toString(),
                metadata.producer?.let { "PDF producer" to it },
                metadata.creator?.let { "Content creator" to it },
                metadata.creationDate?.let { "Created" to dateFormat.format(it) },
                metadata.modificationDate?.let { "Modified" to dateFormat.format(it) },
            ),
        )
    }

    /**
     * Writes the edited values back and saves. [PdfDocument.saveIfModified] does disk work and can
     * throw, so it belongs off the main thread.
     */
    private fun saveDocumentInfo(edited: CustomDocumentInfo) {
        val document = document ?: return
        val metadata = document.pdfMetadata
        metadata.title = edited.title.ifBlank { null }
        metadata.author = edited.author.ifBlank { null }
        metadata.subject = edited.subject.ifBlank { null }
        metadata.keywords = edited.keywords
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { null }
        document.pageBinding = edited.pageBinding

        lifecycleScope.launch {
            val message = try {
                if (withContext(Dispatchers.IO) { document.saveIfModified() }) {
                    "Document info saved"
                } else {
                    "Nothing to save"
                }
            } catch (e: Exception) {
                "Could not save: ${e.message}"
            }
            Toast.makeText(this@CustomSdkUiActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Outline --------------------------------------------------------------------------------

    /**
     * Reads the document outline. getOutlineAsync is used rather than getOutline because the
     * blocking form must not be called on the main thread.
     */
    private fun loadOutline() {
        val document = document ?: return
        lifecycleScope.launch {
            outlineRows = try {
                val elements = withContext(Dispatchers.IO) { document.outlineAsync.blockingGet() }
                elements.flatten(depth = 0)
            } catch (e: Exception) {
                Toast.makeText(this@CustomSdkUiActivity, "Could not read the outline: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                emptyList()
            }
        }
    }

    /** Flattens the outline tree into indented rows. */
    private fun List<OutlineElement>.flatten(depth: Int): List<CustomOutlineRow> = flatMap { element ->
        listOf(
            CustomOutlineRow(
                title = element.title,
                depth = depth,
                pageLabel = element.pageLabel,
                pageIndex = (element.action as? GoToAction)?.pageIndex,
            ),
        ) + element.children.flatten(depth + 1)
    }
}

// UI -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomSdkUiSheet(
    initialPane: CustomPane,
    settings: SettingsOptions,
    documentInfo: CustomDocumentInfo?,
    outlineRows: List<CustomOutlineRow>,
    onDismiss: () -> Unit,
    onApplySettings: (SettingsOptions) -> Unit,
    onSaveDocumentInfo: (CustomDocumentInfo) -> Unit,
    onOutlineRowClick: (CustomOutlineRow) -> Unit,
) {
    var pane by remember { mutableStateOf(initialPane) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        PrimaryTabRow(selectedTabIndex = pane.ordinal) {
            CustomPane.entries.forEach { entry ->
                Tab(
                    selected = entry == pane,
                    onClick = { pane = entry },
                    text = { Text(entry.label) },
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (pane) {
                CustomPane.Settings -> SettingsPane(settings, onDismiss, onApplySettings)
                CustomPane.DocumentInfo -> DocumentInfoPane(documentInfo, onDismiss, onSaveDocumentInfo)
                CustomPane.Outline -> OutlinePane(outlineRows, onOutlineRowClick)
            }
        }
    }
}

@Composable
private fun SettingsPane(options: SettingsOptions, onDismiss: () -> Unit, onApply: (SettingsOptions) -> Unit) {
    // SettingsOptions holds plain mutable fields rather than Compose state, so edit a local copy and
    // hand a finished one back on apply.
    var scrollDirection by remember { mutableStateOf(options.scrollDirection) }
    var scrollMode by remember { mutableStateOf(options.scrollMode) }
    var layoutMode by remember { mutableStateOf(options.layoutMode) }
    var themeMode by remember { mutableStateOf(options.themeMode) }
    var keepScreenOn by remember { mutableStateOf(options.screenTimeoutMillis == TIMEOUT_INFINITE) }

    ChoiceRow("Scroll direction", PageScrollDirection.entries, scrollDirection) { scrollDirection = it }
    ChoiceRow("Page transition", PageScrollMode.entries, scrollMode) { scrollMode = it }
    ChoiceRow("Page layout", PageLayoutMode.entries, layoutMode) { layoutMode = it }
    ChoiceRow("Theme", ThemeMode.entries, themeMode) { themeMode = it }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Keep screen on", style = MaterialTheme.typography.bodyLarge)
        Switch(checked = keepScreenOn, onCheckedChange = { keepScreenOn = it })
    }

    ActionRow(confirmLabel = "Apply", onDismiss = onDismiss) {
        // Reuse the SDK's own carrier, and copy() keeps the values this UI does not offer.
        onApply(
            options.copy().apply {
                this.scrollDirection = scrollDirection
                this.scrollMode = scrollMode
                this.layoutMode = layoutMode
                this.themeMode = themeMode
                screenTimeoutMillis = if (keepScreenOn) TIMEOUT_INFINITE else TIMEOUT_DEFAULT
            },
        )
    }
}

@Composable
private fun DocumentInfoPane(info: CustomDocumentInfo?, onDismiss: () -> Unit, onSave: (CustomDocumentInfo) -> Unit) {
    if (info == null) {
        Text("The document is still loading.")
        return
    }

    var title by remember { mutableStateOf(info.title) }
    var author by remember { mutableStateOf(info.author) }
    var subject by remember { mutableStateOf(info.subject) }
    var keywords by remember { mutableStateOf(info.keywords) }
    var pageBinding by remember { mutableStateOf(info.pageBinding) }

    if (!info.isEditable) {
        Text("This document cannot be saved, so the fields are read only.", style = MaterialTheme.typography.bodySmall)
    }

    listOf(
        Triple("Title", title) { value: String -> title = value },
        Triple("Author", author) { value: String -> author = value },
        Triple("Subject", subject) { value: String -> subject = value },
        Triple("Keywords, comma separated", keywords) { value: String -> keywords = value },
    ).forEach { (label, value, onChange) ->
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            enabled = info.isEditable,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    val bindings = listOf(PageBinding.LEFT_EDGE, PageBinding.RIGHT_EDGE)
    Text("Page binding", style = MaterialTheme.typography.labelLarge)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        bindings.forEachIndexed { index, binding ->
            SegmentedButton(
                selected = binding == pageBinding,
                onClick = { pageBinding = binding },
                enabled = info.isEditable,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = bindings.size),
            ) {
                Text(if (binding == PageBinding.LEFT_EDGE) "Left" else "Right")
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    info.readOnlyRows.forEach { (label, value) ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
        }
    }

    ActionRow(confirmLabel = "Save", enabled = info.isEditable, onDismiss = onDismiss) {
        onSave(
            info.copy(
                title = title,
                author = author,
                subject = subject,
                keywords = keywords,
                pageBinding = pageBinding,
            ),
        )
    }
}

@Composable
private fun OutlinePane(rows: List<CustomOutlineRow>, onRowClick: (CustomOutlineRow) -> Unit) {
    if (rows.isEmpty()) {
        Text("This document has no outline.")
        return
    }
    rows.forEach { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = row.pageIndex != null) { onRowClick(row) }
                .padding(start = (row.depth * 16).dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(row.title, style = MaterialTheme.typography.bodyLarge)
            row.pageLabel?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                ?: row.pageIndex?.let { Text("${it + 1}", style = MaterialTheme.typography.bodyMedium) }
        }
        HorizontalDivider()
    }
}

/** A labelled single choice row over the values of one configuration enum. */
@Composable
private fun <T : Enum<T>> ChoiceRow(label: String, values: List<T>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            values.forEachIndexed { index, value ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = values.size),
                ) {
                    Text(value.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}

/** The dismiss and confirm buttons shared by the editable panes. */
@Composable
private fun ActionRow(confirmLabel: String, enabled: Boolean = true, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onDismiss) { Text("Close") }
        Button(enabled = enabled, onClick = onConfirm) { Text(confirmLabel) }
    }
}
