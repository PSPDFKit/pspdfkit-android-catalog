/*
 *   Copyright © 2023-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   THIS SOURCE CODE AND ANY ACCOMPANYING DOCUMENTATION ARE PROTECTED BY INTERNATIONAL COPYRIGHT LAW
 *   AND MAY NOT BE RESOLD OR REDISTRIBUTED. USAGE IS BOUND TO THE PSPDFKIT LICENSE AGREEMENT.
 *   UNAUTHORIZED REPRODUCTION OR DISTRIBUTION IS SUBJECT TO CIVIL AND CRIMINAL PENALTIES.
 *   This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui

import android.content.Context.MODE_PRIVATE
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.examples.kotlin.AiAssistantComposeActivity.Companion.PREFERENCES_NAME
import com.pspdfkit.catalog.examples.kotlin.AiAssistantComposeActivity.Companion.PREF_AI_IP_ADDRESS
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

@Composable
fun IpAddressDialog(dialogVisibility: Boolean, onDismissRequest: () -> Unit, action: () -> Unit) {
    val context = LocalContext.current
    val preferences = context.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
    var ipAddressValue: String = preferences.getString(PREF_AI_IP_ADDRESS, "") ?: ""

    var localIpAddress by remember { mutableStateOf(ipAddressValue) }

    if (dialogVisibility) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = {
                    if (ipAddressValue != localIpAddress) preferences.edit { putString(PREF_AI_IP_ADDRESS, localIpAddress) }
                    onDismissRequest.invoke()
                    action.invoke()
                }) {
                    Text(stringResource(R.string.ai_assistant_dialog_confirm_button))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        text = stringResource(R.string.ai_assistant_dialog_text),
                        style = TextStyle(
                            fontWeight = FontWeight.W500,
                            fontSize = 17.sp
                        )
                    )

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = localIpAddress,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.ai_assistant_dialog_textfield_placeholder)
                            )
                        },
                        onValueChange = {
                            localIpAddress = it
                        }
                    )
                }
            }
        )
    }
}
