/*
 *   Copyright © 2018-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pspdfkit.document.providers.ContentResolverDataProvider
import com.pspdfkit.document.sharing.DocumentSharingProvider
import com.pspdfkit.document.sharing.DocumentSharingProviderProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * This activity showcases how to use [DocumentSharingProvider] for exposing shared images via
 * system clipboard.
 *
 * This activity handles image share intents (an `ACTION_SEND` intent filter with an `image/`
 * mime-type declared in AndroidManifest.xml).
 */
class CopyToClipboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val action = intent.action
        val type = intent.type
        if (action == Intent.ACTION_SEND && type != null && type.startsWith("image/")) {
            copySharedImageToSystemClipboard(intent)
        }
    }

    private fun copySharedImageToSystemClipboard(intent: Intent) {
        // We support only image files at the moment.
        val mimeType = intent.type
        if (mimeType == null || !mimeType.startsWith("image/")) {
            showMessageAndFinish("File is not an image.")
            return
        }

        // Extract image uri from share intent.
        val imageUri: Uri? =
            intent.data
                ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri

        if (imageUri == null) {
            showMessageAndFinish("Image could not be copied to clipboard.")
            return
        }

        // Show indeterminate progress dialog while the file is being prepared.
        val progressDialog = createProgressDialog("Copying image to clipboard...")
        progressDialog.show()

        // Use DocumentSharingProviderProcessor helper to prepare file for sharing in
        // DocumentSharingProvider.
        lifecycleScope.launch {
            val uri = try {
                DocumentSharingProviderProcessor.prepareFileForSharing(
                    this@CopyToClipboardActivity,
                    ContentResolverDataProvider(imageUri),
                    null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            progressDialog.dismiss()

            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboardManager != null && uri != null) {
                // Expose the share uri via system clipboard.
                val primaryClip = ClipData(null, arrayOf(mimeType), ClipData.Item(uri))
                try {
                    clipboardManager.setPrimaryClip(primaryClip)
                    showMessageAndFinish("Image copied to clipboard.")
                } catch (e: Exception) {
                    showMessageAndFinish("Image could not be copied to clipboard.")
                }
                return@launch
            }
            showMessageAndFinish("Image could not be copied to clipboard.")
        }
    }

    /** Builds an indeterminate progress dialog with a spinner and message. */
    private fun createProgressDialog(message: String): AlertDialog {
        val padding = (24 * resources.displayMetrics.density).toInt()

        val content = LinearLayout(this)
        content.orientation = LinearLayout.HORIZONTAL
        content.gravity = Gravity.CENTER_VERTICAL
        content.setPadding(padding, padding, padding, padding)

        val progressBar = ProgressBar(this)
        progressBar.isIndeterminate = true
        content.addView(progressBar)

        val messageView = TextView(this)
        messageView.text = message
        val messageParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        messageParams.leftMargin = padding
        messageView.layoutParams = messageParams
        content.addView(messageView)

        val dialog = AlertDialog.Builder(this)
            .setView(content)
            .setCancelable(false)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    private fun showMessageAndFinish(errorString: String) {
        Toast.makeText(this, errorString, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        /**
         * Controls whether this activity component is enabled. This setting will override enabled state
         * specified in manifest. Defaults to enabled.
         *
         * @param context Context to use.
         * @param isEnabled If `true`, handling for implicit intents for copying image to clipboard is
         *     enabled.
         */
        @JvmStatic
        fun setEnabled(context: Context, isEnabled: Boolean) {
            val packageManager = context.packageManager
            packageManager.setComponentEnabledSetting(
                ComponentName(context, CopyToClipboardActivity::class.java),
                if (isEnabled) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP,
            )
        }

        /**
         * Check whether this activity component is enabled.
         *
         * @param context Context to use.
         */
        @JvmStatic
        fun isEnabled(context: Context): Boolean {
            val packageManager = context.packageManager
            val componentEnabledSetting =
                packageManager.getComponentEnabledSetting(ComponentName(context, CopyToClipboardActivity::class.java))
            return componentEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
                componentEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
    }
}
