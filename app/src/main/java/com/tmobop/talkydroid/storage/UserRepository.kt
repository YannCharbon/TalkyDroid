package com.tmobop.talkydroid.storage

import androidx.lifecycle.LiveData
import java.util.*

class UserRepository(private val userDao: UserDao) {

    suspend fun insertUser(user : UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user : UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user : UserEntity) {
        userDao.deleteUser(user)
    }

    suspend fun deleteAllUsers() {
        userDao.deleteAllUsers()
    }

    fun exists(id: UUID) : LiveData<Boolean> {
        return userDao.exists(id)
    }

    fun getUserFromId(id: UUID) : LiveData<UserEntity> {
        return userDao.getUserFromId(id)
    }

    fun getAllUsers() : LiveData<List<UserEntity>> {
        return userDao.getAllUsers()
    }

}