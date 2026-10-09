package com.docscanner.sdk

/**
 * Version and constants for the Android DocScanner SDK.
 *
 * Native owns camera preview, white crop rectangle, and crop-on-capture.
 * Flutter wraps this library via JitPack — do not copy these sources into Flutter.
 */
object DocScannerSDK {
    const val VERSION = "1.0.0"

    const val EXTRA_OPTIONS = "com.docscanner.sdk.OPTIONS"
    const val EXTRA_SCAN_BOTH_SIDES = "com.docscanner.sdk.SCAN_BOTH_SIDES"
    const val EXTRA_FRONT_IMAGE_PATH = "com.docscanner.sdk.FRONT_IMAGE_PATH"
    const val EXTRA_BACK_IMAGE_PATH = "com.docscanner.sdk.BACK_IMAGE_PATH"
}
