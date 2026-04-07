/*
 *   Copyright © 2023-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.utils

import android.content.Context
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.java.AesEncryptedFileExample
import com.pspdfkit.catalog.examples.java.AnnotationOverlayExample
import com.pspdfkit.catalog.examples.java.AnnotationSelectionCustomizationExample
import com.pspdfkit.catalog.examples.java.CustomAnnotationCreationToolbarExample
import com.pspdfkit.catalog.examples.java.CustomAnnotationInspectorExample
import com.pspdfkit.catalog.examples.java.CustomDocumentDownloadExample
import com.pspdfkit.catalog.examples.java.CustomElectronicSignatureExample
import com.pspdfkit.catalog.examples.java.CustomFormHighlightColorExample
import com.pspdfkit.catalog.examples.java.CustomInlineSearchExample
import com.pspdfkit.catalog.examples.java.CustomPageTemplatesExample
import com.pspdfkit.catalog.examples.java.CustomSearchUiExample
import com.pspdfkit.catalog.examples.java.CustomShareDialogExample
import com.pspdfkit.catalog.examples.java.CustomSharingMenuExample
import com.pspdfkit.catalog.examples.java.CustomStampAnnotationsExample
import com.pspdfkit.catalog.examples.java.CustomToolbarIconGroupingExample
import com.pspdfkit.catalog.examples.java.DisabledAnnotationPropertyExample
import com.pspdfkit.catalog.examples.java.DocumentSharingExample
import com.pspdfkit.catalog.examples.java.DocumentSwitcherExample
import com.pspdfkit.catalog.examples.java.FormEditingInFragmentExample
import com.pspdfkit.catalog.examples.java.ScreenReaderExample
import com.pspdfkit.catalog.examples.java.SplitDocumentExample
import com.pspdfkit.catalog.examples.java.ToolbarsInFragmentExample
import com.pspdfkit.catalog.examples.java.VerticalScrollbarExample
import com.pspdfkit.catalog.examples.kotlin.AiAssistantComposeExample
import com.pspdfkit.catalog.examples.kotlin.AiAssistantMultiDocComposeExample
import com.pspdfkit.catalog.examples.kotlin.AiAssistantViewPagerExample
import com.pspdfkit.catalog.examples.kotlin.AnnotationConfigurationExample
import com.pspdfkit.catalog.examples.kotlin.AnnotationCreationExample
import com.pspdfkit.catalog.examples.kotlin.AnnotationFlagsExample
import com.pspdfkit.catalog.examples.kotlin.AnnotationOverlayVisibilityExample
import com.pspdfkit.catalog.examples.kotlin.AnnotationRenderingExample
import com.pspdfkit.catalog.examples.kotlin.AnnotationSelectionViewStylingExample
import com.pspdfkit.catalog.examples.kotlin.AnnotationWithAlphaCreationExample
import com.pspdfkit.catalog.examples.kotlin.BookmarkHighlightingExample
import com.pspdfkit.catalog.examples.kotlin.CombineElectronicSignaturesWithDigitalSigningExample
import com.pspdfkit.catalog.examples.kotlin.ComposeExampleApp
import com.pspdfkit.catalog.examples.kotlin.ConstructionExample
import com.pspdfkit.catalog.examples.kotlin.ConvertHtmlToPdfExample
import com.pspdfkit.catalog.examples.kotlin.CustomActionsExample
import com.pspdfkit.catalog.examples.kotlin.CustomAnnotationNoteHinterProviderExample
import com.pspdfkit.catalog.examples.kotlin.CustomApplicationPolicyExample
import com.pspdfkit.catalog.examples.kotlin.CustomDataProviderExample
import com.pspdfkit.catalog.examples.kotlin.CustomFragmentRuntimeConfigurationExample
import com.pspdfkit.catalog.examples.kotlin.CustomLayoutExample
import com.pspdfkit.catalog.examples.kotlin.CustomMainToolbarExample
import com.pspdfkit.catalog.examples.kotlin.CustomSearchUiComposeExample
import com.pspdfkit.catalog.examples.kotlin.CustomSignatureParseExample
import com.pspdfkit.catalog.examples.kotlin.DefaultExample
import com.pspdfkit.catalog.examples.kotlin.DigitalSignatureExample
import com.pspdfkit.catalog.examples.kotlin.DocumentComparisonExample
import com.pspdfkit.catalog.examples.kotlin.DocumentDownloadExample
import com.pspdfkit.catalog.examples.kotlin.DocumentFromCanvasExample
import com.pspdfkit.catalog.examples.kotlin.DocumentJsonExample
import com.pspdfkit.catalog.examples.kotlin.DocumentPagerExample
import com.pspdfkit.catalog.examples.kotlin.DocumentProcessingExample
import com.pspdfkit.catalog.examples.kotlin.DocumentScrollExample
import com.pspdfkit.catalog.examples.kotlin.DocumentTabsExample
import com.pspdfkit.catalog.examples.kotlin.DynamicMultimediaAnnotationExample
import com.pspdfkit.catalog.examples.kotlin.ELearningExample
import com.pspdfkit.catalog.examples.kotlin.ExternalDocumentExample
import com.pspdfkit.catalog.examples.kotlin.FileAnnotationCreationExample
import com.pspdfkit.catalog.examples.kotlin.FilterableThumbnailGridExample
import com.pspdfkit.catalog.examples.kotlin.FormClickInterceptExample
import com.pspdfkit.catalog.examples.kotlin.FormCreationExample
import com.pspdfkit.catalog.examples.kotlin.FormFillingExample
import com.pspdfkit.catalog.examples.kotlin.FormTextFieldSuggestionExample
import com.pspdfkit.catalog.examples.kotlin.FormsJavaScriptExample
import com.pspdfkit.catalog.examples.kotlin.GenerateReportExample
import com.pspdfkit.catalog.examples.kotlin.HideRevealAnnotationsCreationExample
import com.pspdfkit.catalog.examples.kotlin.ImageDocumentExample
import com.pspdfkit.catalog.examples.kotlin.IndexedFullTextSearchExample
import com.pspdfkit.catalog.examples.kotlin.InlineMediaExample
import com.pspdfkit.catalog.examples.kotlin.InstantJsonAttachmentExample
import com.pspdfkit.catalog.examples.kotlin.JavaScriptActionsExample
import com.pspdfkit.catalog.examples.kotlin.JavaScriptCalculatorExample
import com.pspdfkit.catalog.examples.kotlin.JavaScriptFormFillingExample
import com.pspdfkit.catalog.examples.kotlin.JetpackComposeExample
import com.pspdfkit.catalog.examples.kotlin.JetpackComposeImageExample
import com.pspdfkit.catalog.examples.kotlin.KioskExample
import com.pspdfkit.catalog.examples.kotlin.LongTermValidationAfterSigningExample
import com.pspdfkit.catalog.examples.kotlin.LongTermValidationExample
import com.pspdfkit.catalog.examples.kotlin.ManualSigningExample
import com.pspdfkit.catalog.examples.kotlin.MeasurementToolsExample
import com.pspdfkit.catalog.examples.kotlin.MergeDocumentsExample
import com.pspdfkit.catalog.examples.kotlin.NavHostExample
import com.pspdfkit.catalog.examples.kotlin.OcrExample
import com.pspdfkit.catalog.examples.kotlin.OutlineProviderExample
import com.pspdfkit.catalog.examples.kotlin.OverlayViewsExample
import com.pspdfkit.catalog.examples.kotlin.PasswordExample
import com.pspdfkit.catalog.examples.kotlin.PdfFromImageExample
import com.pspdfkit.catalog.examples.kotlin.PdfUiFragmentExample
import com.pspdfkit.catalog.examples.kotlin.PersistentAnnotationSidebarExample
import com.pspdfkit.catalog.examples.kotlin.PersistentTabsExample
import com.pspdfkit.catalog.examples.kotlin.PlaygroundExample
import com.pspdfkit.catalog.examples.kotlin.PopupToolbarCustomisationExample
import com.pspdfkit.catalog.examples.kotlin.ProgressProviderExample
import com.pspdfkit.catalog.examples.kotlin.ReaderViewExample
import com.pspdfkit.catalog.examples.kotlin.RemoteUrlExample
import com.pspdfkit.catalog.examples.kotlin.RuntimeConfigurationExample
import com.pspdfkit.catalog.examples.kotlin.ScientificPaperExample
import com.pspdfkit.catalog.examples.kotlin.SignatureStorageDatabaseExample
import com.pspdfkit.catalog.examples.kotlin.SimpleFragmentExample
import com.pspdfkit.catalog.examples.kotlin.SoundAnnotationDataExtractionExample
import com.pspdfkit.catalog.examples.kotlin.ThirdPartySigningExample
import com.pspdfkit.catalog.examples.kotlin.ThumbnailBarExample
import com.pspdfkit.catalog.examples.kotlin.TwoStepSigningExample
import com.pspdfkit.catalog.examples.kotlin.UserInterfaceViewModesExample
import com.pspdfkit.catalog.examples.kotlin.WatermarkExample
import com.pspdfkit.catalog.examples.kotlin.XfdfExample
import com.pspdfkit.catalog.examples.kotlin.ZoomExample
import com.pspdfkit.catalog.examples.kotlin.instant.InstantExample

fun getSectionsWithExamples(context: Context) = listOf(
    // Entry points for the SDK — "I want to show a PDF."
    SdkExample.Section(
        context.getString(R.string.example_section_getting_started),
        R.drawable.ic_basic,
        DefaultExample(context),
        PlaygroundExample(context),
        SimpleFragmentExample(context),
        PdfUiFragmentExample(context),
    ),
    // "How do I load a PDF from different sources?"
    SdkExample.Section(
        context.getString(R.string.example_section_opening_documents),
        R.drawable.ic_opening_documents,
        ExternalDocumentExample(context),
        CustomDataProviderExample(context),
        DocumentDownloadExample(context),
        CustomDocumentDownloadExample(context),
        ProgressProviderExample(context),
        PasswordExample(context),
        AesEncryptedFileExample(context),
        ImageDocumentExample(context),
        RemoteUrlExample(context),
    ),
    // "How do I configure viewing, scrolling, zoom, and navigation?"
    SdkExample.Section(
        context.getString(R.string.example_section_viewing_navigation),
        R.drawable.ic_behaviour_customization,
        ScientificPaperExample(context),
        ZoomExample(context),
        RuntimeConfigurationExample(context),
        CustomFragmentRuntimeConfigurationExample(context),
        UserInterfaceViewModesExample(context),
        ReaderViewExample(context),
        OutlineProviderExample(context),
        DocumentScrollExample(context),
        ThumbnailBarExample(context),
        BookmarkHighlightingExample(context),
    ),
    // "How do I show multiple documents — tabs, pagers, split view?"
    SdkExample.Section(
        context.getString(R.string.example_section_multi_document),
        R.drawable.ic_multi_document,
        DocumentTabsExample(context),
        PersistentTabsExample(context),
        DocumentPagerExample(context),
        DocumentSwitcherExample(context),
        SplitDocumentExample(context),
        ELearningExample(context),
    ),
    // "How do I create, configure, and manage annotations?"
    SdkExample.Section(
        context.getString(R.string.example_section_annotations),
        R.drawable.ic_annotations,
        AnnotationCreationExample(context),
        AnnotationWithAlphaCreationExample(context),
        FileAnnotationCreationExample(context),
        AnnotationRenderingExample(context),
        AnnotationConfigurationExample(context),
        AnnotationFlagsExample(context),
        AnnotationSelectionCustomizationExample(context),
        CustomAnnotationInspectorExample(context),
        CustomStampAnnotationsExample(context),
        AnnotationOverlayExample(context),
        AnnotationOverlayVisibilityExample(context),
        HideRevealAnnotationsCreationExample(context),
        SoundAnnotationDataExtractionExample(context),
        AnnotationSelectionViewStylingExample(context),
        CustomAnnotationNoteHinterProviderExample(context),
        DynamicMultimediaAnnotationExample(context),
        MeasurementToolsExample(context),
    ),
    // "How do I serialize/deserialize annotations?"
    SdkExample.Section(
        context.getString(R.string.example_section_annotation_import_export),
        R.drawable.ic_import_export,
        DocumentJsonExample(context),
        InstantJsonAttachmentExample(context),
        XfdfExample(context),
    ),
    // "How do I fill and create forms?"
    SdkExample.Section(
        context.getString(R.string.example_section_forms),
        R.drawable.ic_forms,
        FormFillingExample(context),
        FormCreationExample(context),
        FormClickInterceptExample(context),
        CustomFormHighlightColorExample(context),
        FormTextFieldSuggestionExample(context),
    ),
    // "How do I sign documents — digitally or electronically?"
    SdkExample.Section(
        context.getString(R.string.example_section_signatures),
        R.drawable.ic_signing,
        DigitalSignatureExample(context),
        ManualSigningExample(context),
        TwoStepSigningExample(context),
        ThirdPartySigningExample(context),
        LongTermValidationExample(context),
        LongTermValidationAfterSigningExample(context),
        CombineElectronicSignaturesWithDigitalSigningExample(context),
        CustomElectronicSignatureExample(context),
        SignatureStorageDatabaseExample(context),
        CustomSignatureParseExample(context),
    ),
    // "How do I customize toolbars, menus, and popup actions?"
    SdkExample.Section(
        context.getString(R.string.example_section_toolbars_menus),
        R.drawable.ic_toolbar_customization,
        CustomActionsExample(context),
        CustomMainToolbarExample(context),
        PopupToolbarCustomisationExample(context),
        CustomToolbarIconGroupingExample(context),
        CustomAnnotationCreationToolbarExample(context),
        DisabledAnnotationPropertyExample(context),
    ),
    // "How do I integrate surrounding UI, overlays, and accessibility features?"
    SdkExample.Section(
        context.getString(R.string.example_section_layout_overlays_accessibility),
        R.drawable.ic_layout_customization,
        CustomLayoutExample(context),
        ToolbarsInFragmentExample(context),
        FormEditingInFragmentExample(context),
        VerticalScrollbarExample(context),
        PersistentAnnotationSidebarExample(context),
        FilterableThumbnailGridExample(context),
        CustomApplicationPolicyExample(context),
        WatermarkExample(context),
        OverlayViewsExample(context),
        ScreenReaderExample(context),
        InlineMediaExample(context),
    ),
    // "How do I process, merge, convert, or create documents?"
    SdkExample.Section(
        context.getString(R.string.example_section_document_processing),
        R.drawable.ic_document_processing,
        DocumentProcessingExample(context),
        DocumentFromCanvasExample(context),
        OcrExample(context),
        GenerateReportExample(context),
        MergeDocumentsExample(context),
        DocumentComparisonExample(context),
        ConvertHtmlToPdfExample(context),
        CustomPageTemplatesExample(context),
        PdfFromImageExample(context),
    ),
    // "How do I add custom search experiences?"
    SdkExample.Section(
        context.getString(R.string.example_section_search),
        R.drawable.ic_topbar_search,
        CustomInlineSearchExample(context),
        CustomSearchUiExample(context),
        CustomSearchUiComposeExample(context),
        IndexedFullTextSearchExample(context),
    ),
    // "How do I share documents and expose custom share actions?"
    SdkExample.Section(
        context.getString(R.string.example_section_sharing),
        R.drawable.ic_sharing,
        DocumentSharingExample(context),
        CustomSharingMenuExample(context),
        CustomShareDialogExample(context),
    ),
    SdkExample.Section(
        context.getString(R.string.example_section_javascript),
        R.drawable.ic_javascript,
        JavaScriptActionsExample(context),
        JavaScriptFormFillingExample(context),
        FormsJavaScriptExample(context),
        JavaScriptCalculatorExample(context),
    ),
    SdkExample.Section(
        context.getString(R.string.example_section_ai_assistant),
        R.drawable.ic_ai_assistant,
        AiAssistantComposeExample(context),
        AiAssistantMultiDocComposeExample(context),
        AiAssistantViewPagerExample(context),
    ),
    // Real-time collaboration with Nutrient Instant
    SdkExample.Section(
        context.getString(R.string.example_section_collaboration),
        R.drawable.ic_collaborate,
        InstantExample(context),
    ),
    // Compose integration basics. For a full Compose app, see the simple-compose example.
    SdkExample.Section(
        context.getString(R.string.example_section_jc_examples),
        R.drawable.ic_jetpack_compose,
        ComposeExampleApp(context),
        JetpackComposeExample(context),
        JetpackComposeImageExample(context),
        NavHostExample(context),
    ),
    SdkExample.Section(
        context.getString(R.string.example_section_industry),
        R.drawable.ic_industry,
        KioskExample(context),
        ConstructionExample(context),
    ),
)
