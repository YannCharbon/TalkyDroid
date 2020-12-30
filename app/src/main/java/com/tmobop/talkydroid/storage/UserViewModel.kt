package com.tmobop.talkydroid.storage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope

open class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserRepository

    init {
        val userDao = TalkyDroidDatabase
            .getDatabase(application, viewModelScope, application.resources)
            .userDao()
        repository = UserRepository(userDao)
    }

    fun getAllUsers() : LiveData<List<UserEntity>> {
        return repository.getAllUsers()
    }
}