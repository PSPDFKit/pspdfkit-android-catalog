/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities.viewmodels;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import com.pspdfkit.catalog.examples.java.FilterableThumbnailGridExample;
import com.pspdfkit.document.PdfDocument;

/**
 * {@link ViewModel} used for storing the pdf document in the {@link
 * FilterableThumbnailGridExample}.
 */
public class FilterableThumbnailGridViewModel extends ViewModel {
    @Nullable
    public PdfDocument pdfDocument = null;
}
