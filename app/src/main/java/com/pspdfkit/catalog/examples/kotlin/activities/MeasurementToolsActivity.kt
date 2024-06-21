/*
 *   Copyright © 2022-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.activities

import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.UiThread
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationProvider
import com.pspdfkit.annotations.LineAnnotation
import com.pspdfkit.annotations.measurements.MeasurementPrecision
import com.pspdfkit.annotations.measurements.MeasurementValueConfiguration
import com.pspdfkit.annotations.measurements.Scale
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.utils.PdfLog

/**
 * This subclass of [PdfActivity] adds a listener for newly created and updated measurements, and reads existing measurements from the
 * document when it's loaded.
 */
class MeasurementToolsActivity : PdfActivity(), AnnotationProvider.OnAnnotationUpdatedListener {
    /** We hold a list of all the measurements created for this activity. */
    private val measurements: MutableList<Annotation> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // We can register interest in newly created annotations so we can easily pick up measurement information.
        pdfFragment?.addOnAnnotationUpdatedListener(this)
    }

    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        // If we know the scale of the page, we can create it programmatically.
        val pageScale = Scale(1f, Scale.UnitFrom.CM, 1f, Scale.UnitTo.CM)

        // We can add a new measurement config with new scale and floating point precision of the document pages used by the measurement
        // tool annotations using the measurementValueConfigurationEditor#add API.
        // This will set the scale and precision for future measurements.
        val measurementValueConfiguration = MeasurementValueConfiguration("Custom Scale", pageScale, MeasurementPrecision.EIGHTHS_INCH)
        pdfFragment?.measurementValueConfigurationEditor?.add(measurementValueConfiguration, false)

        // We can also make sure the new config is the selected one.
        pdfFragment?.setSelectedMeasurementValueConfiguration(measurementValueConfiguration)

        // All the above work can be done by selecting the fab, when measurement tool is selected in the UI.

        // All the measurement configuration can be retrieved dynamically as well with
        // measurementValueConfigurationEditor#measurementValueConfigurations API.
        listAllMeasurementConfigurations(pdfFragment?.measurementValueConfigurationEditor?.configurations)

        // Similarly you can remove any configuration as well using
        // measurementValueConfigurationEditor#remove API.

        // We can read the annotations to see if there are any measurements that we need to process...
        for (page in 0 until document.pageCount) {
            measurements.addAll(document.annotationProvider.getAnnotations(page).filter { it.isMeasurement })
        }
        // ... and process them.
        processAllMeasurements()

        // Let's create a distance measurement annotation programmatically.
        val distanceMeasurement = LineAnnotation(
            0, // Page number
            PointF(56.693f, 460f), // Start point.
            PointF(150f, 460f), // End point.
            pageScale, // We can specify the scale. Let's use the same one set on the document.
            MeasurementPrecision.THREE_DP // We can use a different precision for this measurement.
        )
        distanceMeasurement.color = Color.BLUE
        pdfFragment?.addAnnotationToPage(distanceMeasurement, false)
    }

    /** List up all measurement configurations in document. */
    private fun listAllMeasurementConfigurations(measurementValueConfigurations: List<MeasurementValueConfiguration>?) =
        measurementValueConfigurations?.forEach {
            PdfLog.i("MeasurementExample", "${it.getNameForDisplay(true)}")
        }

    /** Process all the measurements in our activity. */
    private fun processAllMeasurements() {
        measurements.forEach(::processMeasurement)
    }

    /** Process any measurements that are passed. */
    private fun processMeasurement(annotation: Annotation) {
        if (!annotation.isMeasurement) return
        val info = annotation.measurementInfo ?: return
        PdfLog.i(
            "MeasurementExample",
            "Processing measurement...\n" +
                "Type: ${info.measurementMode.name}; Raw value: ${info.value} ${info.scale.unitTo}; Label: ${info.label}"
        )
    }

    override fun onAnnotationCreated(annotation: Annotation) {
        if (!annotation.isMeasurement) return
        // We have a new measurement!

        // Add it to our set.
        measurements.add(annotation)

        // Let's get the measurement information and do something with it.
        val info = annotation.measurementInfo
        info?.let {
            val toastText = "Created ${it.measurementMode.name} measurement: ${it.label}"
            Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show()
        }

        // It's new, we should also process it...
        processMeasurement(annotation)
    }

    override fun onAnnotationUpdated(annotation: Annotation) {
        processMeasurement(annotation)
    }

    override fun onAnnotationRemoved(annotation: Annotation) {
        if (!annotation.isMeasurement) return
        // Remove it from our set.
        measurements.remove(annotation)
        PdfLog.i("MeasurementExample", "Removed ${annotation.type} measurement annotation.")
    }

    override fun onAnnotationZOrderChanged(pageIndex: Int, oldOrder: MutableList<Annotation>, newOrder: MutableList<Annotation>) {
        // Nothing to do in this example.
    }
}
