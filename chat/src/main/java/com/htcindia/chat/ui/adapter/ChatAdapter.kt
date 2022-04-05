package com.htcindia.chat.ui.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.htcindia.chat.R
import com.htcindia.chat.databinding.LayoutChatBinding
import com.htcindia.chat.ui.chat.conversation.QuickstartConversationsManager
import com.htcindia.twilio.utils.DateUtils
import com.htcindia.twilio.utils.humanReadableByteCountSI
import com.twilio.conversations.Message


class ChatAdapter(
    private val context: Context,
    private val paramValue: String,
    private val quickstartConversationsManager: QuickstartConversationsManager,
    private val clickListener: (Message) -> Unit,
    private val downloadFile: (String, String) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(val binding: LayoutChatBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnCreateContextMenuListener {

        override fun onCreateContextMenu(
            menu: ContextMenu?,
            p1: View?,
            p2: ContextMenu.ContextMenuInfo?
        ) {
            menu?.add("delete")
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = LayoutChatBinding.inflate(layoutInflater, parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = quickstartConversationsManager.getMessages()[position]
        val daysCount = quickstartConversationsManager.getDate()[position]

        if (message != null) {

            if (daysCount) {
                holder.binding.tvChatDate.text =
                    DateUtils.getChatDateWithServerTimeStamp(message.dateUpdated)
                holder.binding.clMessageDate.visibility = View.VISIBLE
            } else {
                holder.binding.clMessageDate.visibility = View.GONE
            }

            if (message.participant.identity.equals(paramValue, true)) {
                holder.binding.clSender.visibility = View.VISIBLE
                holder.binding.clReceiver.visibility = View.GONE
                holder.binding.flSenderImage.visibility = View.GONE
                holder.binding.llSenderMedia.visibility = View.GONE

                holder.binding.tvChatSenderTime.text =
                    DateUtils.getTimeWithServerTimeStamp(message.dateUpdated)
                if (message.hasMedia()) {
                    holder.binding.tvChatSenderText.visibility = View.GONE
//                    holder.binding.ivMsgStatus.visibility = View.GONE
                    if (message.mediaType.contains("jpeg") || message.mediaType.contains("jpg") || message.mediaType.contains("png") || message.mediaType.contains(
                            "gif"
                        )
                    ) {
                        holder.binding.flSenderImage.visibility = View.VISIBLE
                        holder.binding.llSenderMedia.visibility = View.GONE
                        holder.binding.tvChatSenderTime.visibility = View.GONE
                        holder.binding.tvSenderImageTime.text =
                            DateUtils.getTimeWithServerTimeStamp(message.dateUpdated)

                        holder.binding.ivSenderImage.setOnClickListener {
                            clickListener(message)
                        }

                        message.getMediaContentTemporaryUrl { result: String? ->


                            Glide.with(context)
                                .load(result)
                                .error(R.drawable.alert)
                                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                .listener(
                                    object : RequestListener<Drawable> {
                                        override fun onLoadFailed(
                                            e: GlideException?,
                                            model: Any?,
                                            target: Target<Drawable>?,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            holder.binding.clProgressBarSender.visibility =
                                                View.GONE
                                            return false
                                        }

                                        override fun onResourceReady(
                                            resource: Drawable?,
                                            model: Any?,
                                            target: Target<Drawable>?,
                                            dataSource: DataSource?,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            holder.binding.clProgressBarSender.visibility =
                                                View.GONE
                                            return false
                                        }
                                    }
                                )
                                .into(holder.binding.ivSenderImage)
                        }

                    } else {

                        holder.binding.flSenderImage.visibility = View.GONE
                        holder.binding.llSenderMedia.visibility = View.VISIBLE
                        holder.binding.tvChatSenderTime.visibility = View.VISIBLE
                        holder.binding.tvSenderMediaName.text = message.mediaFileName
                        holder.binding.tvSenderMediaSize.text =
                            humanReadableByteCountSI(message.mediaSize)

                        holder.binding.tvChatSenderTime.text =
                            DateUtils.getTimeWithServerTimeStamp(message.dateUpdated)

                        message.getMediaContentTemporaryUrl { result: String? ->

                            holder.binding.tvSenderDownload.setOnClickListener {
                                result?.let { it1 ->
                                    downloadFile(
                                        it1,
                                        message.mediaFileName ?: "file.${message.mediaType}"
                                    )
                                }
                            }
                        }
                    }
                } else {
                    holder.binding.tvChatSenderText.visibility = View.VISIBLE
                    holder.binding.flSenderImage.visibility = View.GONE
                    holder.binding.llSenderMedia.visibility = View.GONE
                    holder.binding.tvChatSenderTime.visibility = View.VISIBLE
                    holder.binding.tvChatSenderText.text = message.messageBody

                }
            } else {

                holder.binding.clSender.visibility = View.GONE
//                holder.binding.ivMsgStatus.visibility = View.GONE
                holder.binding.clReceiver.visibility = View.VISIBLE
                holder.binding.flReceiverImage.visibility = View.GONE
                holder.binding.llReceiverMedia.visibility = View.GONE
                holder.binding.tvChatReceiverAuthor.text =
                    message.participant.identity.substringBefore("(").trim()
                holder.binding.tvChatReceiverTime.text =
                    DateUtils.getTimeWithServerTimeStamp(message.dateUpdated)
                if (message.hasMedia()) {
                    holder.binding.tvChatReceiverText.visibility = View.GONE
                    if (message.mediaType.contains("jpeg") || message.mediaType.contains("jpg") || message.mediaType.contains("png") || message.mediaType.contains(
                            "gif"
                        )
                    ) {
                        holder.binding.flReceiverImage.visibility = View.VISIBLE
                        holder.binding.llReceiverMedia.visibility = View.GONE
                        holder.binding.tvChatReceiverTime.visibility = View.GONE
                        holder.binding.tvChatReceiverAuthor.visibility = View.GONE

                        holder.binding.tvReceiverImageAuthor.text =
                            message.participant.identity.substringBefore("(").trim()
                        holder.binding.tvReceiverImageTime.text =
                            DateUtils.getTimeWithServerTimeStamp(message.dateUpdated)

                        holder.binding.ivReceiverImage.setOnClickListener {
                            clickListener(message)
                        }


                        message.getMediaContentTemporaryUrl { result: String? ->

                            Glide.with(context)
                                .load(result)
                                .error(R.drawable.alert)
                                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                .listener(
                                    object : RequestListener<Drawable> {
                                        override fun onLoadFailed(
                                            e: GlideException?,
                                            model: Any?,
                                            target: Target<Drawable>?,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            holder.binding.clProgressBarReceiver.visibility =
                                                View.GONE
                                            return false
                                        }

                                        override fun onResourceReady(
                                            resource: Drawable?,
                                            model: Any?,
                                            target: Target<Drawable>?,
                                            dataSource: DataSource?,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            holder.binding.clProgressBarReceiver.visibility =
                                                View.GONE
                                            return false
                                        }
                                    }
                                )
                                .into(holder.binding.ivReceiverImage)
                        }

                    } else {
                        holder.binding.flReceiverImage.visibility = View.GONE
                        holder.binding.llReceiverMedia.visibility = View.VISIBLE
                        holder.binding.tvChatReceiverTime.visibility = View.VISIBLE
                        holder.binding.tvReceiverMediaName.text = message.mediaFileName
                        holder.binding.tvReceiverMediaSize.text =
                            humanReadableByteCountSI(message.mediaSize)

                        holder.binding.tvChatReceiverTime.text =
                            DateUtils.getTimeWithServerTimeStamp(message.dateUpdated)

                        message.getMediaContentTemporaryUrl { result: String? ->

                            holder.binding.tvReceiverDownload.setOnClickListener {
                                result?.let { it1 ->
                                    downloadFile(
                                        it1,
                                        message.mediaFileName ?: "file.${message.mediaType}"
                                    )
                                }
                            }
                        }
                    }

                } else {
                    holder.binding.flReceiverImage.visibility = View.GONE
                    holder.binding.llReceiverMedia.visibility = View.GONE
                    holder.binding.tvChatReceiverText.visibility = View.VISIBLE
                    holder.binding.tvChatReceiverTime.visibility = View.VISIBLE
                    holder.binding.tvChatReceiverText.text = message.messageBody
                    holder.binding.tvChatReceiverTime.text =
                        DateUtils.getTimeWithServerTimeStamp(message.dateUpdated)
                }


            }
        }
    }


    override fun getItemCount(): Int = quickstartConversationsManager.getMessages().size

    override fun getItemViewType(position: Int): Int {
        return position
    }

}