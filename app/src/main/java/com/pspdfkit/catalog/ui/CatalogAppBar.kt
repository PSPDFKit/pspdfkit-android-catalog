/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.ui.model.Action
import com.pspdfkit.catalog.ui.model.Dispatcher
import com.pspdfkit.catalog.ui.model.Page
import com.pspdfkit.catalog.ui.model.SearchState
import com.pspdfkit.catalog.ui.model.State
import com.pspdfkit.catalog.ui.model.searchQueryOrBlank
import com.pspdfkit.catalog.ui.theming.AlphaDefs
import com.pspdfkit.catalog.ui.theming.CircularReveal
import com.pspdfkit.utils.getSupportPackageInfo
import kotlinx.coroutines.delay

@Composable
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
fun CatalogAppBar(
    state: State,
    dispatcher: Dispatcher,
    isTablet: Boolean = false
) {
    val isOnMainPage = state.currentPage == Page.ExampleList

    TopAppBar(
        title = {
            Row {
                Text(stringResource(R.string.app_name))

                val context = LocalContext.current
                val version = remember { context.packageManager.getSupportPackageInfo(context.packageName, 0).versionName }
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .alpha(AlphaDefs.titleSdkVersion)
                        .align(Alignment.Bottom),
                    text = version ?: "",
                    fontWeight = FontWeight.W400,
                    fontSize = 16.sp
                )
            }
        },

        navigationIcon =
        if (isOnMainPage) {
            null
        } else {
            (
                {
                    BackNavigationIconButton(
                        isTablet = isTablet,
                        onClick = { dispatcher(Action.UpButtonTapped) }
                    )
                }
                )
        },

        actions = {
            // These are wrapped in a row so they animate nicely when navigating.
            Row(
                modifier = Modifier.animateContentSize()
            ) {
                IconButton(
                    onClick = { dispatcher(Action.SearchButtonTapped) }
                ) {
                    // TODO: COMPOSE extract the strings
                    Icon(
                        painter = painterResource(id = R.drawable.ic_topbar_search),
                        tint = MaterialTheme.colors.onPrimary,
                        contentDescription = "Search button"
                    )
                }

                if (isOnMainPage) {
                    IconButton(
                        onClick = {
                            dispatcher(Action.SettingsButtonTapped)
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_topbar_settings),
                            tint = MaterialTheme.colors.onPrimary,
                            contentDescription = "Search button in the top bar."
                        )
                    }
                }
            }
        },
        elevation = 4.dp
    )

    SearchToolbar(
        searchState = state.searchState,
        isOnMainPage = isOnMainPage,
        onSearchQueryChanged = { dispatcher(Action.SearchQueryChanged(it)) },
        onSearchCancelled = { dispatcher(Action.CancelSearchButtonTapped) }
    )
}

@Composable
private fun BackNavigationIconButton(
    isTablet: Boolean,
    onClick: () -> Unit
) {
    val icon =
        if (isTablet) {
            Icons.Default.Close
        } else {
            Icons.AutoMirrored.Filled.ArrowBack
        }

    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            tint = MaterialTheme.colors.onPrimary,
            contentDescription = "Back navigation button in the top bar."
        )
    }
}

@ExperimentalComposeUiApi
@Composable
@ExperimentalAnimationApi
private fun SearchToolbar(
    searchState: SearchState,
    isOnMainPage: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSearchCancelled: () -> Unit
) {
    val isVisible = searchState != SearchState.Hidden

    val rippleOrigin = with(LocalDensity.current) {
        Offset(
            (if (isOnMainPage) 75 else 30).dp.toPx(),
            30.dp.toPx()
        )
    }

    CircularReveal(
        isVisible = isVisible,
        rippleOrigin = rippleOrigin
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusRequester = FocusRequester()

            // Invalidate the current text field contents only when it's hidding/showing
            // This is to prevent a feedback loop a single input turns into a repetition of itself.
            var textFieldValue by remember(isVisible) {
                mutableStateOf(TextFieldValue(searchState.searchQueryOrBlank()))
            }

            TextField(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                value = textFieldValue,
                onValueChange = {
                    // Here we need to both remember the TextFieldValue and notify the ViewModel.
                    textFieldValue = it
                },
                placeholder = { Text(text = "Search") },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusRequester.freeFocus()
                    }
                )
            )

            // debounce keyboard input slightly to improve search performance
            LaunchedEffect(key1 = textFieldValue) {
                delay(300)
                if (isVisible) {
                    onSearchQueryChanged(textFieldValue.text)
                }
            }

            IconButton(
                onClick = { onSearchCancelled() },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
            }

            LaunchedEffect(isVisible) {
                // Focus and display the software keyboard when the search is made visible
                if (isVisible) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}
