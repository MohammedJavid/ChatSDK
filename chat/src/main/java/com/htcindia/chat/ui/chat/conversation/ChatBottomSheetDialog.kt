package com.htcindia.chat.ui.chat.conversation


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.htcindia.chat.data.model.ChatMediaModel
import com.htcindia.chat.databinding.LayoutChatBottomSheetBinding
import com.htcindia.twilio.utils.getFileName
import com.htcindia.twilio.utils.getFileSize
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ChatBottomSheetDialog(private val getFile: (ChatMediaModel) -> Unit) :
    BottomSheetDialogFragment() {

    private lateinit var binding: LayoutChatBottomSheetBinding
    private lateinit var actions: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutChatBottomSheetBinding.inflate(inflater, container, false)

        actions = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                if (it.data!!.data != null) {
                    val fileData = it.data!!.data!!
                    val parcelFileDescriptor =
                        requireActivity().contentResolver.openFileDescriptor(fileData, "r", null)
                            ?: return@registerForActivityResult
                    val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
                    val fileInfo = File(
                        requireActivity().externalCacheDir,
                        requireActivity().contentResolver.getFileName(fileData)
                    )
                    val outputStream = FileOutputStream(fileInfo)
                    inputStream.copyTo(outputStream)
                    if (requireActivity().contentResolver.getFileSize(fileData)
                            .toLong() / 1024 < 5120
                    ) {
                        getFile(
                            ChatMediaModel(
                                file = fileInfo,
                                fileName = fileInfo.name,
                                fileSize = requireActivity().contentResolver.getFileSize(fileData)
                                    .toLong().toString(),
                                fileType = requireActivity().contentResolver.getType(fileData)!!
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

        setUi()
        return binding.root
    }

    private fun setUi() {

        binding.clImages.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                val mimeTypes = arrayOf("image/png", "image/jpg", "image/jpeg")
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            actions.launch(intent)
        }

        binding.clVideos.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                val mimeTypes = arrayOf("video/mp4", "video/quicktime")
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            actions.launch(intent)
        }

        binding.clAudios.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                val mimeTypes = arrayOf("audio/mpeg", "audio/x-wav")
                type = "audio/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            actions.launch(intent)
        }

        binding.clDocument.setOnClickListener {
            val allSupportedDocumentsTypesToExtensions = mapOf(
                "application/msword" to ".doc",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to ".docx",
                "application/pdf" to ".pdf",
                "application/vnd.ms-excel" to ".xls",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to ".xlsx"
            )

            val supportedMimeTypes = allSupportedDocumentsTypesToExtensions.keys.toTypedArray()

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/pdf"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, supportedMimeTypes)
            }
            actions.launch(intent)
        }

    }


}

