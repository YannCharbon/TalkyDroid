package com.tmobop.talkydroid.classes

class MessageUI(
    val content: String,
    val time: Long,
    val user: String,
    val avatarID: Int,
    val messageType: String) {

    companion object {
        const val TYPE_MESSAGE_SEND = 0
        const val TYPE_MESSAGE_RECEIVED = 1
        const val TYPE_IMAGE_SEND = 2
        const val TYPE_IMAGE_RECEIVED = 3
        const val TYPE_FILE_SEND = 4
        const val TYPE_FILE_RECEIVED = 5
        const val TYPE_LOCATION_SEND = 6
        const val TYPE_LOCATION_RECEIVED = 7
    }
}
