/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import androidx.lifecycle.ViewModel
import com.pspdfkit.utils.PdfLog
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * The purpose of this viewmodel is mainly to allow creating annotation only once upon configuration
 * change in onDocumentLoaded method.
 * This should be used in any place where we create/add annotations to page in onDocumentLoaded method to avoid duplicates
 */
class AnnotationCreationViewModel : ViewModel() {

    private var multiPageObjectsCreated = HashSet<Int>()

    private var objectsCreated = false

    fun createObjects(creator: () -> Unit) {
        if (!objectsCreated) {
            objectsCreated = true
            creator.invoke()
        }
    }

    /**
     * Creates objects on multiple pages. Helpful for creating stress tests scenarios.
     * By default, it creates objects on every 20th page, but you can specify a different step.
     */
    suspend fun createObjects(pageCount: Int, step: Int = 20, creator: (Int) -> Unit) {
        coroutineScope {
            for (pageIndex in 0 until pageCount step step) {
                launch {
                    if (multiPageObjectsCreated.contains(pageIndex)) return@launch
                    multiPageObjectsCreated.add(pageIndex)
                    creator.invoke(pageIndex)
                    PdfLog.d("AnnotationCreationViewModel", "Created objects on page $pageIndex")
                }
            }
        }
        PdfLog.d("AnnotationCreationViewModel", "All done")
    }
}
