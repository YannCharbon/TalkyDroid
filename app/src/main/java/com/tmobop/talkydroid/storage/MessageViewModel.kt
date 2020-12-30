package com.tmobop.talkydroid.storage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope


class MessageViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MessageRepository

    init {
        val messageDao = TalkyDroidDatabase
            .getDatabase(application, viewModelScope, application.resources)
            .messageDao()
        repository = MessageRepository(messageDao)
    }

    fun getAllMessages() : LiveData<List<MessageEntity>> {
        return repository.getAllMessages()
    }

    suspend fun insertMessage(message: MessageEntity) {
        repository.insertMessage(message)
    }
}