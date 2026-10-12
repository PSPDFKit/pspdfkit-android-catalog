/*
 *   Copyright © 2020-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.UiThread
import androidx.core.net.toUri
import com.pspdfkit.annotations.SoundAnnotation
import com.pspdfkit.annotations.sound.AudioExtractor
import com.pspdfkit.annotations.sound.WavWriter
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.compose.toolbar.state.ToolbarItem.ItemEdge
import com.pspdfkit.compose.toolbar.state.ToolbarItem.OverflowBehavior
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.preferences.PSPDFKitPreferences
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * An example on how to create a sound annotation programmatically and extract the audio data from it.
 */
class SoundAnnotationDataExtractionExample(context: Context) :
    SdkExample(context, R.string.soundAnnotationDataExtractionTitle, R.string.soundAnnotationDataExtractionDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Turn off saving, so we have the clean original document every time the example is launched.
        configuration.autosaveEnabled(false)

        configuration.enabledAnnotationTools(
            listOf(
                AnnotationTool.SOUND,
            ),
        )

        // The annotation creator written into newly created annotations. If not set, or set to `null`
        // a dialog will normally be shown when creating an annotation, asking the user to enter a name.
        // We are going to skip this part and set it as "John Doe" only if it was not yet set.
        if (!PSPDFKitPreferences.get(context).isAnnotationCreatorSet) {
            PSPDFKitPreferences.get(context).setAnnotationCreator("John Doe")
        }

        // Extract the document from the assets. The launched activity will add annotations to that document.
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent =
                PdfActivityIntentBuilder
                    .fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(SoundAnnotationDataExtractionActivity::class)
                    .build()
            context.startActivity(intent)
        }
    }
}

/**
 * This activity creates a sound annotation when the document is loaded.
 * It also adds a custom menu item to the toolbar that extracts the sound data from the annotation and saves it to a .wav file.
 */
class SoundAnnotationDataExtractionActivity : PdfActivity() {
    private val viewModel: AnnotationCreationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarItemsProvider { defaultItems ->
            defaultItems + ToolbarItem.Button(
                id = MENU_ID,
                actionId = "extract_sound_wav",
                contentDescription = "Extract sound .wav",
                position = ItemEdge.End,
                overflowBehavior = OverflowBehavior.Never,
            )
        }
        setOnToolbarItemClickListener { itemId ->
            when (itemId) {
                MENU_ID -> {
                    menuItemClicked()
                    true
                }

                else -> false
            }
        }
    }

    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        viewModel.createObjects {
            createSoundAnnotation()
        }
    }

    private fun menuItemClicked() {
        extractSoundData()
    }

    @SuppressLint("CheckResult")
    private fun createSoundAnnotation() {
        try {
            // Extract first audio track from sample in assets.
            // Audio extractor supports decoding audio tracks from all media formats that are supported by Android's `MediaExtractor`.
            val audioExtractor = AudioExtractor(this, "file:///android_asset/media/audioLoop.wav".toUri())
            audioExtractor.selectAudioTrack(0)
            audioExtractor.extractAudioTrackAsync().subscribe { embeddedAudioSource ->
                // Create new sound annotation from the extracted audio track.
                val soundAnnotation = SoundAnnotation(0, RectF(580f, 700f, 600f, 685f), embeddedAudioSource)
                requirePdfFragment().addAnnotationToPage(soundAnnotation, false)
            }
        } catch (e: IOException) {
            // Handle possible IOException, thrown when the Uri does not point to correct file/asset.
        }
    }

    private fun extractSoundData() {
        val annotations = runBlocking { document?.annotationProvider?.getAnnotations(0) } ?: return
        val soundAnnotation = annotations[0] as SoundAnnotation

        val outputFile = File.createTempFile("tmp_", "sound.wav")
        WavWriter.forAnnotation(soundAnnotation).writeToStream(FileOutputStream(outputFile))

        Toast.makeText(this, "Wav file saved to: ${outputFile.path}", Toast.LENGTH_SHORT).show()
        println("Wav: ${outputFile.path}")
    }

    companion object {
        private const val MENU_ID = 1
    }
}
