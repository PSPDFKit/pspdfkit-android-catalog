---
applyTo: "android/examples/catalog/**"
---

# catalog — Flagship SDK Demo App

Compose-based demo app (fragment SDK flavour + OCR), mirrored publicly to github.com/PSPDFKit/pspdfkit-android-catalog. Everything here is customer-visible unless kept in an internal-only location (see below). Adding/registering examples is covered in `../AGENTS.md`.

## App structure (`app/src/main/java/com/pspdfkit/catalog/`)

- `ui/MainActivity.kt` — single Compose activity (example list, search, settings); examples launch a normal `PdfActivity` via `PdfActivityIntentBuilder`. `ui/PhoneCatalog.kt` / `ui/TabletCatalog.kt` split by window width.
- `ui/CatalogViewModel.kt` + `ui/model/{State,Action,Preference,PreferenceKeys}.kt` — MVI-ish; preferences are DataStore-backed and converted into each example's default `PdfActivityConfiguration` by `model/getPdfActivityConfigurationBuilder`, so the settings screen affects every example.
- `examples/kotlin/` and `examples/java/` — the examples themselves; `utils/ExamplesListContent.kt` registers them; `SdkExample.kt` is the base class.
- `tasks/ExtractAssetTask.java` — assets (~34 PDFs under `app/src/main/assets/`) are always copied to internal storage before opening, never opened in place.
- Search is fuzzy matching via `utils/Matchable.kt` + `utils/String.ext.kt`.

## Constraints

- Every example must keep the `(Context, @StringRes title, @StringRes description)` constructor: docs generation scrapes the resource IDs, and CI's smoke run (`android/ci/scripts/examples_run.sh`) launches each example reflectively via a single-`Context` constructor.
- **No internal API usage**: `tools/validation/internal_check.sh` hard-fails on any mention of the SDK's internal packages — the `internal` subpackage of `com.pspdfkit` or `io.nutrient` — anywhere under `examples/`. It is a plain `grep -r`, so comments and Markdown count too; that is why this line does not spell the package names out.
- Keep the modified-BSD sample header on new files ("This notice may not be removed…").
- `keep:internal`/`keep:github`/`keep:packaged` marker blocks control what ships in the public copies — they exist in `settings.gradle.kts` too, not just `app/build.gradle.kts`.

## Internal-only bits (stripped from public copies)

- `app/internal.gradle` — license-key flavours (`full` = real key, `demo` = placeholder) and LeakCanary.
- `app/src/debug/` — debug-only diagnostics (heap-dumping exception handler).
- `app/pspdfcatalog_keystore.jks` — throwaway release keystore so internal release builds are signable; public copies sign release with the debug config instead.

## Tests

Only a small JVM test (`app/src/test/`, fuzzy-match logic) — no androidTest. The real check is CI's launch-every-example smoke run.
