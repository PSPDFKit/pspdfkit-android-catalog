/*
 *   Copyright © 2023-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.utils

/**
 *  Checks if this string contains all characters in [stringToMatch], in order.
 *  For performance reasons - this method is case-sensitive.
 **/
fun String.fuzzlyMatches(stringToMatch: String): Boolean {
    if (length < stringToMatch.length) {
        return false
    }

    var currentIndex = 0
    stringToMatch.forEach { charToMatch ->
        var charMatched = false
        while (!charMatched && currentIndex <= lastIndex) {
            if (this[currentIndex] == charToMatch) {
                charMatched = true
            }
            currentIndex++
        }

        if (!charMatched) {
            return false
        }
    }

    return true
}
