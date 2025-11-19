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
        // .TakePicture() and .CaptureVideo() are the built-in contracts
        // They are used to capture images and videos
        // The result will be stored in the URI passed to the launcher
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    providerFileManager.insertImageToStore(photoInfo)
                } else {
                    // Handle cancellation or failure if necessary
                }
            }

        takeVideoLauncher =
            registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
                if (success) {
                    providerFileManager.insertVideoToStore(videoInfo)
                } else {
                    // Handle cancellation or failure if necessary
                }
            }

        // Setup button listeners
        // Note: The IDs are referenced as R.id.photo_button and R.id.video_button
        // but based on your activity_main.xml, they should be take_photo_button and record_video_button.
        // I will use the correct IDs from the XML here.

        findViewById<android.widget.Button>(R.id.take_photo_button).setOnClickListener {
            // Set the flag to indicate that the user is capturing a photo
            isCapturingVideo = false
            // Check the storage permission
            // If the permission is granted, open the camera
            // Otherwise, request the permission
            checkStoragePermission {
                openImageCapture()
            }
        }

        findViewById<android.widget.Button>(R.id.record_video_button).setOnClickListener {
            // Set the flag to indicate that the user is capturing a video
            isCapturingVideo = true
            // Check the storage permission
            // If the permission is granted, open the camera
            // Otherwise, request the permission
            checkStoragePermission {
                openVideoCapture()
            }
        }
    }

    // Open the camera to capture an image
    private fun openImageCapture() {
        photoInfo =
            providerFileManager.generatePhotoUri(System.currentTimeMillis())
        takePictureLauncher.launch(photoInfo?.uri)
    }

    // Open the camera to capture a video
    private fun openVideoCapture() {
        videoInfo =
            providerFileManager.generateVideoUri(System.currentTimeMillis())
        takeVideoLauncher.launch(videoInfo?.uri)
    }

    // Check the storage permission
    // For Android 10 (Q) and above, the WRITE_EXTERNAL_STORAGE permission is not strictly required for MediaStore
    // For Android 9 and below, the permission is required
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