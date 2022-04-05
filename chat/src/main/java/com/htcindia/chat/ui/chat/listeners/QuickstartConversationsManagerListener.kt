package com.htcindia.chat.ui.chat.listeners

interface QuickstartConversationsManagerListener {
    fun receivedNewMessage()
    fun messageSentCallback()
    fun reloadMessages()
    fun messageDeleteCallback(position: Int)
}