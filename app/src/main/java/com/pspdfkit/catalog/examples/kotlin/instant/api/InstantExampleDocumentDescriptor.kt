/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.instant.api

import android.os.Parcel
import android.os.Parcelable

/**
 * Describes single Instant document on the web preview server.
 */
class InstantExampleDocumentDescriptor(
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
    val webUrl: String
) : Parcelable {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(serverUrl)
        dest.writeString(documentId)
        dest.writeString(jwt)
        dest.writeString(documentCode)
        dest.writeString(webUrl)
    }

    private constructor (input: Parcel) : this(
        input.readString()!!,
        input.readString()!!,
        input.readString()!!,
        input.readString()!!,
        input.readString()!!
    )

    companion object CREATOR : Parcelable.Creator<InstantExampleDocumentDescriptor> {
        override fun createFromParcel(parcel: Parcel): InstantExampleDocumentDescriptor {
            return InstantExampleDocumentDescriptor(parcel)
        }

        override fun newArray(size: Int): Array<InstantExampleDocumentDescriptor?> {
            return arrayOfNulls(size)
        }
    }
}
