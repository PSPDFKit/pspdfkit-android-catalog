/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.library.LibraryFileSystemDataSource
import com.pspdfkit.document.library.PdfLibrary
import com.pspdfkit.document.library.QueryOptions
import com.pspdfkit.document.library.QueryPreviewResult
import com.pspdfkit.document.library.QueryResultListener
import com.pspdfkit.ui.PdfActivityIntentBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class IndexedFullTextSearchExample(context: Context) : SdkExample(
    context,
    R.string.indexedFtsExampleTitle,
    R.string.indexedFtsExampleDescription
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val intent = Intent(context, IndexedFullTextSearchActivity::class.java)
        context.startActivity(intent)
    }
}

class IndexedFullTextSearchActivity : ComponentActivity() {

    companion object {
        private const val EXTRACTED_PDFS_DIR = "extracted_pdfs"
    }

    private var pdfLibrary: PdfLibrary? = null
    private var dataSource: LibraryFileSystemDataSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CatalogTheme {
                LibrarySearchScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dataSource?.cleanup()
        pdfLibrary = null
        dataSource = null
    }

    @Composable
    private fun LibrarySearchScreen() {
        var searchQuery by remember { mutableStateOf("") }
        var searchResults by remember { mutableStateOf<Map<String, Set<QueryPreviewResult>>>(emptyMap()) }
        var isIndexing by remember { mutableStateOf(true) }
        var indexingProgress by remember { mutableStateOf("Preparing to index...") }
        var extractedDirectory by remember { mutableStateOf<File?>(null) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                try {
                    extractedDirectory = extractAllPdfAssets()
                    initializePdfLibrary(extractedDirectory!!) { progress ->
                        indexingProgress = progress
                        if (progress.contains("completed")) {
                            isIndexing = false
                        }
                    }
                } catch (e: Exception) {
                    indexingProgress = "Error: ${e.message}"
                    isIndexing = false
                }
            }
        }

        LaunchedEffect(searchQuery, isIndexing) {
            if (searchQuery.isNotEmpty() && pdfLibrary != null && !isIndexing) {
                performSearch(searchQuery) { results ->
                    searchResults = results
                }
            } else if (searchQuery.isEmpty()) {
                searchResults = emptyMap()
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                dataSource?.cleanup()
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search PDFs") },
                    placeholder = { Text("Enter search terms...") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isIndexing
                )

                if (isIndexing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = indexingProgress,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else if (searchQuery.isEmpty()) {
                    Text(
                        text = "Enter a search term to find content in the PDF library",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else if (searchResults.isEmpty()) {
                    Text(
                        text = "No results found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    searchResults.forEach { (_, results) ->
                        items(results.toList()) { result ->
                            SearchResultCard(
                                result = result,
                                onResultClick = { clickedResult ->
                                    openDocumentAtPage(clickedResult)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SearchResultCard(
        result: QueryPreviewResult,
        onResultClick: (QueryPreviewResult) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onResultClick(result) },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Document: ${getDocumentName(result.uid)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Page ${result.pageIndex + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = result.previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    private suspend fun extractAllPdfAssets(): File {
        return withContext(Dispatchers.IO) {
            val extractedDir = File(filesDir, EXTRACTED_PDFS_DIR)
            if (!extractedDir.exists()) {
                extractedDir.mkdirs()
            }

            val assetManager = assets
            val pdfAssets = assetManager.list("")?.filter { it.endsWith(".pdf") } ?: emptyList()

            for (assetName in pdfAssets) {
                val outputFile = File(extractedDir, assetName)
                if (!outputFile.exists()) {
                    assetManager.open(assetName).use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }

            extractedDir
        }
    }

    private suspend fun initializePdfLibrary(
        documentsDirectory: File,
        onProgress: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                onProgress("Initializing PDF library...")

                val libraryDbPath = File(filesDir, "pdf_library.db").absolutePath
                pdfLibrary = PdfLibrary(libraryDbPath)
                val library = pdfLibrary ?: throw IllegalStateException("Failed to create PdfLibrary instance")
                onProgress("Setting up data source...")
                dataSource = LibraryFileSystemDataSource(library, documentsDirectory)
                library.dataSource = dataSource

                onProgress("Indexing...")
                library.updateIndexFromDataSource()

                onProgress("Indexing completed!")
            } catch (e: Exception) {
                onProgress("Error during initialization: ${e.message}")
                throw e
            }
        }
    }

    private fun performSearch(
        query: String,
        onResults: (Map<String, Set<QueryPreviewResult>>) -> Unit
    ) {
        val library = pdfLibrary ?: return

        val queryOptions = QueryOptions.Builder()
            .generateTextPreviews(true)
            .maximumPreviewResultsPerDocument(3)
            .maximumPreviewResultsTotal(20)
            .build()

        library.search(
            query,
            queryOptions,
            object : QueryResultListener {
                override fun onSearchCompleted(searchString: String, results: Map<String, Set<Int>>) {
                    // Basic results handled by preview callback
                }

                override fun onSearchPreviewsGenerated(
                    searchString: String,
                    results: Map<String, Set<QueryPreviewResult>>
                ) {
                    runOnUiThread {
                        onResults(results)
                    }
                }
            }
        )
    }

    private fun openDocumentAtPage(result: QueryPreviewResult) {
        val descriptor = dataSource?.indexItemDescriptorForDocumentWithUid(result.uid)
        if (descriptor != null) {
            try {
                // Construct the full path to the extracted file
                val extractedDir = File(filesDir, EXTRACTED_PDFS_DIR)
                val documentFile = File(extractedDir, descriptor.documentPath)
                val fileUri = Uri.fromFile(documentFile)

                val configuration = PdfActivityConfiguration.Builder(this)
                    .page(result.pageIndex)
                    .build()

                val intent = PdfActivityIntentBuilder.fromUri(this, fileUri)
                    .configuration(configuration)
                    .build()

                startActivity(intent)
            } catch (e: Exception) {
                // Handle error - could show a toast or log
                android.util.Log.e("LibraryAssetSearch", "Error opening document: ${e.message}")
            }
        }
    }

    private fun getDocumentName(uid: String): String {
        val descriptor = dataSource?.indexItemDescriptorForDocumentWithUid(uid)
        return descriptor?.documentPath?.substringAfterLast("/") ?: "Unknown Document"
    }
}
