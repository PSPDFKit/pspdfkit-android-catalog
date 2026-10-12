/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.composeviewer

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The viewer's settings: the tri-state theme choice (system / light / dark, driving both the
 * app chrome and the document theme), Material You dynamic color (API 31+), and clearing the
 * imported documents. Everything persists in the example's DataStore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(viewModel: ComposeViewerViewModel, onBack: () -> Unit) {
    val darkMode by viewModel.darkMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            ThemeChoiceRow("Follow system", selected = darkMode == ViewerDarkMode.SYSTEM) {
                viewModel.setDarkMode(ViewerDarkMode.SYSTEM)
            }
            ThemeChoiceRow("Light", selected = darkMode == ViewerDarkMode.LIGHT) {
                viewModel.setDarkMode(ViewerDarkMode.LIGHT)
            }
            ThemeChoiceRow("Dark", selected = darkMode == ViewerDarkMode.DARK) {
                viewModel.setDarkMode(ViewerDarkMode.DARK)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dynamic color", style = MaterialTheme.typography.bodyLarge)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        Text(
                            text = "Requires Android 12 or newer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    onCheckedChange = { viewModel.setDynamicColor(it) },
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                )
            }

            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    viewModel.clearImportedDocuments()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text("Clear imported documents")
            }
        }
    }
}

@Composable
private fun ThemeChoiceRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
