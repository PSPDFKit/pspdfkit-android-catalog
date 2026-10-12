/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.composeviewer

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.collection.LruCache
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.utils.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * The example-private preference store. Deliberately NOT the catalog's shared "settings" store —
 * keeping the example self-contained makes it copy-paste-able into a real app.
 */
private val Context.composeViewerDataStore: DataStore<Preferences> by preferencesDataStore(name = "compose_pdf_viewer")

/** One entry of the recents library. */
data class RecentDocument(val path: String, val name: String, val lastOpenedMillis: Long)

/** Dark-mode setting values (a tri-state, so "follow the system" stays expressible). */
object ViewerDarkMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}

/**
 * The viewer's single state holder: the recents library, the theme settings, page-1 thumbnails,
 * and file import — a deliberately dependency-free replacement for the `simple-compose` app's
 * Koin + Room stack. Recents persist as a JSON array in DataStore preferences (the same pattern
 * `PersistentTabsExample` uses); thumbnails render through [PdfDocumentLoader] behind an
 * in-memory [LruCache].
 */
class ComposeViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore get() = getApplication<Application>().composeViewerDataStore

    /** The recents library, newest first. */
    val recents: StateFlow<List<RecentDocument>> = dataStore.data
        .map { preferences -> parseRecents(preferences[RECENTS_KEY]).sortedByDescending { it.lastOpenedMillis } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The dark-mode setting — one of [ViewerDarkMode]. */
    val darkMode: StateFlow<Int> = dataStore.data
        .map { it[DARK_MODE_KEY] ?: ViewerDarkMode.SYSTEM }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ViewerDarkMode.SYSTEM)

    /**
     * Whether Material You dynamic color is enabled (only honored on API 31+). Off by default so
     * the viewer opens in the catalog's own palette like every other example; the Settings screen
     * turns it on.
     */
    val dynamicColor: StateFlow<Boolean> = dataStore.data
        .map { it[DYNAMIC_COLOR_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setDarkMode(mode: Int) {
        viewModelScope.launch { dataStore.edit { it[DARK_MODE_KEY] = mode } }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled } }
    }

    /** Page-1 thumbnails, keyed by file path and sized in kilobytes (an eighth of max memory). */
    private val thumbnailCache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    /** Renders (or returns the cached) page-1 thumbnail for [path], or `null` when it fails. */
    suspend fun thumbnail(path: String): Bitmap? = thumbnailCache[path] ?: withContext(Dispatchers.IO) {
        runCatching {
            val document = PdfDocumentLoader.openDocument(getApplication(), Uri.fromFile(File(path)))
            val pageSize = document.getPageSize(0)
            // Fit the page into the cell space, keeping its aspect ratio.
            val available = Size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
            val ratio = if (pageSize.width > pageSize.height) {
                available.width / pageSize.width
            } else {
                available.height / pageSize.height
            }
            document.renderPageToBitmap(
                getApplication(),
                0,
                (pageSize.width * ratio).toInt(),
                (pageSize.height * ratio).toInt(),
            ).also { thumbnailCache.put(path, it) }
        }.getOrNull()
    }

    /**
     * Records that [file] was opened now — inserting it or refreshing its timestamp.
     *
     * [name] is the label shown on the library card. It defaults to the name already stored for
     * this path, then to the file name: an entry keeps the label it was inserted with, so
     * re-opening it from the library cannot overwrite a caller-supplied label with the on-disk
     * file name (which for the bundled demo document carries `ExtractAssetTask`'s hash suffix).
     */
    fun recordOpened(file: File, name: String? = null) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                val entries = parseRecents(preferences[RECENTS_KEY])
                val label = name ?: entries.firstOrNull { it.path == file.path }?.name ?: file.name
                val updated = entries
                    .filterNot { it.path == file.path }
                    .plus(RecentDocument(file.path, label, System.currentTimeMillis()))
                preferences[RECENTS_KEY] = encodeRecents(updated)
            }
        }
    }

    /**
     * Imports the SAF-picked [uri]: copies it into the example's private directory and records it
     * as a recent. [onResult] runs on the main thread with either the imported file or the failure
     * that stopped the import — a picker grant can be revoked and a provider can fail mid-stream,
     * so the caller has to be able to tell the user rather than appear to do nothing.
     */
    fun importDocument(uri: Uri, onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { copyToImportDirectory(uri) }
            result.onSuccess { recordOpened(it) }
            onResult(result)
        }
    }

    /** Copies [uri] into [importDirectory], removing a half-written file if the copy fails. */
    private fun copyToImportDirectory(uri: Uri): Result<File> {
        val resolver = getApplication<Application>().contentResolver
        val directory = importDirectory().apply { mkdirs() }
        val target = runCatching { importTarget(directory, uri) }.getOrElse { return Result.failure(it) }
        return runCatching {
            val input = resolver.openInputStream(uri)
                ?: error("The provider returned no data for the picked document.")
            input.use { source -> target.outputStream().use { source.copyTo(it) } }
            target
        }.onFailure { target.delete() }
    }

    /**
     * Resolves where [uri] is copied to. The provider's display name is only a starting point:
     * it is provider-controlled, so it is reduced to a bare file name and the resolved target is
     * verified to still sit directly inside [directory] — a name such as `../../databases/x`
     * would otherwise let an import overwrite unrelated app-private files.
     */
    private fun importTarget(directory: File, uri: Uri): File {
        val displayName = getApplication<Application>().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        val fileName = displayName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.takeUnless { it.isBlank() || it == "." || it == ".." }
            ?: "${UUID.randomUUID()}.pdf"
        val target = File(directory, fileName)
        check(target.canonicalFile.parentFile == directory.canonicalFile) {
            "Refusing to import \"$fileName\": it resolves outside of ${directory.name}."
        }
        return target
    }

    /**
     * Deletes every *imported* file and drops its recents entry. The bundled demo document stays:
     * it was never imported, its file lives outside [importDirectory] and survives this, so
     * removing its entry would strand the library on an empty screen until the example is
     * relaunched.
     */
    fun clearImportedDocuments() {
        viewModelScope.launch {
            val directory = importDirectory()
            withContext(Dispatchers.IO) { directory.deleteRecursively() }
            // Only the gone documents' thumbnails, so the surviving cards keep theirs instead of
            // flashing a spinner while they re-render.
            thumbnailCache.snapshot().keys
                .filter { File(it).startsWith(directory) }
                .forEach { thumbnailCache.remove(it) }
            dataStore.edit { preferences ->
                val kept = parseRecents(preferences[RECENTS_KEY]).filterNot { File(it.path).startsWith(directory) }
                preferences[RECENTS_KEY] = encodeRecents(kept)
            }
        }
    }

    private fun importDirectory() = File(getApplication<Application>().filesDir, IMPORT_DIR_NAME)

    /**
     * The recents wire format: a JSON array of `{path, name, lastOpened}` objects. Plain
     * `org.json` on purpose — the catalog build has no serialization plugin, and the payload is
     * three fields.
     */
    private fun parseRecents(json: String?): List<RecentDocument> {
        if (json.isNullOrEmpty()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                val entry = array.optJSONObject(index) ?: return@mapNotNull null
                RecentDocument(
                    path = entry.optString("path"),
                    name = entry.optString("name"),
                    lastOpenedMillis = entry.optLong("lastOpened"),
                )
            }.filter { File(it.path).exists() }
        }.getOrDefault(emptyList())
    }

    private fun encodeRecents(entries: List<RecentDocument>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("path", entry.path)
                    .put("name", entry.name)
                    .put("lastOpened", entry.lastOpenedMillis),
            )
        }
        return array.toString()
    }

    private companion object {
        val RECENTS_KEY = stringPreferencesKey("recent_documents")
        val DARK_MODE_KEY = intPreferencesKey("dark_mode")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        const val IMPORT_DIR_NAME = "compose-viewer"
        const val THUMBNAIL_WIDTH = 200f
        const val THUMBNAIL_HEIGHT = 350f
    }
}
