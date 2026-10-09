package com.docscanner.example

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.docscanner.sdk.DocScannerActivity
import com.docscanner.sdk.DocScannerSDK
import com.docscanner.sdk.ScanOptions

/**
 * Example host app. All camera / overlay / crop UI lives in DocScannerSDK-Android.
 * Each photo slot opens a single-sided scan and shows the cropped result.
 */

const val SHOW_CROP_OVERLAY = true

class MainActivity : AppCompatActivity() {

    private enum class ScanTarget { FRONT, BACK }

    private lateinit var resulFrontImage: ImageView
    private lateinit var resulBackImage: ImageView
    private var scanTarget = ScanTarget.FRONT

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val path = result.data?.getStringExtra(DocScannerSDK.EXTRA_FRONT_IMAGE_PATH) ?: return@registerForActivityResult
        val bitmap = BitmapFactory.decodeFile(path) ?: return@registerForActivityResult
        val target = if (scanTarget == ScanTarget.FRONT) resulFrontImage else resulBackImage
        target.setImageBitmap(bitmap)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySafeArea(findViewById(R.id.contentRoot))
        resulFrontImage = findViewById(R.id.resulFrontImage)
        resulBackImage = findViewById(R.id.resulBackImage)

        resulFrontImage.setOnClickListener { openScanner(ScanTarget.FRONT) }
        resulBackImage.setOnClickListener { openScanner(ScanTarget.BACK) }
    }

    /**
     * Keep the page below the status bar, display cutout, app bar, and navigation bar.
     * AppCompat adds the action bar height to the top inset when the window is edge-to-edge.
     */
    private fun applySafeArea(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                maxOf(bars.left, insets.systemWindowInsetLeft),
                maxOf(bars.top, insets.systemWindowInsetTop),
                maxOf(bars.right, insets.systemWindowInsetRight),
                maxOf(bars.bottom, insets.systemWindowInsetBottom)
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun openScanner(target: ScanTarget) {
        scanTarget = target
        val intent = DocScannerActivity.createIntent(
            this,
            ScanOptions(showCropOverlay = SHOW_CROP_OVERLAY, scanBothSides = false)
        )
        scannerLauncher.launch(intent)
    }
}
