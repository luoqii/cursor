package com.example.photocapture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.photocapture.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private var currentPhotoFile: File? = null
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerLaunchers()

        binding.captureButton.setOnClickListener {
            ensureCameraPermissionAndCapture()
        }
    }

    private fun registerLaunchers() {
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoFile?.let { file ->
                    displayPhoto(FileProvider.getUriForFile(
                        this,
                        FILE_PROVIDER_AUTHORITY,
                        file
                    ))
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun ensureCameraPermissionAndCapture() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> launchCamera()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationale()
            }

            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.camera_permission_explanation)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.camera_permission_explanation)
            .setPositiveButton(R.string.open_settings) { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun launchCamera() {
        val photoFile = createImageFile()
        currentPhotoFile = photoFile
        val uri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, photoFile)
        takePictureLauncher.launch(uri)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(getExternalFilesDir(null), "images").apply { mkdirs() }
        return File.createTempFile("JPEG_${'$'}timeStamp_", ".jpg", storageDir)
    }

    private fun displayPhoto(uri: Uri) {
        val bitmap = loadBitmapFromUri(uri)
        if (bitmap == null) {
            Toast.makeText(this, R.string.error_loading_photo, Toast.LENGTH_SHORT).show()
            return
        }
        binding.photoPreview.setImageBitmap(bitmap)
        val formattedTime = timestampFormat.format(Date())
        binding.photoTimestampText.text = getString(R.string.last_photo_time, formattedTime)
        binding.captureButton.setText(R.string.retake_photo)
        binding.photoPreview.contentDescription = formattedTime
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "com.example.photocapture.fileprovider"
    }
}
