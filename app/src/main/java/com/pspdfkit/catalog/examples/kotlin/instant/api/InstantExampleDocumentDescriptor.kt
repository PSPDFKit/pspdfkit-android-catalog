/*
 *   Copyright © 2020-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.instant.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Describes single Instant document on the web preview server.
 */
@Parcelize
data class InstantExampleDocumentDescriptor(
    /** Instant Server (Nutrient Document Engine) url.  */
    val serverUrl: String,
    /** Instant document id.  */
    private val documentId: String,
    /** Authentication token (JWT) used to authenticate access to the document.  */
    val jwt: String,
    /** Document code identifying document and editing group.  */
    @com.google.gson.annotations.SerializedName("encodedDocumentId")
    val documentCode: String,
    /** Web preview url.  */
    @com.google.gson.annotations.SerializedName("url")
    val webUrl: String,
) : Parcelable
