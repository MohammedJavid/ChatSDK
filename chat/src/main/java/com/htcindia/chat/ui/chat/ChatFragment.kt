package com.htcindia.chat.ui.chat

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher

import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.htcindia.chat.R
import com.htcindia.chat.data.model.ChatMediaModel
import com.htcindia.chat.databinding.FragmentChatBinding
import com.htcindia.chat.databinding.LayoutViewImageBinding
import com.htcindia.chat.ui.adapter.ChatAdapter
import com.htcindia.chat.ui.chat.conversation.ChatBottomSheetDialog
import com.htcindia.chat.ui.chat.conversation.ChatCameraBottomSheetDialog
import com.htcindia.chat.ui.chat.conversation.QuickstartConversationsManager
import com.htcindia.twilio.utils.humanReadableByteCountSI
import com.htcindia.chat.ui.chat.listeners.QuickstartConversationsManagerListener
import com.twilio.conversations.Message
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


class ChatFragment(private val activity: AppCompatActivity,
                   private val accessToken: String,
                   private val identity: String,
                   private val roomName: String,
                   private val participantsList: List<String>,
                   private val pathProvider: String) : Fragment(), QuickstartConversationsManagerListener {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private var quickstartConversationsManager: QuickstartConversationsManager? = null
    private lateinit var adapter: ChatAdapter
    private var chatImageDialog: Dialog? = null
    private var chatImageLayout: LayoutViewImageBinding? = null
    private var downloadManager: DownloadManager? = null
    private var downLoadId: Long? = 0
    private lateinit var requestAudioPermissionLauncher: ActivityResultLauncher<String>
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var countDownTimer: CountDownTimer? = null
    private var second = -1
    private var minute: Int = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)

        loading()
        quickstartConversationsManager = QuickstartConversationsManager(
            fragmentContext = activity,
            participantList = participantsList,
            fileLoader = binding.progressBarFile,
            paramValue = identity,
            roomName = roomName,
            typingStarted = { typingName: String -> typingStarted(typingName) },
            typingEnded = { typingName: String -> typingEnded(typingName) },
            error = { error: String -> error(error) }
        )

        quickstartConversationsManager?.let {
            adapter = ChatAdapter(activity,
                identity,
                it,
                { selectedItem: Message -> chatItemClicked(selectedItem) },
                { fileUrl: String, fileName: String -> downloadAttachmentFile(fileUrl, fileName) }
            )
        }

        quickstartConversationsManager?.initializeWithAccessToken(activity, accessToken)

        chatImageLayout = LayoutViewImageBinding.inflate(LayoutInflater.from(activity))
        chatImageDialog = Dialog(activity, R.style.list_dialog_style)
        chatImageDialog?.setContentView(chatImageLayout?.root!!)
        quickstartConversationsManager?.setListener(this)

        initializeDownloadManager()
        binding.rclrViewChat.setHasFixedSize(true)
        adapter.setHasStableIds(true)
        binding.rclrViewChat.adapter = adapter

        binding.rclrViewChat.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                binding.rclrViewChat.smoothScrollToPosition(
                    bottom
                )
            }
        }

        requestAudioPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    recordAudio()
                } else {
                    requestAudioPermission()
                }
            }

        return binding.root
    }


    override fun onResume() {
        super.onResume()

        binding.ivAttachment.setOnClickListener {
            ChatBottomSheetDialog { attachment -> getFile(attachment) }.apply {
                show(
                    this@ChatFragment.childFragmentManager,
                    "ChatBottomSheetDialog"
                )
            }
        }

        binding.ivCamera.setOnClickListener {
            ChatCameraBottomSheetDialog(pathProvider) { attachment -> getFile(attachment) }.apply {
                show(
                    this@ChatFragment.childFragmentManager,
                    "ChatCameraBottomSheetDialog"
                )
            }
        }

        binding.ivSend.setOnClickListener {
            val messageBody: String = binding.etMessage.text.toString().trim()
            if (!binding.etMessage.text.isNullOrBlank()) {
                quickstartConversationsManager?.sendMessage(messageBody)
            }

            binding.etMessage.requestFocus()
            binding.etMessage.text.clear()
        }

        binding.ivAudio.setOnClickListener {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        binding.ivAudioStop.setOnClickListener {
            try {
                recorder!!.stop()
                recorder!!.release()
                resetTimer()
                if (audioFile != null) {
                    if (audioFile!!.length() / 1024 <= 5120) {
                        val audioMessage = ChatMediaModel(
                            file = audioFile,
                            fileName = audioFile!!.name,
                            fileSize = audioFile!!.length().toString(),
                            fileType = "audio/mpeg"
                        )
                        quickstartConversationsManager?.sendMediaMessage(audioMessage)
                    } else {
                        Toast.makeText(
                            activity,
                            "${audioFile!!.name} exceeds maximum size of 5Mb",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                binding.clAudioRecord.visibility = View.GONE
                binding.clMessage.visibility = View.VISIBLE

            } catch (ex: Exception) {
                Toast.makeText(activity, "Recording Error : Failed", Toast.LENGTH_SHORT)
                    .show()
                binding.clAudioRecord.visibility = View.GONE
                binding.clMessage.visibility = View.VISIBLE
            }
        }

        binding.ivAudioCancel.setOnClickListener {
            if (recorder != null) {
                try {
                    recorder!!.stop()
                    recorder!!.release()
                    resetTimer()
                    binding.clAudioRecord.visibility = View.GONE
                    binding.clMessage.visibility = View.VISIBLE
                } catch (ex: Exception) {
                    resetTimer()
                    binding.clAudioRecord.visibility = View.GONE
                    binding.clMessage.visibility = View.VISIBLE
                }
            }
        }


        binding.etMessage.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    quickstartConversationsManager?.conversation?.typing()
                    binding.ivAudio.visibility = View.GONE
                    binding.ivSend.visibility = View.VISIBLE
                }

                override fun afterTextChanged(text: Editable?) {

                    if (!text.toString().isNullOrBlank()) {
                        binding.ivAudio.visibility = View.GONE
                        binding.ivSend.visibility = View.VISIBLE
                    } else {
                        binding.ivAudio.visibility = View.VISIBLE
                        binding.ivSend.visibility = View.GONE
                    }
                }
            }
        )
        binding.btnChatBack.setOnClickListener {
            activity.onBackPressed()
        }
    }

    override fun receivedNewMessage() {
        runBlocking {
            adapter.notifyItemInserted(quickstartConversationsManager?.getMessages()?.size?: 0 - 1)
            binding.rclrViewChat.postDelayed({
                if (adapter.itemCount > 0) {
                    binding.rclrViewChat.scrollToPosition(binding.rclrViewChat.adapter?.itemCount!! - 1)
                }
            }, 1000)
        }
    }

    override fun reloadMessages() {
        runBlocking {
            adapter.notifyDataSetChanged()
            binding.rclrViewChat.postDelayed({
                if (adapter.itemCount > 0) {
                    binding.rclrViewChat.scrollToPosition(binding.rclrViewChat.adapter?.itemCount!! - 1)
                }
                success()
            }, 1000)
        }
    }

    override fun messageSentCallback() {

        runBlocking {
            binding.etMessage.setText("")
        }
    }

    override fun messageDeleteCallback(position: Int) {
        runBlocking {
            adapter.notifyItemRemoved(position)
        }
        binding.clChatOption.visibility = View.GONE
        binding.clMessage.visibility = View.VISIBLE
    }

    private fun initializeDownloadManager() {
        downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
    }

    private fun downloadAttachmentFile(uri: String, filename: String) {
        Toast.makeText(activity, "File is downloading...", Toast.LENGTH_SHORT).show()
        val request =
            DownloadManager.Request(Uri.parse(uri))
        request.setTitle(filename)
            .setDescription("File is downloading...")
            .setDestinationInExternalFilesDir(
                requireContext(),
                Environment.DIRECTORY_DOWNLOADS, filename
            )
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        downLoadId = downloadManager!!.enqueue(request)
    }


    private fun requestAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                recordAudio()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {}
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
            }
        }
    }

    private fun recordAudio() {
        val timeStamp = SimpleDateFormat("ddMMyyyy_HHmmss").format(Date())
        recorder = MediaRecorder()
        recorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        audioFile = File(activity.externalCacheDir, "${timeStamp}.mp3")
        recorder!!.setOutputFile(audioFile!!.path)
        recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        try {
            recorder!!.prepare()
            recorder!!.start()
            showTimer()
            binding.clAudioRecord.visibility = View.VISIBLE
            binding.clMessage.visibility = View.GONE
        } catch (ex: Exception) {
        }
    }

    private fun showTimer() {
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                second++
                binding.tvAudioTimer.text = recorderTime()
                if (second > 120) {
                    try {
                        recorder!!.stop()
                        recorder!!.release()
                        resetTimer()
                        Toast.makeText(
                            activity,
                            "${audioFile!!.name} exceeds maximum time of 2 Mins",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (audioFile != null) {
                            if (audioFile!!.length() / 1024 <= 5120) {
                                val audioMessage = ChatMediaModel(
                                    file = audioFile,
                                    fileName = audioFile!!.name,
                                    fileSize = humanReadableByteCountSI(audioFile!!.length())!!,
                                    fileType = "audio/mpeg"
                                )
                                quickstartConversationsManager?.sendMediaMessage(audioMessage)

                            } else {
                                Toast.makeText(
                                    activity,
                                    "${audioFile!!.name} exceeds maximum size of 5Mb",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        binding.clAudioRecord.visibility = View.GONE
                        binding.clMessage.visibility = View.VISIBLE

                    } catch (ex: Exception) {
                        Toast.makeText(
                            activity,
                            "Recording Error : Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.clAudioRecord.visibility = View.GONE
                        binding.clMessage.visibility = View.VISIBLE
                    }
                }
            }

            override fun onFinish() {}
        }
        countDownTimer!!.start()
    }

    fun recorderTime(): String {
        if (second == 60) {
            minute++
            second = 0
        }
        return "${DecimalFormat("00").format(minute)} : ${DecimalFormat("00").format(second)}"
    }

    private fun resetTimer() {
        if (countDownTimer != null) {
            countDownTimer?.cancel()
            second = -1
            minute = 0
            binding.tvAudioTimer.text = "00:00"
        }
    }

    private fun getFile(attachment: ChatMediaModel) {
        if (attachment.file != null) {
            quickstartConversationsManager?.sendMediaMessage(attachment)
        }
    }

    private fun chatItemClicked(message: Message) {

        chatImageLayout?.clProgressBar!!.visibility = View.VISIBLE
        chatImageLayout?.ivSourceImage!!.setImageDrawable(null)
        chatImageLayout?.tvImageTitle!!.text = message.mediaFileName
        message.getMediaContentTemporaryUrl {
            chatImageLayout!!.btnImageDownload.setOnClickListener { _ ->
                downloadAttachmentFile(it, message.mediaFileName)
            }
            Glide.with(activity)
                .load(it)
                .error(R.drawable.alert)
                .listener(
                    object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            chatImageLayout?.clProgressBar!!.visibility = View.GONE
                            return false
                        }
                    }
                )
                .into(chatImageLayout?.ivSourceImage!!)
        }

        chatImageLayout?.btnImageBack!!.setOnClickListener {
            chatImageDialog?.dismiss()
        }

        chatImageDialog?.show()
    }

    private fun typingEnded(typingName: String) {
        if (binding.tvUsername.text.toString().equals(typingName)) {
            binding.tvUsername.text = null
            binding.tvUsername.visibility = View.GONE
            binding.tvIsTyping.visibility = View.GONE
        }
    }

    private fun typingStarted(typingName: String) {
        binding.tvUsername.text = typingName
        binding.tvUsername.visibility = View.VISIBLE
        binding.tvIsTyping.visibility = View.VISIBLE
    }

    fun loading() {
        binding.clProgressBar.isVisible = true
        binding.constraintLayout2.isVisible = false
        binding.clErrorMessage.isVisible = false
    }

    private fun success() {
        binding.clProgressBar.isVisible = false
        binding.constraintLayout2.isVisible = true
        binding.clErrorMessage.isVisible = false
    }

    private fun error(message: String) {
        binding.clProgressBar.isVisible = false
        binding.constraintLayout2.isVisible = false
        binding.clErrorMessage.isVisible = true
        binding.tvErrorMsg.text = message
    }

}