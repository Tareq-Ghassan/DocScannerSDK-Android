# DocScanner Android Example

Demonstrates launching `DocScannerActivity` from the host app.

The **white crop rectangle**, camera preview, and crop-on-capture all live in
`DocScannerSDK-Android` (`../docscanner-sdk`). This example only starts the
activity. Tap the front or back photo to scan that side, then the cropped
image is shown in the slot that was tapped.

```bash
cd example
./gradlew :app:assembleDebug
```
