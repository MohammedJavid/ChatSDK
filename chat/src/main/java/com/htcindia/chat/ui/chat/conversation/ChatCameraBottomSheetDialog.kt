package com.htcindia.chat.ui.chat.conversation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.htcindia.chat.data.model.ChatMediaModel
import com.htcindia.chat.databinding.LayoutChatCameraSheetBinding
import com.htcindia.twilio.utils.getFileName
import com.htcindia.twilio.utils.getFileSize
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatCameraBottomSheetDialog(
    private val pathProvider: String,
    private val getFile: (ChatMediaModel) -> Unit) :
    BottomSheetDialogFragment() {

    private lateinit var binding: LayoutChatCameraSheetBinding
    private lateinit var cameraActions: ActivityResultLauncher<Intent>
    private lateinit var videoActions: ActivityResultLauncher<Intent>
    private var mUri: Uri? = null
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestVideoPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutChatCameraSheetBinding.inflate(inflater, container, false)

        binding.clCapture.setOnClickListener {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.clCaptureVideo.setOnClickListener {
            requestVideoPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    takePhoto()
                } else {
                    requestCameraPermission()
                }
            }

        requestVideoPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    takeVideo()
                } else {
                    requestVideoPermission()
                }
            }

        cameraActions =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK && mUri != null) {
                    mUri.let { uri ->
                        val parcelFileDescriptor =
                            requireActivity().contentResolver.openFileDescriptor(uri!!, "r", null)
                                ?: return@registerForActivityResult
                        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
                        val fileInfo = File(
                            requireActivity().cacheDir,
                            requireActivity().contentResolver.getFileName(uri)
                        )
                        val outputStream = FileOutputStream(fileInfo)
                        inputStream.copyTo(outputStream)
                        if (requireActivity().contentResolver.getFileSize(uri)
                                .toLong() / 1024 < 5120
                        ) {
                            getFile(
                                ChatMediaModel(
                                    file = fileInfo,
                                    fileName = fileInfo.name,
                                    fileSize = requireActivity().contentResolver.getFileSize(uri)
                                        .toLong().toString(),
                                    fileType = requireActivity().contentResolver.getType(uri)!!
                                )
                            )
                        } else {
                            Toast.makeText(
                                requireActivity(),
                                "${fileInfo.name} exceeds maximum size of 5Mb",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                this.dismiss()
            }

        videoActions = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK && mUri != null) {
                mUri.let { uri ->
                    val parcelFileDescriptor =
                        requireActivity().contentResolver.openFileDescriptor(uri!!, "r", null)
                            ?: return@registerForActivityResult
                    val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
                    val fileInfo = File(
                        requireActivity().cacheDir,
                        requireActivity().contentResolver.getFileName(uri)
                    )
                    val outputStream = FileOutputStream(fileInfo)
                    inputStream.copyTo(outputStream)
                    if (requireActivity().contentResolver.getFileSize(uri).toLong() / 1024 < 5120) {
                        getFile(
                            ChatMediaModel(
                                file = fileInfo,
                                fileName = fileInfo.name,
                                fileSize = requireActivity().contentResolver.getFileSize(uri)
                                    .toLong().toString(),
                                fileType = requireActivity().contentResolver.getType(uri)!!
                            )
                        )
                    } else {
                        Toast.makeText(
                            requireActivity(),
                            "${fileInfo.name} exceeds maximum size of 5Mb",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            this.dismiss()
        }

        return binding.root
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takePhoto()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                this.dismiss()
            }
            else -> {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts(
                    "package",
                    pathProvider, null
                )
                intent.data = uri
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                this.dismiss()
            }
        }
    }

    private fun requestVideoPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takeVideo()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                this.dismiss()
            }
            else -> {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts(
                    "package",
                    pathProvider, null
                )
                intent.data = uri
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                this.dismiss()
            }
        }
    }

    private fun takePhoto() {
        mUri = null
        val timeStamp = SimpleDateFormat("ddMMyyyy_HHmmss").format(Date())

//        val capturedImage = File.createTempFile(timeStamp, ".jpg", requireActivity().cacheDir)
        val capturedImage = File(requireActivity().externalCacheDir, "${timeStamp}.jpg")
        if (capturedImage.exists()) {
            capturedImage.delete()
        }
        capturedImage.createNewFile()
        mUri = if (Build.VERSION.SDK_INT >= 24) {
            FileProvider.getUriForFile(
                requireActivity(), pathProvider + ".provider",
                capturedImage
            )
        } else {
            Uri.fromFile(capturedImage)
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)

        cameraActions.launch(intent)
    }

    private fun takeVideo() {
        mUri = null
        val timeStamp = SimpleDateFormat("ddMMyyyy_HHmmss").format(Date())
        val capturedImage = File(requireActivity().externalCacheDir, "${timeStamp}.mp4")
        if (capturedImage.exists()) {
            capturedImage.delete()
        }
        capturedImage.createNewFile()
        mUri = if (Build.VERSION.SDK_INT >= 24) {
            FileProvider.getUriForFile(
                requireActivity(), pathProvider + ".provider",
                capturedImage
            )
        } else {
            Uri.fromFile(capturedImage)
        }

        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)

        videoActions.launch(intent)
    }

}