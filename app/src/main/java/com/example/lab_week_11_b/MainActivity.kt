package com.example.lab_week_11_b

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // Request code for permission request to external storage
    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 3
    }

    // Helper class to manage files in MediaStore
    private lateinit var providerFileManager: ProviderFileManager

    // Data model for the file
    private var photoInfo: FileInfo? = null
    private var videoInfo: FileInfo? = null

    // Flag to indicate whether the user is capturing a photo or video
    private var isCapturingVideo = false

    // Activity result launcher to capture images and videos
    private lateinit var takePictureLauncher: ActivityResultLauncher<android.net.Uri>
    private lateinit var takeVideoLauncher: ActivityResultLauncher<android.net.Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the ProviderFileManager
        providerFileManager =
            ProviderFileManager(
                applicationContext,
                FileHelper(applicationContext),
                contentResolver,
                Executors.newSingleThreadExecutor(),
                MediaContentHelper()
            )

        // Initialize the activity result launcher
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    providerFileManager.insertImageToStore(photoInfo)
                } else {
                    // Optional: Handle cancellation
                }
            }

        takeVideoLauncher =
            registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
                if (success) {
                    providerFileManager.insertVideoToStore(videoInfo)
                } else {
                    // Optional: Handle cancellation
                }
            }

        // Setup button listeners (using correct IDs from activity_main.xml)
        findViewById<android.widget.Button>(R.id.take_photo_button).setOnClickListener {
            // Set the flag to indicate that the user is capturing a photo
            isCapturingVideo = false
            // Check the storage permission
            checkStoragePermission {
                openImageCapture()
            }
        }

        findViewById<android.widget.Button>(R.id.record_video_button).setOnClickListener {
            // Set the flag to indicate that the user is capturing a video
            isCapturingVideo = true
            // Check the storage permission
            checkStoragePermission {
                openVideoCapture()
            }
        }
    }

    // Open the camera to capture an image
    private fun openImageCapture() {
        photoInfo =
            providerFileManager.generatePhotoUri(System.currentTimeMillis())

        // FIX: Use !! to assert that uri is not null, as we just assigned photoInfo
        takePictureLauncher.launch(photoInfo!!.uri)
    }

    // Open the camera to capture a video
    private fun openVideoCapture() {
        videoInfo =
            providerFileManager.generateVideoUri(System.currentTimeMillis())

        // FIX: Use !! to assert that uri is not null, as we just assigned videoInfo
        takeVideoLauncher.launch(videoInfo!!.uri)
    }

    // Check the storage permission
    private fun checkStoragePermission(onPermissionGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Check for the WRITE_EXTERNAL_STORAGE permission
            when (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )) {
                // If the permission is granted
                PackageManager.PERMISSION_GRANTED -> {
                    onPermissionGranted()
                }
                // if the permission is not granted, request the permission
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_EXTERNAL_STORAGE
                    )
                }
            }
        } else {
            onPermissionGranted()
        }
    }

    // For android 9 and below
    // Handle the permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions,
            grantResults)
        when (requestCode) {
            // Check if requestCode is for the External Storage permission or not
            REQUEST_EXTERNAL_STORAGE -> {
                // If granted, open the camera
                if ((grantResults.isNotEmpty() && grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED)) {
                    if (isCapturingVideo) {
                        openVideoCapture()
                    } else {
                        openImageCapture()
                    }
                }
                return
            }
            // for other request code, do nothing
            else -> {
            }
        }
    }
}