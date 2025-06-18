/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.kotlin.SignatureActivityContract.Companion.RESULT_EXTRA_SIGNATURE
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.TabBarHidingMode
import com.pspdfkit.signatures.Signature
import com.pspdfkit.signatures.Signature.Companion.createStampSignature
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * Shows how to build Signature object and pass between activities.
 * This use case demonstrated that Signature object can be properly written to and created from Parcel.
 */
class CustomSignatureParseExample(context: Context) : SdkExample(
    context,
    R.string.customSignatureParseExampleTitle,
    R.string.customSignatureParseExampleBody
) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        configuration.layout(R.layout.custom_example_signature_parse_activity)
        configuration.navigationButtonsEnabled(false)
        configuration.setTabBarHidingMode(TabBarHidingMode.HIDE)
        configuration.contentEditingEnabled(false)
        configuration.setMeasurementToolsEnabled(false)

        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(CustomSignatureParseActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}

class CustomSignatureParseActivity : PdfActivity() {

    private val addSignatureContract =
        registerForActivityResult(SignatureActivityContract()) { signature ->
            Log.d("CustomSignatureParseActivity", signature.toString())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<Button>(R.id.add_signature_button).setOnClickListener {
            addSignatureContract.launch("tag")
        }

        addSignatureContract.contract
    }
}

class SignatureActivityContract :
    ActivityResultContract<String, Signature?>() {

    override fun createIntent(
        context: Context,
        input: String
    ): Intent = Intent(context, ResultSignatureActivity::class.java)

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): Signature? = if (resultCode == RESULT_OK) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val signature = intent?.extras?.getParcelable(
                RESULT_EXTRA_SIGNATURE,
                Signature::class.java
            )
            signature?.let {
                println("Correctly parsed signature $signature")
            }
            signature
        } else {
            @Suppress("DEPRECATION")
            intent?.extras?.getParcelable(RESULT_EXTRA_SIGNATURE)
        }
    } else {
        null
    }

    companion object {
        const val RESULT_EXTRA_SIGNATURE = "SignatureActivityContract.RESULT_EXTRA_SIGNATURE"
    }
}

class ResultSignatureActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_example_parse_activity)

        findViewById<Button>(R.id.return_signature_button).setOnClickListener {
            val signatureBitmap = ContextCompat.getDrawable(
                this@ResultSignatureActivity,
                R.drawable.mock_page
            )?.toBitmap()!!

            val (width, height) = signatureBitmap.width to signatureBitmap.height
            val signature = createStampSignature(
                bitmap = signatureBitmap,
                stampRect = RectF(0f, 0f, width.toFloat(), height.toFloat()),
                biometricSignatureData = null,
                drawWidthRatio = 1f
            )

            setResult(RESULT_OK, Intent().putExtra(RESULT_EXTRA_SIGNATURE, signature))
            finish()
        }
    }
}
