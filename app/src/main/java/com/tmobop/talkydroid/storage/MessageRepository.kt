package com.tmobop.talkydroid.storage

import androidx.lifecycle.LiveData
import java.util.*

class MessageRepository(private val messageDao: MessageDao) {

    suspend fun insertMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }

    suspend fun updateMessage(message: MessageEntity) {
        messageDao.updateMessage(message)
    }

    suspend fun deleteMessage(message: MessageEntity) {
        messageDao.deleteMessage(message)
    }

    suspend fun deleteAllMessages() {
        messageDao.deleteAllMessages()
    }

    fun getAllMessages(): LiveData<List<MessageEntity>> {
        return messageDao.getAllMessages()
    }

    fun getLastMessage(): LiveData<MessageEntity> {
        return messageDao.getLastMessage()
    }

    fun getOrderedMessages(): LiveData<List<MessageEntity>> {
        return messageDao.getOrderedMessages()
    }

    fun getAllMessagesFromUserId(senderId: UUID): LiveData<List<MessageEntity>> {
        return messageDao.getAllMessagesFromUserId(senderId)
    }
}