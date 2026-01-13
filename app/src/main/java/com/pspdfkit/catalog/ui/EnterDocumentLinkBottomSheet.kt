/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A Compose bottom sheet for entering an Instant document link.
 *
 * @param isVisible Whether the bottom sheet should be visible
 * @param onDismiss Callback invoked when the bottom sheet is dismissed/cancelled
 * @param onConfirm Callback invoked when the user confirms with the entered document link
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterDocumentLinkBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (documentLink: String) -> Unit
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var documentLink by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Enter Document Link",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = documentLink,
                onValueChange = { documentLink = it },
                label = { Text("Document Link") },
                placeholder = { Text("Enter Document Link") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onConfirm(documentLink)
                        }
                    },
                    enabled = documentLink.isNotBlank()
                ) {
                    Text("Open Document")
                }
            }
        }
    }
}
