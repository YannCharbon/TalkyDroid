package com.tmobop.talkydroid.storage

import androidx.lifecycle.LiveData
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

    suspend fun setUserName(id: UUID, userName: String) {
        userWithMessagesDao.setUserName(id, userName)
    }

    suspend fun deleteAllMessagesInConversation(senderId: UUID) {
        userWithMessagesDao.deleteAllMessagesInConversation(senderId)
    }

    suspend fun setUserAvatar(id: UUID, avatarPath: String) {
        userWithMessagesDao.setUserAvatar(id, avatarPath)
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

    fun getAllMessagesFromUserId(senderId: UUID): LiveData<List<MessageEntity>> {
        return userWithMessagesDao.getAllMessagesFromUserId(senderId)
    }

    suspend fun deleteUserFromUserId(id: UUID) {
        userWithMessagesDao.deleteUserFromUserId(id)
    }
}
