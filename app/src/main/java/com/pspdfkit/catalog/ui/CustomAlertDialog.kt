/*
 *   Copyright © 2023-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   THIS SOURCE CODE AND ANY ACCOMPANYING DOCUMENTATION ARE PROTECTED BY INTERNATIONAL COPYRIGHT LAW
 *   AND MAY NOT BE RESOLD OR REDISTRIBUTED. USAGE IS BOUND TO THE PSPDFKIT LICENSE AGREEMENT.
 *   UNAUTHORIZED REPRODUCTION OR DISTRIBUTION IS SUBJECT TO CIVIL AND CRIMINAL PENALTIES.
 *   This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pspdfkit.catalog.R
import com.pspdfkit.signatures.DigitalSignatureType

@Composable
fun SelectSignatureTypeDialog(dialogVisibility: Boolean, onDismissRequest: () -> Unit, action: (DigitalSignatureType) -> Unit) {
    if (dialogVisibility) {
        AlertDialog(
            onDismissRequest = {
                onDismissRequest.invoke()
            },
            title = {
                Text(
                    text = stringResource(id = R.string.selectDigitalSignatureType),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(stringResource(id = R.string.signatureDialogDescription))
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismissRequest.invoke()
                            action.invoke(DigitalSignatureType.BASIC)
                        }
                    ) {
                        Text(stringResource(id = R.string.digitalSignatureTypeBasic))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = {
                            onDismissRequest.invoke()
                            action.invoke(DigitalSignatureType.CADES)
                        }
                    ) {
                        Text(stringResource(id = R.string.digitalSignatureTypePAdES))
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun CustomAlertDialogPreview() {
    SelectSignatureTypeDialog(dialogVisibility = true, onDismissRequest = {}, action = {})
}
