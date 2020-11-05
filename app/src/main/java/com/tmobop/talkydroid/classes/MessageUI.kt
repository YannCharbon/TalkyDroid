package com.tmobop.talkydroid.classes

class MessageUI(val content : String,
                val time : Long,
                val user : String,
                val avatarID : Int,
                val messageType : Int) {

    companion object {
        const val TYPE_MESSAGE_SEND = 0
        const val TYPE_MESSAGE_RECEIVED = 1
    }
}