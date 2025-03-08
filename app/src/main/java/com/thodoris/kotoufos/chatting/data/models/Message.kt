package com.thodoris.kotoufos.chatting.data.models

data class Message(
    val id: String = "",
    val chatID: String,
    val senderID: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)