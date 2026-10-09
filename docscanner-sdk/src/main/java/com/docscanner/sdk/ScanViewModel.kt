package com.docscanner.sdk

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

enum class ScanMode {
    FRONT, BACK, SINGLE
}

/**
 * ViewModel for scan state and captured image paths.
 */
class ScanViewModel : ViewModel() {

    private val _frontImagePath = MutableLiveData<String?>()
    val frontImagePath: LiveData<String?> = _frontImagePath

    private val _backImagePath = MutableLiveData<String?>()
    val backImagePath: LiveData<String?> = _backImagePath

    private val _scanMode = MutableLiveData(ScanMode.FRONT)
    val scanMode: LiveData<ScanMode> = _scanMode

    private val _requiresBothSides = MutableLiveData(false)
    val requiresBothSides: LiveData<Boolean> = _requiresBothSides

    var options: ScanOptions = ScanOptions()
        private set

    /** When true, temporary files are kept for the Activity result consumer. */
    var retainFiles: Boolean = false

    fun applyOptions(options: ScanOptions) {
        this.options = options
        _requiresBothSides.value = options.scanBothSides
        _scanMode.value = if (options.scanBothSides) ScanMode.FRONT else ScanMode.SINGLE
    }

    fun setFrontImagePath(path: String) {
        _frontImagePath.value = path
    }

    fun setBackImagePath(path: String) {
        _backImagePath.value = path
    }

    fun setScanMode(mode: ScanMode) {
        _scanMode.value = mode
    }

    fun needsBackScan(): Boolean {
        return _requiresBothSides.value == true && _backImagePath.value == null
    }

    fun cleanup() {
        if (retainFiles) return
        _frontImagePath.value?.let { runCatching { File(it).delete() } }
        _backImagePath.value?.let { runCatching { File(it).delete() } }
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
