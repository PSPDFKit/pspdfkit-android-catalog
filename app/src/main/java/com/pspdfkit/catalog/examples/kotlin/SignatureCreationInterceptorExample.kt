/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.signatures.SignatureCreationInterceptor
import com.pspdfkit.ui.signatures.SignatureCreationRequest
import com.pspdfkit.ui.signatures.SignaturePlacementRequest
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import java.io.File
import kotlin.math.abs

// A document with a signature form field, so both entry points can be tried: the signature tool in
// the toolbar and tapping the field.
private const val FORM_DOC = "Form_example.pdf"

/**
 * Shows how to step into the built-in signature flow without replacing its UI.
 *
 * Before the signature dialog opens, a small sheet of our own asks for stroke thickness and
 * opacity, and the dialog is then shown with those values. After the signature is created, a second
 * prompt lets the user decide where on the page it goes. The values chosen last time are remembered,
 * which is all it takes to persist the user's preferred appearance.
 */
class SignatureCreationInterceptorExample(context: Context) :
    SdkExample(
        context,
        R.string.signatureCreationInterceptorExampleTitle,
        R.string.signatureCreationInterceptorExampleDescription,
    ) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(FORM_DOC, title, context) { documentFile: File ->
            // Only the signature tool, so the flow this example shows is one tap away.
            configuration.enabledAnnotationTools(listOf(AnnotationTool.SIGNATURE))
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(SignatureCreationInterceptorActivity::class.java)
                .build()
            context.startActivity(intent)
        }
    }
}

class SignatureCreationInterceptorActivity :
    PdfActivity(),
    SignatureCreationInterceptor {

    // Remembered across signatures: the appearance the user chose last time.
    private var lastThickness = 4f
    private var lastAlpha = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This is the whole integration.
        requirePdfFragment().signatureCreationInterceptor = this
    }

    override fun onSignatureCreationRequested(request: SignatureCreationRequest) {
        val thicknessLabel = TextView(this)
        val thicknessSeekBar = SeekBar(this).apply {
            max = 19 // 1pt … 20pt
            progress = lastThickness.toInt() - 1
        }
        val alphaLabel = TextView(this)
        val alphaSeekBar = SeekBar(this).apply {
            max = 9 // 10% … 100%
            progress = (lastAlpha * 10).toInt() - 1
        }
        fun refreshLabels() {
            thicknessLabel.text = getString(R.string.signatureCreationInterceptorThickness, thicknessSeekBar.progress + 1)
            alphaLabel.text = getString(R.string.signatureCreationInterceptorOpacity, (alphaSeekBar.progress + 1) * 10)
        }
        val onChange = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) = refreshLabels()
            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        }
        thicknessSeekBar.setOnSeekBarChangeListener(onChange)
        alphaSeekBar.setOnSeekBarChangeListener(onChange)
        refreshLabels()

        val padding = (16 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, 0)
            listOf(thicknessLabel, thicknessSeekBar, alphaLabel, alphaSeekBar).forEach {
                addView(it, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.signatureCreationInterceptorAppearanceTitle)
            .setView(content)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lastThickness = (thicknessSeekBar.progress + 1).toFloat()
                lastAlpha = (alphaSeekBar.progress + 1) / 10f
                // Show the built-in dialog with the chosen appearance.
                request.proceed(request.options.copy(defaultThickness = lastThickness, defaultAlpha = lastAlpha))
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> request.cancel() }
            .setOnCancelListener { request.cancel() }
            .show()
    }

    override fun onSignaturePlacementRequested(request: SignaturePlacementRequest) {
        // A form field has a fixed location, so there is nothing to ask.
        if (request.formElement != null) return request.proceed()

        val pageSize = requirePdfFragment().document?.getPageSize(request.pageIndex) ?: return request.proceed()
        val box = request.boundingBox
        // PDF coordinates: the origin is bottom-left and `top` is the larger y value, which makes
        // `RectF.height()` negative — hence the absolute value.
        val boxWidth = box.width()
        val boxHeight = abs(box.height())
        val margin = 24f
        val choices = arrayOf(
            getString(R.string.signatureCreationInterceptorPlaceProposed),
            getString(R.string.signatureCreationInterceptorPlaceBottomRight),
            getString(R.string.signatureCreationInterceptorPlaceTopLeft),
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.signatureCreationInterceptorPlacementTitle)
            .setItems(choices) { _, which ->
                when (which) {
                    1 -> request.proceed(
                        request.pageIndex,
                        RectF(
                            pageSize.width - boxWidth - margin,
                            margin + boxHeight,
                            pageSize.width - margin,
                            margin,
                        ),
                    )

                    2 -> request.proceed(
                        request.pageIndex,
                        RectF(
                            margin,
                            pageSize.height - margin,
                            margin + boxWidth,
                            pageSize.height - margin - boxHeight,
                        ),
                    )

                    else -> request.proceed()
                }
            }
            .setOnCancelListener { request.cancel() }
            .show()
    }
}
