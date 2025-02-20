/*
 *   Copyright © 2023-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.utils

import android.content.Context
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.java.AnnotationFlagsExample
import com.pspdfkit.catalog.examples.java.AnnotationOverlayExample
import com.pspdfkit.catalog.examples.java.AnnotationSelectionCustomizationExample
import com.pspdfkit.catalog.examples.java.AnnotationWithAlphaCreationExample
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
import com.pspdfkit.catalog.examples.java.DynamicMultimediaAnnotationExample
import com.pspdfkit.catalog.examples.java.FilterableThumbnailGridExample
import com.pspdfkit.catalog.examples.java.FormEditingInFragmentExample
import com.pspdfkit.catalog.examples.java.FormsJavaScriptExample
import com.pspdfkit.catalog.examples.java.IndexedFullTextSearchExample
import com.pspdfkit.catalog.examples.java.JavaScriptActionsExample
import com.pspdfkit.catalog.examples.java.JavaScriptFormFillingExample
import com.pspdfkit.catalog.examples.java.RandomDocumentReplacementExample
import com.pspdfkit.catalog.examples.java.ScreenReaderExample
import com.pspdfkit.catalog.examples.java.SplitDocumentExample
import com.pspdfkit.catalog.examples.java.ToolbarsInFragmentExample
import com.pspdfkit.catalog.examples.java.VerticalScrollbarExample
import com.pspdfkit.catalog.examples.java.XfdfExample
import com.pspdfkit.catalog.examples.java.decryption.AesEncryptedFileExample
import com.pspdfkit.catalog.examples.kotlin.AnnotationConfigurationExample
import com.pspdfkit.catalog.examples.kotlin.AnnotationCreationExample
import com.pspdfkit.catalog.examples.kotlin.AnnotationRenderingExample
import com.pspdfkit.catalog.examples.kotlin.AnnotationSelectionViewStylingExample
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
import com.pspdfkit.catalog.examples.kotlin.DarkThemeExample
import com.pspdfkit.catalog.examples.kotlin.DefaultExample
import com.pspdfkit.catalog.examples.kotlin.DigitalSignatureExample
import com.pspdfkit.catalog.examples.kotlin.DocumentComparisonExample
import com.pspdfkit.catalog.examples.kotlin.DocumentDownloadExample
import com.pspdfkit.catalog.examples.kotlin.DocumentFromCanvasExample
import com.pspdfkit.catalog.examples.kotlin.DocumentJsonExample
import com.pspdfkit.catalog.examples.kotlin.DocumentProcessingExample
import com.pspdfkit.catalog.examples.kotlin.DocumentScrollExample
import com.pspdfkit.catalog.examples.kotlin.DocumentTabsExample
import com.pspdfkit.catalog.examples.kotlin.ELearningExample
import com.pspdfkit.catalog.examples.kotlin.EmptyActivityExample
import com.pspdfkit.catalog.examples.kotlin.ExternalDocumentExample
import com.pspdfkit.catalog.examples.kotlin.FileAnnotationCreationExample
import com.pspdfkit.catalog.examples.kotlin.FormClickInterceptExample
import com.pspdfkit.catalog.examples.kotlin.FormCreationExample
import com.pspdfkit.catalog.examples.kotlin.FormFillingExample
import com.pspdfkit.catalog.examples.kotlin.FragmentExample
import com.pspdfkit.catalog.examples.kotlin.GenerateReportExample
import com.pspdfkit.catalog.examples.kotlin.HideRevealAnnotationsCreationExample
import com.pspdfkit.catalog.examples.kotlin.ImageDocumentExample
import com.pspdfkit.catalog.examples.kotlin.InlineMediaExample
import com.pspdfkit.catalog.examples.kotlin.InstantJsonAttachmentExample
import com.pspdfkit.catalog.examples.kotlin.JavaScriptCalculatorExample
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
import com.pspdfkit.catalog.examples.kotlin.ProgressProviderExample
import com.pspdfkit.catalog.examples.kotlin.ReaderViewExample
import com.pspdfkit.catalog.examples.kotlin.RemoteUrlExample
import com.pspdfkit.catalog.examples.kotlin.RotatePageExample
import com.pspdfkit.catalog.examples.kotlin.RuntimeConfigurationExample
import com.pspdfkit.catalog.examples.kotlin.ScientificPaperExample
import com.pspdfkit.catalog.examples.kotlin.SignatureStorageDatabaseExample
import com.pspdfkit.catalog.examples.kotlin.SimpleFragmentExample
import com.pspdfkit.catalog.examples.kotlin.SoundAnnotationDataExtractionExample
import com.pspdfkit.catalog.examples.kotlin.TextHighlightPopupCustomisationExample
import com.pspdfkit.catalog.examples.kotlin.UserInterfaceViewModesExample
import com.pspdfkit.catalog.examples.kotlin.ViewPager2Example
import com.pspdfkit.catalog.examples.kotlin.WatermarkExample
import com.pspdfkit.catalog.examples.kotlin.ZoomExample
import com.pspdfkit.catalog.examples.kotlin.customsearchuicompose.CustomSearchUiComposeExample
import com.pspdfkit.catalog.examples.kotlin.instant.InstantExample

fun getSectionsWithExamples(context: Context) = listOf(
    SdkExample.Section(
        context.getString(R.string.example_section_basic),
        R.drawable.ic_basic,
        DefaultExample(context),
        PlaygroundExample(context),
        SimpleFragmentExample(context),
        KioskExample(context),
        InstantExample(context),
        DocumentTabsExample(context),
        EmptyActivityExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_industry),
        R.drawable.ic_industry,
        ELearningExample(context),
        ConstructionExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_opening_documents),
        R.drawable.ic_opening_documents,
        ExternalDocumentExample(context),
        CustomDataProviderExample(context),
        DocumentDownloadExample(context),
        ProgressProviderExample(context),
        PasswordExample(context),
        AesEncryptedFileExample(context),
        ImageDocumentExample(context),
        RemoteUrlExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_behaviour_customization),
        R.drawable.ic_behaviour_customization,
        ScientificPaperExample(context),
        DarkThemeExample(context),
        ZoomExample(context),
        RuntimeConfigurationExample(context),
        CustomFragmentRuntimeConfigurationExample(context),
        UserInterfaceViewModesExample(context),
        CustomApplicationPolicyExample(context),
        CustomAnnotationNoteHinterProviderExample(context),
        RotatePageExample(context),
        OutlineProviderExample(context)
    ),

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
        DocumentJsonExample(context),
        InstantJsonAttachmentExample(context),
        XfdfExample(context),
        HideRevealAnnotationsCreationExample(context),
        SoundAnnotationDataExtractionExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_forms),
        R.drawable.ic_forms,
        FormFillingExample(context),
        FormCreationExample(context),
        FormClickInterceptExample(context),
        CustomFormHighlightColorExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_digital_signing),
        R.drawable.ic_digital_signatures,
        DigitalSignatureExample(context),
        ManualSigningExample(context),
        LongTermValidationExample(context),
        LongTermValidationAfterSigningExample(context),
        CombineElectronicSignaturesWithDigitalSigningExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_electronic_signatures),
        R.drawable.ic_signing,
        CombineElectronicSignaturesWithDigitalSigningExample(context),
        CustomElectronicSignatureExample(context),
        SignatureStorageDatabaseExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_components),
        R.drawable.ic_components,
        ReaderViewExample(context),
        MeasurementToolsExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_javascript),
        R.drawable.ic_javascript,
        JavaScriptActionsExample(context),
        JavaScriptFormFillingExample(context),
        FormsJavaScriptExample(context),
        JavaScriptCalculatorExample(context)
    ),

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
        PdfFromImageExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_layout_customization),
        R.drawable.ic_layout_customization,
        CustomLayoutExample(context),
        FragmentExample(context),
        ToolbarsInFragmentExample(context),
        FormEditingInFragmentExample(context),
        PdfUiFragmentExample(context),
        CustomDocumentDownloadExample(context),
        DocumentSwitcherExample(context),
        VerticalScrollbarExample(context),
        SplitDocumentExample(context),
        PersistentAnnotationSidebarExample(context),
        AnnotationSelectionViewStylingExample(context),
        DocumentScrollExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_toolbar_customization),
        R.drawable.ic_toolbar_customization,
        CustomActionsExample(context),
        CustomToolbarIconGroupingExample(context),
        CustomAnnotationCreationToolbarExample(context),
        CustomInlineSearchExample(context),
        CustomSearchUiExample(context),
        DisabledAnnotationPropertyExample(context),
        TextHighlightPopupCustomisationExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_sharing),
        R.drawable.ic_sharing,
        DocumentSharingExample(context),
        CustomSharingMenuExample(context),
        CustomShareDialogExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_jc_examples),
        R.drawable.ic_jetpack_compose,
        ComposeExampleApp(context),
        CustomSearchUiComposeExample(context),
        JetpackComposeExample(context),
        JetpackComposeImageExample(context),
        NavHostExample(context)
    ),

    SdkExample.Section(
        context.getString(R.string.example_section_misc_examples),
        R.drawable.ic_misc_examples,
        WatermarkExample(context),
        BookmarkHighlightingExample(context),
        OverlayViewsExample(context),
        PersistentTabsExample(context),
        FilterableThumbnailGridExample(context),
        ScreenReaderExample(context),
        IndexedFullTextSearchExample(context),
        InlineMediaExample(context),
        DynamicMultimediaAnnotationExample(context),
        RandomDocumentReplacementExample(context),
        ViewPager2Example(context)
    )
)
