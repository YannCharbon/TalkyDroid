package com.tmobop.talkydroid.storage

import androidx.lifecycle.LiveData
import androidx.room.Query
import java.util.*

class UserWithMessagesRepository(private val userWithMessagesDao: UserWithMessagesDao) {

    suspend fun insertUser(userEntity: UserEntity) {
        userWithMessagesDao.insertUser(userEntity)
    }

    suspend fun deleteAllUsers() {
        userWithMessagesDao.deleteAllUsers()
    }

    suspend fun setAllUsersOffline() {
        userWithMessagesDao.setAllUsersOffline()
    }

    suspend fun setUserOnline(id: UUID) {
        userWithMessagesDao.setUserOnline(id)
    }

    fun exists(id: UUID): LiveData<Boolean> {
        return userWithMessagesDao.exists(id)
    }

    fun getAllMessages(): LiveData<List<MessageEntity>> {
        return userWithMessagesDao.getAllMessages()
    }

    fun getUsersWithMessages(): LiveData<List<UserWithMessages>> {
        return userWithMessagesDao.getUsersWithMessages()
    }

    fun getUserWithMessagesFromId(id: UUID): LiveData<UserWithMessages> {
        return userWithMessagesDao.getUserWithMessagesFromId(id)
    }

    fun getMessagesFromUserIds(senderId: UUID, receiverId: UUID): LiveData<List<MessageEntity?>?> {
        return userWithMessagesDao.getMessagesFromUserIds(senderId, receiverId)
    }
}
