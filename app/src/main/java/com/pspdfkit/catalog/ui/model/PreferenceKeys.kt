/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui.model

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceKeys {
    val PageScrollDirection = stringPreferencesKey("page_scroll_direction")
    val PageLayoutMode = stringPreferencesKey("page_layout_mode")
    val PageScrollContinuous = booleanPreferencesKey("scroll_continuously")
    val FitPageToWidth = booleanPreferencesKey("fit_page_to_width")
    val FirstPageAsSingle = booleanPreferencesKey("first_page_as_single")
    val ShowGapBetweenPages = booleanPreferencesKey("show_gap_between_pages")
    val ImmersiveMode = booleanPreferencesKey("immersive_mode")
    val SystemUserInterfaceMode = stringPreferencesKey("user_interface_view_mode")
    val HideUiWhenCreatingAnnotations = booleanPreferencesKey("hide_ui_when_creating_annotations")
    val ShowSearchAction = booleanPreferencesKey("show_search_action")
    val InlineSearch = booleanPreferencesKey("inline_search")
    val ThumbnailBarMode = stringPreferencesKey("thumbnail_bar_mode")
    val ShowThumbnailGridAction = booleanPreferencesKey("show_thumbnail_grid_action")
    val EnableDocumentOutline = booleanPreferencesKey("show_outline_action")
    val ShowAnnotationListAction = booleanPreferencesKey("show_annotation_list_action")
    val ShowPageNumberOverlay = booleanPreferencesKey("show_page_number_overlay")
    val ShowPageLabels = booleanPreferencesKey("show_page_labels")
    val InvertColors = booleanPreferencesKey("invert_colors")
    val Grayscale = booleanPreferencesKey("grayscale")
    val StartPage = intPreferencesKey("start_page")
    val RestoreLastViewedPage = booleanPreferencesKey("restore_last_viewed_page")
    val ClearCache = stringPreferencesKey("clear_cache")
    val ClearAppData = stringPreferencesKey("clear_app_data")
    val EnableAnnotationEditing = booleanPreferencesKey("enable_annotation_editing")
    val EnableAnnotationRotation = booleanPreferencesKey("enable_annotation_rotation")
    val AnnotationReplies = stringPreferencesKey("annotation_reply_features")
    val EnableTextSelection = booleanPreferencesKey("enable_text_selection")
    val EnableFormEditing = booleanPreferencesKey("enable_form_editing")
    val ShowShareAction = booleanPreferencesKey("show_share_action")
    val ShowPrintAction = booleanPreferencesKey("show_print_action")
    val ThemeMode = stringPreferencesKey("theme_mode")
    val EnableVolumeButtonsNavigation = booleanPreferencesKey("enable_volume_buttons_navigation")
    val MultiThreadedRendering = booleanPreferencesKey("multi_threaded_rendering")
    val LeakCanaryEnabled = booleanPreferencesKey("leak_canary_enabled")
}
