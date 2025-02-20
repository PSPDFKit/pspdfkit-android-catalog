/*
 *   Copyright © 2023-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   THIS SOURCE CODE AND ANY ACCOMPANYING DOCUMENTATION ARE PROTECTED BY INTERNATIONAL COPYRIGHT LAW
 *   AND MAY NOT BE RESOLD OR REDISTRIBUTED. USAGE IS BOUND TO THE PSPDFKIT LICENSE AGREEMENT.
 *   UNAUTHORIZED REPRODUCTION OR DISTRIBUTION IS SUBJECT TO CIVIL AND CRIMINAL PENALTIES.
 *   This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.barcodescanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.pspdfkit.catalog.barcodescanner.repo.ScannerRepo
import com.pspdfkit.catalog.barcodescanner.repo.ScannerRepoImpl
import com.pspdfkit.catalog.barcodescanner.utils.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScannerViewModel(private val repo: ScannerRepo) : ViewModel() {

    private val _scanState = MutableStateFlow(ScanState())
    val scanStateStateFlow = _scanState.asStateFlow()

    init {
        scanCode()
    }

    private fun scanCode() {
        viewModelScope.launch {
            repo.startScanning().collect {
                when (it) {
                    is ScanResult.Success -> {
                        _scanState.value = ScanState(qrValue = it.value)
                    }
                    is ScanResult.Error -> {
                        _scanState.value = ScanState(error = it.error)
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(scanner: GmsBarcodeScanner) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = ScannerRepoImpl(scanner)
                return ScannerViewModel(repo) as T
            }
        }
    }
}
