/*
 *   Copyright © 2023-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.examples.kotlin.instant.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.pspdfkit.catalog.barcodescanner.ScannerViewModel

/**
 * Provides live QR code scanning. Start this activity for result and it will retrieve first
 * recognized encoded data with the BARCODE_ENCODED_KEY. Requires Manifest.permission.CAMERA.
 */
class BarcodeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_AZTEC, Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = GmsBarcodeScanning.getClient(this, options)
        val vm = ViewModelProvider(this, ScannerViewModel.provideFactory(scanner))[ScannerViewModel::class.java]

        setContent {
            val state = vm.scanStateStateFlow.collectAsState()

            state.value.qrValue?.let { handleResult(it) }
        }
    }

    private fun handleResult(value: String) {
        val returnIntent = Intent()
        returnIntent.putExtra(BARCODE_ENCODED_KEY, value)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    companion object {
        const val BARCODE_RESULT_REQUEST_CODE = 2
        const val BARCODE_ENCODED_KEY = "BARCODE_ENCODED_KEY"
    }
}
