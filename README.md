# DocScannerSDK-Android

Native Android document scanner with a **white crop rectangle** overlay.

Flutter apps should depend on this library via **JitPack** — do not copy Kotlin
sources into the Flutter plugin.

## Install (JitPack)

```gradle
repositories { maven { url 'https://jitpack.io' } }
dependencies {
    implementation 'com.github.Tareq-Ghassan:DocScannerSDK-Android:1.0.1'
}
```

## Quick start

```kotlin
val intent = DocScannerActivity.createIntent(
    context,
    ScanOptions(showCropOverlay = true, scanBothSides = false)
)
startActivityForResult(intent, REQ)
// Result extras:
// DocScannerSDK.EXTRA_FRONT_IMAGE_PATH
// DocScannerSDK.EXTRA_BACK_IMAGE_PATH
```

## How it works

1. `DocScannerActivity` opens CameraX preview
2. `OverlayView` draws a white rectangle (native UI)
3. On capture, `ScanFragment.cropToOverlay()` crops the bitmap to that frame
4. Cropped JPEG path is returned to the host

## Module layout

```
docscanner-sdk/   # library (publish to JitPack)
example/          # sample host app
```

## Release

```bash
git tag 1.0.1 && git push origin 1.0.1
```

Tagging creates a GitHub Release and triggers JitPack.

## License

MIT
