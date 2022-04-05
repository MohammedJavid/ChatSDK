package com.htcindia.chat.ui.chat.conversation

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.htcindia.chat.data.model.ChatMediaModel
import com.htcindia.chat.ui.chat.listeners.AccessTokenListener
import com.htcindia.twilio.utils.DateUtils
import com.htcindia.chat.ui.chat.listeners.QuickstartConversationsManagerListener
import com.twilio.conversations.*
import com.twilio.conversations.ConversationsClient.ConnectionState
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class QuickstartConversationsManager(
    private val fragmentContext: Context,
    private val participantList: List<String>,
    private var fileLoader: ProgressBar,
    private var paramValue: String,
    private val roomName: String,
    private var typingStarted: (String) -> Unit,
    private var typingEnded: (String) -> Unit,
    private val error: (String) -> Unit
) {

    private val DEFAULT_CONVERSATION_NAME = roomName

    private val messages = ArrayList<Message?>()

    private var typingParticipant: String? = null

    var conversationsClient: ConversationsClient? = null

    var conversation: Conversation? = null

    var conversationsManagerListener: QuickstartConversationsManagerListener? = null


    fun getDate(): ArrayList<Boolean> {
        var date: Date? = null
        val dateCountList = ArrayList<Boolean>()
        val dateFormat = SimpleDateFormat("dd MMM yyyy")
        messages.forEach { message ->
            val tempDate =
                dateFormat.parse(DateUtils.getDateWithServerTimeStampChat(message!!.dateUpdated)!!)
            if (date != null) {
                val daysCount =
                    TimeUnit.DAYS.convert(date!!.time - tempDate!!.time, TimeUnit.MILLISECONDS)
                if (daysCount != 0L) {
                    date = tempDate
                    dateCountList.add(true)
                } else {
                    dateCountList.add(false)
                }
            } else {
                date = tempDate
                dateCountList.add(true)
            }

        }
        return dateCountList
    }

    fun initializeWithAccessToken(context: Context?, token: String?) {
        val props = ConversationsClient.Properties.newBuilder().createProperties()
        ConversationsClient.create(context!!, token!!, props, mConversationsClientCallback)
    }

    private val mConversationsClientCallback: CallbackListener<ConversationsClient> =
        object : CallbackListener<ConversationsClient> {
            override fun onSuccess(conversationsClient: ConversationsClient) {
                this@QuickstartConversationsManager.conversationsClient = conversationsClient

                try {
                    conversationsClient.addListener(this@QuickstartConversationsManager.mConversationsClientListener)
                } catch (exception: Exception) {
                    exception.message?.let { Log.d("mConversationsClientCallback1: ", it) }
                    error("Error Joining Chat")
                }
            }

            override fun onError(errorInfo: ErrorInfo) {
                Log.d("mConversationsClientCallback2: ", errorInfo.message)
                error("Error Joining Chat")
            }
        }


    fun loadChannels() {
        if (conversationsClient == null || conversationsClient!!.myConversations == null) {
            return
        }

        try {
            conversationsClient!!.getConversation(
                DEFAULT_CONVERSATION_NAME,
                object : CallbackListener<Conversation?> {
                    override fun onSuccess(conversation: Conversation?) {
                        if (conversation != null) {

                            if (conversation.status == Conversation.ConversationStatus.JOINED
                                || conversation.status == Conversation.ConversationStatus.NOT_PARTICIPATING
                            ) {
                                Log.d("Already Exists in Conversation: ", DEFAULT_CONVERSATION_NAME)
                                this@QuickstartConversationsManager.conversation = conversation
                                this@QuickstartConversationsManager.conversation?.addListener(
                                    mDefaultConversationListener
                                )
                                this@QuickstartConversationsManager.loadPreviousMessages(
                                    conversation
                                )
                            } else {
                                Log.d(
                                   "Joining Conversation: " , DEFAULT_CONVERSATION_NAME
                                )
                                joinConversation(conversation)
                            }
                        }
                    }

                    override fun onError(errorInfo: ErrorInfo) {
                        Log.e(
                            "Error retrieving conversation: " , errorInfo.message
                        )
                        createConversation()
                    }
                })
        } catch (errorInfo: Exception) {
            error(errorInfo.message!!)
        }
    }

    private fun createConversation() {

        conversationsClient!!.createConversation(DEFAULT_CONVERSATION_NAME,
            object : CallbackListener<Conversation?> {
                override fun onSuccess(conversation: Conversation?) {
                    try {
                        conversation?.setUniqueName(DEFAULT_CONVERSATION_NAME) {

                            participantList.forEach {
                                conversation.addParticipantByIdentity(
                                    it,
                                    null,
                                    StatusListener { })
                            }
//                            conversation.addParticipantByIdentity(
//                                "Vinothkumar",
//                                null,
//                                StatusListener { })
//                            conversation.addParticipantByIdentity(
//                                "Sathishkumar",
//                                null,
//                                StatusListener { })
//                            conversation.addParticipantByIdentity(
//                                "Vignesh",
//                                null,
//                                StatusListener { })
//                            conversation.addParticipantByIdentity(
//                                "Kalaiselvan",
//                                null,
//                                StatusListener { })
//                            conversation.addParticipantByIdentity(
//                                "Javid",
//                                null,
//                                StatusListener { })
//                            conversation.addParticipantByIdentity(
//                                "Venkatesh",
//                                null,
//                                StatusListener { })

                            if (conversation != null) {
                                Log.d(
                                    "Twilio Chat",
                                    "Joining Conversation: $DEFAULT_CONVERSATION_NAME"
                                )
                                Log.d(
                                    "Twilio Chat",
                                    "onSuccess1: " + conversation.uniqueName
                                )
                                Log.d(
                                    "Twilio Chat",
                                    "onSuccess2: " + conversation.friendlyName
                                )

                                joinConversation(conversation)
                            } else {
                                Log.e("Twilio Chat", "Error Joining Chat")
                                error("Error Joining Chat")
                            }
                        }
                    } catch (exception: Exception) {
                        error("Error Joining Chats")
                    }
                }

                override fun onError(errorInfo: ErrorInfo) {
                    error(errorInfo.message)
                }
            })
    }

    private fun loadPreviousMessages(conversation: Conversation) {
        try {
            conversation.getLastMessages(200,
                object : CallbackListener<List<Message?>> {
                    override fun onSuccess(result: List<Message?>) {
                        messages.addAll(result)
                        if (conversationsManagerListener != null) {
                            conversationsManagerListener?.reloadMessages()
                        }
                    }

                    override fun onError(errorInfo: ErrorInfo) {
                        error(errorInfo.message)
                    }
                })
        } catch (errorInfo: Exception) {
            error(errorInfo.message!!)
        }
    }

    private fun joinConversation(conversation: Conversation) {
        Log.d("Joining Conversation: " ,conversation.uniqueName)
        try {
            if (conversation.status == Conversation.ConversationStatus.JOINED) {
                this@QuickstartConversationsManager.conversation = conversation
                this@QuickstartConversationsManager.conversation!!.addListener(
                    mDefaultConversationListener
                )
                return
            }
            conversation.join(object : StatusListener {
                override fun onSuccess() {

                    this@QuickstartConversationsManager.conversation = conversation
                    this@QuickstartConversationsManager.conversation!!.addListener(
                        mDefaultConversationListener
                    )
                    loadPreviousMessages(conversation)
                }

                override fun onError(errorInfo: ErrorInfo) {
                    Log.d("joinConversation: ", errorInfo.message)
                    error("Error Joining Chat")
                }
            })
        } catch (errorInfo: Exception) {
            error(errorInfo.message!!)
        }
    }

    @DelicateCoroutinesApi
    private val mConversationsClientListener: ConversationsClientListener =
        object : ConversationsClientListener {
            override fun onConversationAdded(conversation: Conversation) {}
            override fun onConversationUpdated(
                conversation: Conversation,
                updateReason: Conversation.UpdateReason
            ) {
            }

            override fun onConversationDeleted(conversation: Conversation) {}
            override fun onConversationSynchronizationChange(conversation: Conversation) {}
            override fun onError(errorInfo: ErrorInfo) {}
            override fun onUserUpdated(user: User, updateReason: User.UpdateReason) {}
            override fun onUserSubscribed(user: User) {}

            override fun onUserUnsubscribed(user: User) {}

            override fun onClientSynchronization(synchronizationStatus: ConversationsClient.SynchronizationStatus) {
                if (synchronizationStatus == ConversationsClient.SynchronizationStatus.COMPLETED) {
                    loadChannels()
                }
            }

            override fun onNewMessageNotification(s: String, s1: String, l: Long) {}
            override fun onAddedToConversationNotification(s: String) {}
            override fun onRemovedFromConversationNotification(s: String) {}
            override fun onNotificationSubscribed() {}
            override fun onNotificationFailed(errorInfo: ErrorInfo) {}
            override fun onConnectionStateChange(connectionState: ConnectionState) {
                if (connectionState.name.lowercase().contains("disconnected")) {
                    Toast.makeText(
                        fragmentContext,
                        "Chat ".plus(connectionState.name.lowercase()),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onTokenExpired() {}
            override fun onTokenAboutToExpire() {
                retrieveToken(object : AccessTokenListener {
                    override fun receivedAccessToken(token: String?, exception: Exception?) {
                        if (token != null) {
                            conversationsClient!!.updateToken(token) {}
                        }
                    }
                })
            }
        }

    private val mDefaultConversationListener: ConversationListener = object : ConversationListener {
        override fun onMessageAdded(message: Message) {
            messages.add(message)

            conversationsManagerListener?.receivedNewMessage()
        }

        override fun onMessageUpdated(message: Message, updateReason: Message.UpdateReason) {}

        override fun onMessageDeleted(message: Message) {
            for (index in 0..messages.size) {
                if (message == messages[index]) {
                    if (conversationsManagerListener != null) {
                        conversationsManagerListener!!.messageDeleteCallback(index)
                        messages.remove(message)
                        break
                    }
                }
            }
        }

        override fun onParticipantAdded(participant: Participant) {}

        override fun onParticipantUpdated(
            participant: Participant,
            updateReason: Participant.UpdateReason
        ) {
        }

        override fun onParticipantDeleted(participant: Participant) {}

        override fun onTypingStarted(conversation: Conversation, participant: Participant) {
            typingParticipant = participant.identity
            if (typingParticipant != null) {
                if (!participant.identity.equals(paramValue)) {
                    typingStarted(typingParticipant!!.substringBefore("(").trim())
                }
            }
        }

        override fun onTypingEnded(conversation: Conversation, participant: Participant) {
            typingParticipant = participant.identity
            if (typingParticipant != null) {
                if (!participant.identity.equals(paramValue)) {
                    typingEnded(typingParticipant!!.substringBefore("(").trim())
                }
            }
        }

        override fun onSynchronizationChanged(conversation: Conversation) {}
    }

    @DelicateCoroutinesApi
    private fun retrieveToken(listener: AccessTokenListener) {

//        lifecycleScope.launch {
//            try {
//                val response = twilioApiService.getChatToken(
//                    request = TwilioRequest(identity = paramValue)
//                )
//
//                if (response.body() != null) {
//                    listener.receivedAccessToken(response.body()!!.token, null)
//                }
//            } catch (exception: Exception) {
//                listener.receivedAccessToken(null, exception)
//            }
//        }
    }

    fun getMessages(): ArrayList<Message?> {
        return messages
    }

    fun setListener(listener: QuickstartConversationsManagerListener?) {
        conversationsManagerListener = listener
    }

    fun sendMessage(messageBody: String?) {
        if (conversation != null) {
            val options = Message.options().withBody(messageBody)
            conversation!!.sendMessage(options) {
                if (conversationsManagerListener != null) {
                    conversationsManagerListener!!.messageSentCallback()
                }
            }
        }
    }

    fun sendMediaMessage(attachment: ChatMediaModel) {
        fileLoader.max = attachment.fileSize!!.toLong().toInt()
        val options = Message.options()
            .withMedia(ByteArrayInputStream(attachment.file!!.readBytes()), attachment.fileType)
            .withMediaFileName(attachment.fileName)
            .withMediaProgressListener(object : ProgressListener {
                override fun onStarted() {
                    fileLoader.visibility = View.VISIBLE
                }

                override fun onProgress(bytes: Long) {
                    fileLoader.progress = bytes.toInt()
                }

                override fun onCompleted(mediaSid: String?) {
                    fileLoader.visibility = View.GONE
                }
            })

        conversation!!.sendMessage(options) {
            if (conversationsManagerListener != null) {
                conversationsManagerListener!!.messageSentCallback()
            }
        }
    }

}