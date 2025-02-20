/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
@file:SuppressLint("UsingMaterialAndMaterial3Libraries")

package com.pspdfkit.catalog.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.pspdfkit.PSPDFKit
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.service.DownloadedFilesObserverService
import com.pspdfkit.catalog.ui.model.State
import com.pspdfkit.catalog.ui.model.getPdfActivityConfigurationBuilder
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.catalog.ui.theming.Dimens
import com.pspdfkit.catalog.utils.dataStore
import com.pspdfkit.document.download.DownloadJob
import com.pspdfkit.document.download.DownloadProgressFragment
import com.pspdfkit.document.download.DownloadRequest
import com.pspdfkit.ui.PdfActivityIntentBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.lang.reflect.Constructor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
class MainActivity : AppCompatActivity() {

    // Properties needed to handle permission requesting using coroutines
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionLauncherCallback: (Boolean) -> Unit

    private val viewModel: CatalogViewModel by viewModels {
        CatalogViewModel.Factory(application, dataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Setting the theme before onCreate
        setTheme(R.style.PSPDFCatalog_Theme)

        super.onCreate(savedInstanceState)

        storagePermissionLauncher = activityResultRegistry.register(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ActivityResultContracts.RequestPermission(),
            ::onFilePermission
        )

        setContent {
            CatalogTheme {
                @Suppress("DEPRECATION")
                window.statusBarColor = MaterialTheme.colors.primaryVariant.toArgb()
                val configuration = LocalConfiguration.current
                val isTablet = configuration.screenWidthDp > Dimens.tabletWidthCutout.value

                val state by viewModel.state.collectAsState(State())

                if (isTablet) {
                    TabletCatalog(state, viewModel::dispatch)
                } else {
                    PhoneCatalog(state, viewModel::dispatch)
                }
            }
        }

        // Launch specific example if launch example extra was provided.
        if (intent != null && intent.hasExtra(EXTRA_LAUNCH_EXAMPLE)) {
            // When launching examples directly, ensure that PSPDFKit has completed initialization.
            val sleepTimeMs = 5L
            val timeoutMs = 2000 // roughly 2 sec before crashing
            var loopCount = 0
            while (!PSPDFKit.isInitialized() && (sleepTimeMs * loopCount++ < timeoutMs)) {
                Thread.sleep(sleepTimeMs)
            }
            // Crash if timeout was was hit.
            PSPDFKit.ensureInitialized()
            val className = intent.getStringExtra(EXTRA_LAUNCH_EXAMPLE) ?: return
            launchExampleWithClassName(className)
        }
    }

    override fun onResume() {
        super.onResume()

        if (intent != null && intent.actionIsViewOrEdit()) {
            lifecycleScope.launch {
                // When opening local files outside of android's Storage Access Framework ask for
                // permissions to external storage.
                if (intent.data?.scheme == URI_SCHEME_FILE && !storagePermissionsAreGranted()) {
                    return@launch
                }

                // We already have read/write permissions to external storage or don't need them.
                showDocument(intent)
            }
        }

        DownloadedFilesObserverService.startService(this)
    }

    override fun onStop() {
        super.onStop()
        DownloadedFilesObserverService.stopService(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        storagePermissionLauncher.unregister()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        val callSuper = viewModel.backPressed()
        if (callSuper) {
            super.onBackPressed()
        }
    }

    private suspend fun storagePermissionsAreGranted() = suspendCoroutine<Boolean> {
        // On Android 6.0+ we ask for SD card access permission.
        // Since documents can be annotated we ask for write permission as well.
        if (checkStoragePermission()) {
            it.resume(true)
        } else {
            storagePermissionLauncherCallback = it::resume
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun onFilePermission(result: Boolean) {
        storagePermissionLauncherCallback(result)
    }

    private fun showDocument(intent: Intent) {
        val uri = intent.data ?: return

        // If the URI can be resolved to a local filesystem path, we can directly access it for
        // best performance.
        if (PSPDFKit.isLocalFileUri(this, uri)) {
            openDocumentAndFinishActivity(uri)
            return
        }

        // Find the DownloadProgressFragment for showing download progress, or create a new one.
        val downloadFragment = supportFragmentManager
            .findFragmentByTag(DOWNLOAD_PROGRESS_FRAGMENT) as? DownloadProgressFragment
            ?: tryCreateDownloadFragment(uri)

        if (downloadFragment == null) {
            showDownloadErrorAndFinishActivity()
            return
        }

        // Once the download is complete we launch the PdfActivity from the downloaded file.
        downloadFragment.job
            .setProgressListener(
                object : DownloadJob.ProgressListenerAdapter() {
                    override fun onComplete(output: File) {
                        openDocumentAndFinishActivity(Uri.fromFile(output))
                    }
                }
            )
    }

    private fun tryCreateDownloadFragment(uri: Uri): DownloadProgressFragment? =
        try {
            val request = DownloadRequest.Builder(this).uri(uri).build()
            val job = DownloadJob.startDownload(request)
            DownloadProgressFragment().apply {
                show(supportFragmentManager, DOWNLOAD_PROGRESS_FRAGMENT)
                setJob(job)
            }
        } catch (ex: Exception) {
            null
        }

    private fun showDownloadErrorAndFinishActivity() {
        AlertDialog.Builder(this)
            .setTitle("Download error")
            .setMessage("PSPDFKit could not download the PDF file from the given URL.")
            .setNeutralButton("Exit catalog app") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { finish() }
            .setCancelable(false)
            .show()
    }

    private fun openDocumentAndFinishActivity(uri: Uri) {
        val configuration = getPdfActivityConfigurationBuilder().build()
        val intent = PdfActivityIntentBuilder.fromUri(this, uri)
            .configuration(configuration)
            .build()
        startActivity(intent)
        finish()
    }

    /**
     * You can launch specific example by providing LAUNCH_EXAMPLE extra to launch intent. You'll
     * need to provide:
     *
     * <pre>-e LAUNCH_EXAMPLE [example_class_name]</pre>
     *
     * to either `adb shell am` or to Android Studio run configuration.
     *
     * For example, to start basic example:
     *
     * <pre>-e LAUNCH_EXAMPLE com.pspdfkit.catalog.examples.kotlin.BasicExample</pre>
     */
    private fun launchExampleWithClassName(exampleClassName: String) {
        try {
            val exampleClass = Class.forName(exampleClassName)
            if (SdkExample::class.java.isAssignableFrom(exampleClass)) {
                val constructor: Constructor<*> = exampleClass.getConstructor(Context::class.java)
                (constructor.newInstance(this) as SdkExample)
                    .launchExample(this, getPdfActivityConfigurationBuilder())
            } else {
                throw IllegalArgumentException(
                    "Example class " +
                        exampleClassName +
                        " must be assignable to SdkExample"
                )
            }
        } catch (ex: Throwable) {
            throw IllegalArgumentException(
                "Can't launch example with class name $exampleClassName: ${ex.message}",
                ex
            )
        }
    }

    private fun checkStoragePermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun Intent.actionIsViewOrEdit(): Boolean =
        action == Intent.ACTION_VIEW || action == Intent.ACTION_EDIT

    private fun getPdfActivityConfigurationBuilder() =
        viewModel.state.value.getPdfActivityConfigurationBuilder(this)

    companion object {
        private const val URI_SCHEME_FILE = "file"
        private const val DOWNLOAD_PROGRESS_FRAGMENT = "DownloadProgressFragment"

        /**
         * Optional extra with class name of the example that should be launched on when starting the
         * activity.
         */
        const val EXTRA_LAUNCH_EXAMPLE = "LAUNCH_EXAMPLE"
    }
}
