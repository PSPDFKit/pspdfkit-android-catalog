/*
 *   Copyright © 2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.customsearchuicompose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.search.TextSearch
import com.pspdfkit.jetpack.compose.interactors.DocumentState
import com.pspdfkit.ui.search.SearchResultHighlighter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CustomSearchUiComposeViewModel : ViewModel() {

    var searchQuery by mutableStateOf("")
        private set

    private var document: PdfDocument? = null

    private var textSearch: TextSearch? = null
    private var highlighter: SearchResultHighlighter? = null

    fun onDocumentLoaded(document: PdfDocument, pdfConfiguration: PdfConfiguration, documentState: DocumentState, highlighter: SearchResultHighlighter?) {
        this.document = document
        textSearch = TextSearch(
            document,
            pdfConfiguration
        )
        highlighter?.let {
            documentState.documentConnection.addDrawableProvider(it)
        }
        this.highlighter = highlighter
    }

    fun performSearch(query: String) {
        searchQuery = query

        viewModelScope.launch(Dispatchers.Default) {
            textSearch?.performSearch(searchQuery)?.let { results ->
                if (results.isNotEmpty()) {
                    highlighter?.addSearchResults(results)
                } else {
                    highlighter?.clearSearchResults()
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ) = CustomSearchUiComposeViewModel() as T
        }
    }
}
