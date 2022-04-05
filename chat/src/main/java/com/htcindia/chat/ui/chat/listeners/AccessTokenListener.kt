package com.htcindia.chat.ui.chat.listeners

interface AccessTokenListener {
    fun receivedAccessToken(token: String?, exception: Exception?)
}