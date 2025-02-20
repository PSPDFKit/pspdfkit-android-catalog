/*
 *   Copyright © 2023-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   THIS SOURCE CODE AND ANY ACCOMPANYING DOCUMENTATION ARE PROTECTED BY INTERNATIONAL COPYRIGHT LAW
 *   AND MAY NOT BE RESOLD OR REDISTRIBUTED. USAGE IS BOUND TO THE PSPDFKIT LICENSE AGREEMENT.
 *   UNAUTHORIZED REPRODUCTION OR DISTRIBUTION IS SUBJECT TO CIVIL AND CRIMINAL PENALTIES.
 *   This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.barcodescanner.repo

import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.pspdfkit.catalog.barcodescanner.utils.ScanResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

internal class ScannerRepoImpl(
    private val scanner: GmsBarcodeScanner
) : ScannerRepo {

    override fun startScanning(): Flow<ScanResult> {
        return callbackFlow {
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    launch {
                        send(getBarcodeData(barcode))
                    }
                }.addOnFailureListener {
                    launch {
                        send(ScanResult.Error(it))
                    }
                }
            awaitClose()
        }
    }

    private fun getBarcodeData(barcode: Barcode): ScanResult {
        // At the moment, we just want a URL from the barcode. Extend this if we need more types in future
        return when (barcode.valueType) {
            Barcode.TYPE_URL -> {
                val url = barcode.url?.url
                return if (url != null) {
                    ScanResult.Success(url)
                } else {
                    ScanResult.Error(Exception())
                }
            }

            else -> ScanResult.Error(Exception())
        }
    }
}
