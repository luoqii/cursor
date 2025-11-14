package com.example.photocapture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.photocapture.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var recordVideoLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var mediaController: MediaController

    private var currentPhotoFile: File? = null
    private var currentVideoFile: File? = null
    private var pendingPermissionAction: (() -> Unit)? = null
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupVideoPreview()
        registerLaunchers()

        binding.captureButton.setOnClickListener {
            ensureCameraPermission { launchCamera() }
        }

        binding.recordButton.setOnClickListener {
            ensureCameraPermission { launchVideoCapture() }
        }
    }

    private fun registerLaunchers() {
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoFile?.let { file ->
                    displayPhoto(
                        FileProvider.getUriForFile(
                            this,
                            FILE_PROVIDER_AUTHORITY,
                            file
                        )
                    )
                }
            }
        }

        recordVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    currentVideoFile?.let { file ->
                        val uri = FileProvider.getUriForFile(
                            this,
                            FILE_PROVIDER_AUTHORITY,
                            file
                        )
                        displayVideo(uri)
                        revokeUriPermission(
                            uri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } ?: Toast.makeText(this, R.string.video_capture_failed, Toast.LENGTH_SHORT).show()
                }

                Activity.RESULT_CANCELED -> currentVideoFile?.delete()

                else -> {
                    Toast.makeText(this, R.string.video_capture_failed, Toast.LENGTH_SHORT).show()
                    currentVideoFile?.delete()
                }
            }
            currentVideoFile = null
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingPermissionAction?.invoke()
            } else {
                pendingPermissionAction = null
                showPermissionDeniedDialog()
            }
            pendingPermissionAction = null
        }
    }

    private fun setupVideoPreview() {
        mediaController = MediaController(this).apply {
            setAnchorView(binding.videoPreview)
        }
        binding.videoPreview.setMediaController(mediaController)
        binding.videoPreview.visibility = View.GONE
    }

    private fun ensureCameraPermission(onGranted: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> onGranted()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationale(onGranted)
            }

            else -> {
                pendingPermissionAction = onGranted
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showPermissionRationale(onPositiveAction: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.camera_permission_explanation)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                pendingPermissionAction = onPositiveAction
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

    private fun launchVideoCapture() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        val availableActivities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (availableActivities.isEmpty()) {
            Toast.makeText(this, R.string.camera_app_missing, Toast.LENGTH_SHORT).show()
            return
        }

        val videoFile = try {
            createVideoFile()
        } catch (ex: IOException) {
            ex.printStackTrace()
            Toast.makeText(this, R.string.video_capture_failed, Toast.LENGTH_SHORT).show()
            return
        }

        currentVideoFile = videoFile
        val uri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, videoFile)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

        availableActivities.forEach { info ->
            grantUriPermission(
                info.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        recordVideoLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(getExternalFilesDir(null), "images").apply { mkdirs() }
        return File.createTempFile("JPEG_${'$'}timeStamp_", ".jpg", storageDir)
    }

    private fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(getExternalFilesDir(null), "videos").apply { mkdirs() }
        return File.createTempFile("MP4_${'$'}timeStamp_", ".mp4", storageDir)
    }

    private fun displayPhoto(uri: Uri) {
        val bitmap = loadBitmapFromUri(uri)
        if (bitmap == null) {
            Toast.makeText(this, R.string.error_loading_photo, Toast.LENGTH_SHORT).show()
            return
        }
        binding.videoPreview.stopPlayback()
        binding.videoPreview.visibility = View.GONE
        binding.photoPreview.visibility = View.VISIBLE
        binding.photoPreview.setImageBitmap(bitmap)
        val formattedTime = timestampFormat.format(Date())
        binding.photoTimestampText.text = getString(R.string.last_photo_time, formattedTime)
        binding.captureButton.setText(R.string.retake_photo)
        binding.photoPreview.contentDescription = formattedTime
    }

    private fun displayVideo(uri: Uri) {
        try {
            binding.videoPreview.stopPlayback()
            binding.photoPreview.setImageBitmap(null)
            binding.photoPreview.visibility = View.GONE
            binding.videoPreview.visibility = View.VISIBLE
            binding.videoPreview.setVideoURI(uri)
            binding.videoPreview.setOnPreparedListener { mp ->
                mp.isLooping = false
                binding.videoPreview.seekTo(1)
            }
            binding.videoPreview.start()
            val formattedTime = timestampFormat.format(Date())
            binding.videoTimestampText.text = getString(R.string.last_video_time, formattedTime)
            binding.recordButton.setText(R.string.retake_video)
            binding.videoPreview.contentDescription = formattedTime
        } catch (ex: Exception) {
            ex.printStackTrace()
            Toast.makeText(this, R.string.error_loading_video, Toast.LENGTH_SHORT).show()
        }
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
