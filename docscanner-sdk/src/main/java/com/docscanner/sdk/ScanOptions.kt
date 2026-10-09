package com.docscanner.sdk

import android.graphics.Color
import android.os.Bundle
import java.io.Serializable

/**
 * Options for the native document scanner UI.
 *
 * Overlay geometry is drawn by [OverlayView] inside the SDK — not by the host app.
 */
data class ScanOptions(
    val showCropOverlay: Boolean = true,
    val overlayBorderColor: Int = Color.WHITE,
    val overlayBorderWidthDp: Float = 3f,
    val overlayCornerRadiusDp: Float = 12f,
    val overlayMarginHorizontalDp: Float = 32f,
    val overlayHeightDp: Float = 220f,
    val scanBothSides: Boolean = false,
    val jpegQuality: Int = 95,
    val flashEnabled: Boolean = false,
) : Serializable {

    fun toBundle(): Bundle = Bundle().apply {
        putBoolean(KEY_SHOW_OVERLAY, showCropOverlay)
        putInt(KEY_BORDER_COLOR, overlayBorderColor)
        putFloat(KEY_BORDER_WIDTH, overlayBorderWidthDp)
        putFloat(KEY_CORNER_RADIUS, overlayCornerRadiusDp)
        putFloat(KEY_MARGIN_H, overlayMarginHorizontalDp)
        putFloat(KEY_HEIGHT, overlayHeightDp)
        putBoolean(KEY_BOTH_SIDES, scanBothSides)
        putInt(KEY_QUALITY, jpegQuality)
        putBoolean(KEY_FLASH, flashEnabled)
    }

    companion object {
        private const val KEY_SHOW_OVERLAY = "showCropOverlay"
        private const val KEY_BORDER_COLOR = "overlayBorderColor"
        private const val KEY_BORDER_WIDTH = "overlayBorderWidthDp"
        private const val KEY_CORNER_RADIUS = "overlayCornerRadiusDp"
        private const val KEY_MARGIN_H = "overlayMarginHorizontalDp"
        private const val KEY_HEIGHT = "overlayHeightDp"
        private const val KEY_BOTH_SIDES = "scanBothSides"
        private const val KEY_QUALITY = "jpegQuality"
        private const val KEY_FLASH = "flashEnabled"

        fun fromBundle(bundle: Bundle?): ScanOptions {
            if (bundle == null) return ScanOptions()
            return ScanOptions(
                showCropOverlay = bundle.getBoolean(KEY_SHOW_OVERLAY, true),
                overlayBorderColor = bundle.getInt(KEY_BORDER_COLOR, Color.WHITE),
                overlayBorderWidthDp = bundle.getFloat(KEY_BORDER_WIDTH, 3f),
                overlayCornerRadiusDp = bundle.getFloat(KEY_CORNER_RADIUS, 12f),
                overlayMarginHorizontalDp = bundle.getFloat(KEY_MARGIN_H, 32f),
                overlayHeightDp = bundle.getFloat(KEY_HEIGHT, 220f),
                scanBothSides = bundle.getBoolean(KEY_BOTH_SIDES, false),
                jpegQuality = bundle.getInt(KEY_QUALITY, 95),
                flashEnabled = bundle.getBoolean(KEY_FLASH, false),
            )
        }

        @Suppress("DEPRECATION")
        fun fromIntentExtras(extras: Bundle?): ScanOptions {
            if (extras == null) return ScanOptions()
            val nested = extras.getBundle(DocScannerSDK.EXTRA_OPTIONS)
            if (nested != null) return fromBundle(nested)
            val both = extras.getBoolean(DocScannerSDK.EXTRA_SCAN_BOTH_SIDES, false)
            return ScanOptions(scanBothSides = both)
        }
    }
}
