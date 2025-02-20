/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.ui.model.Action
import com.pspdfkit.catalog.ui.model.ButtonPreference
import com.pspdfkit.catalog.ui.model.CheckboxPreference
import com.pspdfkit.catalog.ui.model.Dispatcher
import com.pspdfkit.catalog.ui.model.IntegerPreference
import com.pspdfkit.catalog.ui.model.Preference
import com.pspdfkit.catalog.ui.model.PreferenceKeys
import com.pspdfkit.catalog.ui.model.PreferencesSection
import com.pspdfkit.catalog.ui.model.RadioPreference
import com.pspdfkit.catalog.ui.model.State
import com.pspdfkit.catalog.ui.model.searchQueryOrBlank
import com.pspdfkit.catalog.ui.theming.AlphaDefs
import com.pspdfkit.catalog.ui.theming.Animations
import com.pspdfkit.catalog.ui.theming.Dimens
import com.pspdfkit.catalog.utils.filterBySearchState
import com.pspdfkit.catalog.utils.getAnnotationReplyFeaturesFromString
import com.pspdfkit.catalog.utils.getPageLayoutFromString
import com.pspdfkit.catalog.utils.getPageScrollDirectionFromString
import com.pspdfkit.catalog.utils.getRadioOptionStringFromEnumName
import com.pspdfkit.catalog.utils.getThemeModeFromString
import com.pspdfkit.catalog.utils.getThumbnailBarModeFromString
import com.pspdfkit.catalog.utils.getUserInterfaceModeFromString
import com.pspdfkit.utils.getSupportPackageInfo
import java.util.Locale

/**
 * Main page containing the expandable list of user options.
 * Different types of interactive preference setting Composables are exposed to the user:
 * [CheckboxSetting]s, [RadioSetting]s, [ButtonSetting]s, and [IntegerSetting]s.
 *
 * Each of these represent an internal preference, as seen in [Preference] and [com.pspdfkit.catalog.utils.CatalogPreferences].
 */
@Composable
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
fun Preferences(
    state: State,
    dispatcher: Dispatcher
) {
    var preferences by remember { mutableStateOf(listOf<PreferencesSection>()) }

    // I'm using a LaunchedEffect here to filter the list in a background thread
    LaunchedEffect(state.preferenceSections, state.searchState) {
        preferences = state.preferenceSections.filterBySearchState(state.searchState, preferences) { parent, filteredChildren ->
            PreferencesSection(parent.title, filteredChildren)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        // We use our own ExpandableList, which implements a LazyColumn.
        ExpandableList(
            searchQuery = state.searchState.searchQueryOrBlank(),
            itemsInSections = preferences,
            expandedSectionsState = state.expandedPreferenceSectionTitles,
            sectionKey = { it.title },
            areSectionHeadersSticky = false,

            topHeaderLayout = {
                Column(
                    modifier = Modifier.padding(Dimens.preferencesPageHeaderPadding)
                ) {
                    Image(
                        modifier = Modifier.padding(top = 16.dp),
                        painter = painterResource(id = R.drawable.ic_pspdfkit_circle_logo),
                        contentDescription = stringResource(R.string.preferences_pspdfkit_logo)
                    )

                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.h3,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .alpha(AlphaDefs.title)
                            .padding(top = 11.dp, bottom = 1.dp)
                    )

                    val context = LocalContext.current
                    val version = remember { context.packageManager.getSupportPackageInfo(context.packageName, 0).versionName }
                    Text(
                        modifier = Modifier.alpha(AlphaDefs.half),
                        text = stringResource(R.string.preferences_header_version, version ?: ""),
                        style = MaterialTheme.typography.body1
                    )
                }

                SpacerLine()
            },

            sectionHeaderLayout = { sectionIndex, sectionIsExpanded, _ ->
                val title = preferences.elementAt(sectionIndex).title
                PreferenceHeader(
                    sectionTitle = title,
                    expanded = sectionIsExpanded,
                    onExpandButtonClicked = {
                        val action = Action.TogglePreferenceSection(title)
                        dispatcher(action)
                    }
                )
            },

            itemLayout = { preference ->
                Column(
                    modifier = Modifier.padding(Dimens.preferencesListPadding)
                ) {
                    when (preference) {
                        is CheckboxPreference -> CheckboxSetting(
                            value = state.preferences[preference.key] as Boolean,
                            preference = preference,
                            onValueChanged = { key, value ->
                                dispatcher(Action.PreferenceChanged(key, value))
                            }
                        )

                        is IntegerPreference -> IntegerSetting(
                            value = state.preferences[preference.key] as Int,
                            preference = preference,
                            onValueChanged = { key, value ->
                                dispatcher(Action.PreferenceChanged(key, value))
                            }
                        )

                        is ButtonPreference -> ButtonSetting(
                            preference = preference,
                            onButtonPressed = { dispatcher(Action.PreferenceButtonTapped(it)) }
                        )

                        is RadioPreference -> RadioSetting(
                            state = state,
                            preference = preference,
                            onValueChanged = { key, value ->
                                dispatcher(Action.PreferenceChanged(key, value))
                            }
                        )
                    }
                }
            },

            sectionFooterLayout = {
                SpacerTransparentFooter()
            },

            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CheckboxSetting(
    value: Boolean,
    preference: Preference<Boolean>,
    onValueChanged: (Preferences.Key<Boolean>, Boolean) -> Unit
) {
    Column(
        modifier = Modifier.clickable { onValueChanged(preference.key, !value) }
    ) {
        Row(
            modifier = Modifier
                .height(Dimens.inlineSettingHeight)
                .fillMaxWidth(),
            verticalAlignment = CenterVertically
        ) {
            Checkbox(
                checked = value,
                onCheckedChange = { onValueChanged(preference.key, it) },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary)
            )

            Text(
                modifier = Modifier
                    .padding(start = 32.dp)
                    .alpha(AlphaDefs.title),
                text = preference.title,
                style = MaterialTheme.typography.h4
            )
        }

        SpacerLine()
    }
}

@Composable
@ExperimentalFoundationApi
fun PreferenceHeader(
    sectionTitle: String,
    expanded: Boolean,
    onExpandButtonClicked: () -> Unit
) {
    // Animations for switching colors to purple and alpha to 100% when a section is expanded.
    val expandSectionColorAnimation by Animations.sectionExpandingColor(expanded = expanded)
    val expandSectionAlphaAnimation by Animations.sectionExpandingAlpha(expanded = expanded)

    Box(
        modifier = Modifier.clickable { onExpandButtonClicked() }
    ) {
        Row(
            modifier = Modifier
                .height(Dimens.listHeaderHeight)
                .padding(Dimens.preferencesItemHeaderPadding)
                .fillMaxWidth(),
            verticalAlignment = CenterVertically
        ) {
            Text(
                modifier = Modifier.alpha(expandSectionAlphaAnimation),

                text = sectionTitle,
                style = MaterialTheme.typography.h1,
                color = expandSectionColorAnimation
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                ExpandSectionButton(
                    sectionTitle,
                    Dimens.iconSize,
                    expanded,
                    onExpandButtonClicked
                )
            }
        }
    }
}

@Composable
fun IntegerSetting(
    value: Int,
    preference: IntegerPreference,
    onValueChanged: (Preferences.Key<Int>, Int) -> Unit
) {
    val openDialog = remember { mutableStateOf(false) }
    val textFieldValue = remember { mutableStateOf(TextFieldValue()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openDialog.value = true }
    ) {
        // The description of IntegerSettings has the integer as the last number in the description.
        SettingItemWithDescription(preference, value.toString())
        SpacerLine()
    }

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                openDialog.value = false
            },

            title = {
                Text(
                    modifier = Modifier
                        .alpha(AlphaDefs.title),
                    text = preference.title,
                    style = MaterialTheme.typography.h4
                )
            },

            text = {
                Column {
                    Text(
                        modifier = Modifier
                            .alpha(AlphaDefs.half),
                        text = stringResource(R.string.preferences_starting_page_dialog_desc),
                        style = MaterialTheme.typography.body1
                    )

                    TextField(
                        modifier = Modifier
                            .padding(top = 10.dp),

                        value = textFieldValue.value,
                        onValueChange = {
                            // Here we need to both remember the TextFieldValue and notify the ViewModel.
                            textFieldValue.value = it
                            onValueChanged(
                                preference.key,
                                if (it.text.isEmpty()) 0 else it.text.toInt()
                            )
                        },
                        placeholder = { Text(text = value.toString()) },
                        maxLines = 1,
                        colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent),

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { openDialog.value = false }
                        )
                    )
                }
            },

            buttons = { }
        )
    }
}

@Composable
fun ButtonSetting(
    preference: ButtonPreference,
    onButtonPressed: (Preferences.Key<String>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onButtonPressed(preference.key) }
    ) {
        SettingItemWithDescription(preference)
        SpacerLine()
    }
}

/**
 * A simple setting item with a title and description.
 * Used by [ButtonSetting] and [IntegerSetting].
 */
@Composable
fun SettingItemWithDescription(preference: Preference<*>, descriptionExtras: String? = null) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(Dimens.buttonSettingPadding)
            .wrapContentHeight()
            .requiredHeightIn(min = Dimens.buttonSettingMinHeight)
    ) {
        Text(
            text = preference.title,
            style = MaterialTheme.typography.h4,
            modifier = Modifier.alpha(AlphaDefs.title)
        )

        if (preference.description.isNotEmpty()) {
            val finalDescription =
                if (descriptionExtras != null) {
                    preference.description.plus(descriptionExtras)
                } else {
                    preference.description
                }

            Text(
                text = finalDescription,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.alpha(AlphaDefs.half)
            )
        }
    }
}

/**
 * There are two kinds of radio settings: inline and as a pop-up alert.
 * Inline radio settings go directly to [RadioButtonGroup], while pop-ups go through [PopUpRadioSetting] first.
 */
@Composable
fun RadioSetting(
    preference: RadioPreference,
    state: State,
    onValueChanged: (Preferences.Key<String>, String) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalContext.current.resources
    val selectedOption = getRadioOptionStringFromEnumName(state.preferences[preference.key] as String, context)

    val possibleOptions = remember(preference.possibleValuesResource) {
        resources.getStringArray(preference.possibleValuesResource).toList()
    }

    val onSelectionChanged: (String) -> Unit = {
        when (preference.key) {
            PreferenceKeys.PageScrollDirection -> onValueChanged(
                preference.key,
                getPageScrollDirectionFromString(it, context).name
            )

            PreferenceKeys.PageLayoutMode -> onValueChanged(
                preference.key,
                getPageLayoutFromString(it, context).name
            )

            PreferenceKeys.SystemUserInterfaceMode -> onValueChanged(
                preference.key,
                getUserInterfaceModeFromString(it, context).name
            )

            PreferenceKeys.ThumbnailBarMode -> onValueChanged(
                preference.key,
                getThumbnailBarModeFromString(it, context).name
            )

            PreferenceKeys.AnnotationReplies -> onValueChanged(
                preference.key,
                getAnnotationReplyFeaturesFromString(it, context).name
            )

            PreferenceKeys.ThemeMode -> onValueChanged(
                preference.key,
                getThemeModeFromString(it, context).name
            )
        }
    }

    if (preference.isInline) {
        RadioButtonGroup(
            selectedOption = selectedOption,
            possibleOptions = possibleOptions,
            onSelectionChanged = onSelectionChanged
        )
    } else {
        PopUpRadioSetting(
            preference = preference,
            selectedOption = selectedOption,
            possibleOptions = possibleOptions,
            onSelectionChanged = onSelectionChanged
        )
    }
}

@Composable
fun PopUpRadioSetting(
    preference: RadioPreference,
    selectedOption: String,
    possibleOptions: Collection<String>,
    onSelectionChanged: (String) -> Unit
) {
    val openDialog = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .height(Dimens.popupRadioSettingHeight)
            .fillMaxWidth()
            .clickable { openDialog.value = true },
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier
                .alpha(AlphaDefs.title),
            text = preference.title,
            style = MaterialTheme.typography.h4
        )

        Text(
            modifier = Modifier
                .alpha(AlphaDefs.half),
            text = selectedOption,
            style = MaterialTheme.typography.h4
        )
    }

    Box(
        contentAlignment = Alignment.BottomStart
    ) {
        SpacerLine()
    }

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                openDialog.value = false
            },

            title = {
                Text(
                    modifier = Modifier
                        .alpha(AlphaDefs.title),
                    text = preference.title,
                    style = MaterialTheme.typography.h4
                )
            },

            text = {
                Column {
                    RadioButtonGroup(
                        selectedOption = selectedOption,
                        possibleOptions = possibleOptions
                    ) {
                        openDialog.value = false
                        onSelectionChanged.invoke(it)
                    }
                }
            },

            buttons = { }
        )
    }
}

@Composable
fun RadioButtonGroup(
    selectedOption: String,
    possibleOptions: Collection<String>,
    onSelectionChanged: ((String) -> Unit)
) {
    possibleOptions.forEach { currentOption ->

        val isSelected = remember(selectedOption, currentOption) {
            currentOption.lowercase(Locale.getDefault()) == selectedOption.lowercase(Locale.getDefault())
        }

        Column {
            Row(
                Modifier
                    .height(Dimens.inlineSettingHeight)
                    .fillMaxWidth()
                    .selectable(
                        selected = currentOption == selectedOption,
                        role = Role.RadioButton,
                        onClick = {
                            onSelectionChanged.invoke(currentOption)
                        }
                    ),
                verticalAlignment = CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary),
                    onClick = null
                )
                Text(
                    modifier = Modifier
                        .padding(start = 32.dp)
                        .alpha(AlphaDefs.title),
                    text = currentOption,
                    style = MaterialTheme.typography.h4
                )
            }

            SpacerLine()
        }
    }
}
