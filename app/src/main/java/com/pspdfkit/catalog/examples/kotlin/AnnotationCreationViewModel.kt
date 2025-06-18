/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import androidx.lifecycle.ViewModel

/**
 * The purpose of this viewmodel is mainly to allow creating annotation only once upon configuration
 * change in onDocumentLoaded method.
 * This should be used in any place where we create/add annotations to page in onDocumentLoaded method to avoid duplicates
 */
class AnnotationCreationViewModel : ViewModel() {

    private var objectsCreated = false

    fun createObjects(creator: () -> Unit) {
        if (!objectsCreated) {
            objectsCreated = true
            creator.invoke()
        }
    }
}
