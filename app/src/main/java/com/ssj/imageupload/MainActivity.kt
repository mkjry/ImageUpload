package com.ssj.imageupload

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var cameraPreview: TextureView
    private lateinit var captureButton: Button
    private lateinit var progressBar: ProgressBar
    private val viewModel: MainViewModel by viewModels()
    private lateinit var cameraExecutor: ExecutorService

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraPreview = findViewById(R.id.camera_preview)
        captureButton = findViewById(R.id.capture_button)
        progressBar = findViewById(R.id.progress_bar)

        // Executor 초기화
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)

        }
        captureButton.setOnClickListener {
            captureImage()
        }

        viewModel.uploadState.observe(this) { state ->
            when (state) {
                is UploadState.Loading -> {
                    progressBar.visibility = android.view.View.VISIBLE
                }
                is UploadState.Success -> {
                    progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Upload successful: ${state.response.message}", Toast.LENGTH_SHORT).show()
                }
                is UploadState.Error -> {
                    progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Upload failed: ${state.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider { request ->
                    val surface = Surface(cameraPreview.surfaceTexture)
                    request.provideSurface(surface, cameraExecutor) { }
                }
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))

        // TextureView가 준비될 때까지 기다림
        if (cameraPreview.isAvailable) {
            cameraPreview.surfaceTexture?.let { texture ->
                val surface = Surface(texture)
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider { request ->
                    request.provideSurface(surface, cameraExecutor) { }
                }
            }
        } else {
            cameraPreview.viewTreeObserver.addOnGlobalLayoutListener {
                if (cameraPreview.isAvailable) {
                    cameraPreview.surfaceTexture?.let { texture ->
                        val surface = Surface(texture)
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider { request ->
                            request.provideSurface(surface, cameraExecutor) { }
                        }
                    }
                }
            }
        }
    }

    private fun captureImage() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        viewModel.uploadImage(imageFile)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}