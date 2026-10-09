package com.docscanner.sdk

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.docscanner.sdk.databinding.FragmentScanBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * Modern Camera Fragment using CameraX API
 * Displays camera preview with fixed crop overlay and captures cropped images
 */
class ScanFragment : Fragment() {
    
    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ScanViewModel
    private lateinit var cameraExecutor: ExecutorService
    
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[ScanViewModel::class.java]
        
        setupCamera()
        setupUI()
        applyOverlayOptions()
    }
    
    private fun setupUI() {
        binding.captureButton.setOnClickListener {
            capturePhoto()
        }
        
        binding.flashButton.setOnClickListener {
            toggleFlash()
        }
        
        // Show instruction
        showInstruction()
    }
    
    private fun setupCamera() {
        val previewView = binding.previewView
        // The provider is cached after the first scan, so this callback can run
        // before the new preview is attached and display is still null.
        previewView.doOnAttach {
            previewView.doOnLayout {
                if (!isAdded || _binding == null) return@doOnLayout
                val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
                cameraProviderFuture.addListener({
                    if (!isAdded || _binding == null) return@addListener
                    if (binding.previewView.display == null) {
                        binding.previewView.post {
                            if (isAdded && _binding != null) setupCamera()
                        }
                        return@addListener
                    }
                    bindCameraUseCases(cameraProviderFuture.get())
                }, ContextCompat.getMainExecutor(requireContext()))
            }
        }
    }
    
    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val previewView = _binding?.previewView ?: return
        val rotation = previewView.display?.rotation ?: return
        val viewPort = previewView.viewPort ?: previewView.getViewPort(rotation) ?: return

        this.cameraProvider = cameraProvider

        // Preview use case
        preview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Image capture use case with high quality. Same ViewPort as the preview so the
        // captured buffer's crop matches the area the user sees.
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(rotation)
            .build()

        val useCaseGroup = UseCaseGroup.Builder()
            .setViewPort(viewPort)
            .addUseCase(preview!!)
            .addUseCase(imageCapture!!)
            .build()
        
        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCaseGroup
            )
            setupTapToFocus()
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            showError("Failed to start camera")
        }
    }
    
    private fun setupTapToFocus() {
        binding.previewView.setOnTouchListener { _, event ->
            val meteringPointFactory = binding.previewView.meteringPointFactory
            val focusPoint = meteringPointFactory.createPoint(event.x, event.y)
            
            val action = FocusMeteringAction.Builder(focusPoint)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            camera?.cameraControl?.startFocusAndMetering(action)
            
            // Show focus indicator
            showFocusIndicator(event.x, event.y)
            
            true
        }
    }
    
    private fun showFocusIndicator(x: Float, y: Float) {
        // TODO: Show a circle animation at the tap location
    }
    
    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return
        
        binding.captureButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        // Read view geometry on the main thread. The capture callback runs on cameraExecutor.
        val cropFrame = overlayFrameInPreview()
        
        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val bitmap = imageProxyToBitmap(image)
                        if (bitmap == null) {
                            showError("Failed to process image")
                            enableCaptureButton()
                            return
                        }
                        val croppedBitmap = cropToOverlay(bitmap, cropFrame)
                        val path = writeJpeg(croppedBitmap)
                        deliverSavedImage(path)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving image", e)
                        showError("Failed to save image")
                        enableCaptureButton()
                    } finally {
                        image.close()
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exception)
                    showError("Failed to capture photo")
                    enableCaptureButton()
                }
            }
        )
    }
    
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val crop = image.cropRect
        val cropX = crop.left.coerceIn(0, decoded.width - 1)
        val cropY = crop.top.coerceIn(0, decoded.height - 1)
        val cropWidth = crop.width().coerceIn(1, decoded.width - cropX)
        val cropHeight = crop.height().coerceIn(1, decoded.height - cropY)
        val cropped = if (cropX == 0 && cropY == 0 && cropWidth == decoded.width && cropHeight == decoded.height) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, cropX, cropY, cropWidth, cropHeight)
        }

        return rotateBitmap(cropped, image.imageInfo.rotationDegrees)
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        
        return Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matrix, true
        )
    }
    
    /**
     * White frame in [PreviewView] coordinates. [OverlayView] draws the stroke inset
     * inside its own bounds, and the preview is scaled with fillCenter.
     */
    private fun overlayFrameInPreview(): CropFrame {
        val preview = binding.previewView
        val overlay = binding.overlayView
        val previewLoc = IntArray(2)
        val overlayLoc = IntArray(2)
        preview.getLocationOnScreen(previewLoc)
        overlay.getLocationOnScreen(overlayLoc)
        val originX = overlayLoc[0] - previewLoc[0]
        val originY = overlayLoc[1] - previewLoc[1]
        val local = overlay.getCropRect()
        val left: Float
        val top: Float
        val right: Float
        val bottom: Float
        if (local.width() > 1f && local.height() > 1f) {
            left = originX + local.left
            top = originY + local.top
            right = originX + local.right
            bottom = originY + local.bottom
        } else {
            left = originX.toFloat()
            top = originY.toFloat()
            right = left + overlay.width
            bottom = top + overlay.height
        }
        return CropFrame(left, top, right, bottom, preview.width, preview.height)
    }

    private fun cropToOverlay(originalBitmap: Bitmap, frame: CropFrame): Bitmap {
        val viewW = frame.previewWidth.coerceAtLeast(1).toFloat()
        val viewH = frame.previewHeight.coerceAtLeast(1).toFloat()
        val bitmapW = originalBitmap.width.toFloat()
        val bitmapH = originalBitmap.height.toFloat()

        // Match PreviewView fillCenter: one scale that covers the view, extra image is cropped.
        val scale = max(viewW / bitmapW, viewH / bitmapH)
        val offsetX = (bitmapW * scale - viewW) / 2f
        val offsetY = (bitmapH * scale - viewH) / 2f

        val left = ((frame.left + offsetX) / scale).toInt()
        val top = ((frame.top + offsetY) / scale).toInt()
        val right = ((frame.right + offsetX) / scale).toInt()
        val bottom = ((frame.bottom + offsetY) / scale).toInt()

        val safeCropX = left.coerceIn(0, originalBitmap.width - 1)
        val safeCropY = top.coerceIn(0, originalBitmap.height - 1)
        val safeCropWidth = (right - safeCropX).coerceIn(1, originalBitmap.width - safeCropX)
        val safeCropHeight = (bottom - safeCropY).coerceIn(1, originalBitmap.height - safeCropY)

        return Bitmap.createBitmap(
            originalBitmap,
            safeCropX,
            safeCropY,
            safeCropWidth,
            safeCropHeight
        )
    }
    
    private fun writeJpeg(bitmap: Bitmap): String {
        val file = createImageFile()
        val quality = viewModel.options.jpegQuality.coerceIn(1, 100)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return file.absolutePath
    }

    /** LiveData.setValue and view updates must run on the main thread. */
    private fun deliverSavedImage(path: String) {
        val host = activity ?: return
        host.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            when (viewModel.scanMode.value) {
                ScanMode.FRONT -> {
                    viewModel.setFrontImagePath(path)
                    if (viewModel.needsBackScan()) {
                        viewModel.setScanMode(ScanMode.BACK)
                        if (_binding != null) {
                            showInstruction()
                            enableCaptureButton()
                        }
                    } else {
                        finishScanning()
                    }
                }
                ScanMode.BACK -> {
                    viewModel.setBackImagePath(path)
                    finishScanning()
                }
                ScanMode.SINGLE, null -> {
                    viewModel.setFrontImagePath(path)
                    finishScanning()
                }
            }
        }
    }
    
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "DOC_${timeStamp}.jpg"
        val storageDir = requireContext().cacheDir
        return File(storageDir, fileName)
    }
    
    private fun toggleFlash() {
        camera?.let {
            val currentMode = it.cameraInfo.torchState.value
            it.cameraControl.enableTorch(currentMode != TorchState.ON)
            
            // Update flash button icon
            updateFlashButton()
        }
    }
    
    private fun updateFlashButton() {
        camera?.let {
            val isOn = it.cameraInfo.torchState.value == TorchState.ON
            binding.flashButton.setImageResource(
                if (isOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
            )
        }
    }
    
    private fun showInstruction() {
        val message = when (viewModel.scanMode.value) {
            ScanMode.FRONT -> "Position the front of the document within the white frame"
            ScanMode.BACK -> "Position the back of the document within the white frame"
            else -> "Position the document within the white frame"
        }
        binding.instructionText.text = message
    }

    private fun applyOverlayOptions() {
        val opts = viewModel.options
        val density = resources.displayMetrics.density
        binding.overlayView.visibility =
            if (opts.showCropOverlay) android.view.View.VISIBLE else android.view.View.GONE
        binding.overlayView.borderColor = opts.overlayBorderColor
        binding.overlayView.borderWidth = opts.overlayBorderWidthDp * density
        binding.overlayView.cornerRadius = opts.overlayCornerRadiusDp * density
        if (opts.flashEnabled) {
            camera?.cameraControl?.enableTorch(true)
        }
    }
    
    private fun finishScanning() {
        (requireActivity() as? DocScannerActivity)?.deliverResultAndFinish()
            ?: run {
                requireActivity().setResult(android.app.Activity.RESULT_OK)
                requireActivity().finish()
            }
    }
    
    private fun showError(message: String) {
        requireActivity().runOnUiThread {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun enableCaptureButton() {
        requireActivity().runOnUiThread {
            binding.captureButton.isEnabled = true
            binding.progressBar.visibility = View.GONE
        }
    }
    
    override fun onDestroyView() {
        _binding = null
        cameraProvider?.unbindAll()
        camera = null
        imageCapture = null
        preview = null
        super.onDestroyView()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    
    private data class CropFrame(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val previewWidth: Int,
        val previewHeight: Int
    )

    companion object {
        private const val TAG = "ScanFragment"
    }
}
