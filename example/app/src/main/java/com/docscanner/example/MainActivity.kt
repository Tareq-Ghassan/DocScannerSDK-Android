package com.docscanner.example

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.docscanner.sdk.DocScannerActivity
import com.docscanner.sdk.DocScannerSDK
import com.docscanner.sdk.ScanOptions

/**
 * Example host app. All camera / overlay / crop UI lives in DocScannerSDK-Android.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var resultImage: ImageView

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            statusText.text = "Scan cancelled"
            return@registerForActivityResult
        }
        val front = result.data?.getStringExtra(DocScannerSDK.EXTRA_FRONT_IMAGE_PATH)
        val back = result.data?.getStringExtra(DocScannerSDK.EXTRA_BACK_IMAGE_PATH)
        statusText.text = buildString {
            append("Front: ").append(front ?: "—")
            if (back != null) append("\nBack: ").append(back)
        }
        if (front != null) {
            resultImage.setImageBitmap(BitmapFactory.decodeFile(front))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        resultImage = findViewById(R.id.resultImage)
        findViewById<Button>(R.id.scanButton).setOnClickListener {
            val intent = DocScannerActivity.createIntent(
                this,
                ScanOptions(showCropOverlay = true, scanBothSides = false)
            )
            scannerLauncher.launch(intent)
        }
    }
}
