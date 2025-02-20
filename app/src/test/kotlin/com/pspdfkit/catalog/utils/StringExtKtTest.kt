/*
 *   Copyright © 2023-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringExtKtTest {

    private val originalString = "Fragment Example"

    @Test
    fun `string to match in main string returns true`() {
        assertTrue(originalString.fuzzlyMatches("Fragment Example"))
    }

    @Test
    fun `string to match not in main string returns false`() {
        assertFalse(originalString.fuzzlyMatches("Fragment Examplee"))
    }

    @Test
    fun `string to match in order with typo in main string returns true`() {
        assertTrue(originalString.fuzzlyMatches("Fgmnt Exmple"))
    }

    @Test
    fun `string to match not in order with typo in main string returns false`() {
        assertFalse(originalString.fuzzlyMatches("Fgrmnt Exmple"))
    }
}
