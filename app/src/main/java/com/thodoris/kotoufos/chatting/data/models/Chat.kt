package com.thodoris.kotoufos.chatting.data.models

data class Chat(
    val id: String = "",
    val participants: List<String>,
    val lastMessage: String,
    val timestamp: Long
)
