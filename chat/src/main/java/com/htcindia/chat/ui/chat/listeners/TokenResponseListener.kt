package com.pinkerton.vigilance.ui.chatandlivestream.listeners

interface TokenResponseListener {
    fun receivedTokenResponse(success: Boolean, exception: Exception?)
}