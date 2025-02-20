/*
 *   Copyright © 2023-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   THIS SOURCE CODE AND ANY ACCOMPANYING DOCUMENTATION ARE PROTECTED BY INTERNATIONAL COPYRIGHT LAW
 *   AND MAY NOT BE RESOLD OR REDISTRIBUTED. USAGE IS BOUND TO THE PSPDFKIT LICENSE AGREEMENT.
 *   UNAUTHORIZED REPRODUCTION OR DISTRIBUTION IS SUBJECT TO CIVIL AND CRIMINAL PENALTIES.
 *   This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.barcodescanner.utils

sealed class ScanResult {
    internal data class Success(val value: String) : ScanResult()
    internal data class Error(val error: Throwable) : ScanResult()
}
