package com.docscanner.sdk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar

/**
 * Host activity for the native document scanner.
 *
 * Shows CameraX preview + white crop rectangle and returns cropped image paths.
 */
class DocScannerActivity : AppCompatActivity() {

    private lateinit var viewModel: ScanViewModel
    private lateinit var navController: NavController
    private lateinit var options: ScanOptions

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            navigateToScanner()
        } else {
            showPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doc_scanner)

        options = ScanOptions.fromIntentExtras(intent.extras)
        viewModel = ViewModelProvider(this)[ScanViewModel::class.java]
        viewModel.applyOptions(options)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) navigateToScanner()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun navigateToScanner() {
        if (navController.currentDestination?.id == R.id.scanFragment) return
        navController.navigate(R.id.scanFragment)
    }

    private fun showPermissionDenied() {
        Snackbar.make(
            findViewById(android.R.id.content),
            "Camera permission is required to scan documents",
            Snackbar.LENGTH_LONG
        ).show()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    /** Called by [ScanFragment] when scanning finishes. */
    fun deliverResultAndFinish() {
        val data = Intent().apply {
            putExtra(DocScannerSDK.EXTRA_FRONT_IMAGE_PATH, viewModel.frontImagePath.value)
            putExtra(DocScannerSDK.EXTRA_BACK_IMAGE_PATH, viewModel.backImagePath.value)
        }
        // Keep files for the caller — do not delete in onDestroy after success.
        viewModel.retainFiles = true
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    companion object {
        fun createIntent(context: Context, options: ScanOptions = ScanOptions()): Intent {
            return Intent(context, DocScannerActivity::class.java).apply {
                putExtra(DocScannerSDK.EXTRA_OPTIONS, options.toBundle())
                putExtra(DocScannerSDK.EXTRA_SCAN_BOTH_SIDES, options.scanBothSides)
            }
        }
    }
}
