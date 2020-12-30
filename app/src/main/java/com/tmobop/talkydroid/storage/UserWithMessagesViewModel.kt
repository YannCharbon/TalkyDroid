package com.tmobop.talkydroid.storage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Query
import java.util.*

open class UserWithMessagesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository : UserWithMessagesRepository

    init {
        val userWithMessagesDao = TalkyDroidDatabase
            .getDatabase(application, viewModelScope, application.resources)
            .userWithMessagesDao()
        repository = UserWithMessagesRepository(userWithMessagesDao)
    }

    fun getAllUsersWithMessages() : LiveData<List<UserWithMessages>> {
        return repository.getUsersWithMessages()
    }

    suspend fun insertUser(userEntity: UserEntity) {
        repository.insertUser(userEntity)
    }

    suspend fun deleteAllUsers() {
        repository.deleteAllUsers()
    }

    suspend fun setAllUsersOffline() {
        repository.setAllUsersOffline()
    }

    suspend fun setUserOnline(id: UUID) {
        repository.setUserOnline(id)
    }

    fun exists(id: UUID): LiveData<Boolean> {
        return repository.exists(id)
    }

    fun getAllMessages(): LiveData<List<MessageEntity>> {
        return repository.getAllMessages()
    }

    fun getUserWithMessagesFromId(id: UUID): LiveData<UserWithMessages> {
        return repository.getUserWithMessagesFromId(id)
    }

}