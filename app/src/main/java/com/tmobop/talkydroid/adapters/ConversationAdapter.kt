package com.tmobop.talkydroid.adapters

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.classes.MessageType
import com.tmobop.talkydroid.classes.MessageUI
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_FILE_RECEIVED
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_FILE_SEND
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_IMAGE_RECEIVED
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_IMAGE_SEND
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_LOCATION_RECEIVED
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_LOCATION_SEND
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_MESSAGE_RECEIVED
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_MESSAGE_SEND
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ConversationAdapter(context: Context) : RecyclerView.Adapter<MessageViewHolder>() {

    private val messages: ArrayList<MessageUI> = ArrayList()

    //------------------------------------------
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {

        val context = parent.context

        return when (viewType) {
            TYPE_MESSAGE_SEND -> {
                val view = LayoutInflater.from(context).inflate(
                    R.layout.bubble_msg_send,
                    parent,
                    false
                )
                MessageSendViewHolder(view)
            }
            TYPE_MESSAGE_RECEIVED -> {
                val view = LayoutInflater.from(context).inflate(
                    R.layout.bubble_msg_received,
                    parent,
                    false
                )
                MessageReceivedViewHolder(view)
            }
            TYPE_IMAGE_SEND -> {
                val view = LayoutInflater.from(context).inflate(
                    R.layout.bubble_img_send,
                    parent,
                    false
                )
                ImageSendViewHolder(view)
            }
            TYPE_IMAGE_RECEIVED -> {
                val view = LayoutInflater.from(context).inflate(
                    R.layout.bubble_img_received,
                    parent,
                    false
                )
                ImageReceivedViewHolder(view)
            }

            // TODO -> Add : TYPE_LOCATION_SEND, TYPE_LOCATION_RECEIVED, TYPE_FILE_SEND, TYPE_FILE_RECEIVED

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    //------------------------------------------
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {

        val item = messages[position]

        when (holder) {
            is MessageSendViewHolder -> holder.bind(item)
            is MessageReceivedViewHolder -> holder.bind(item)
            is ImageSendViewHolder -> holder.bind(item)
            is ImageReceivedViewHolder -> holder.bind(item)
            else -> throw IllegalArgumentException()
        }
    }

    //------------------------------------------
    override fun getItemCount(): Int = messages.size

    //------------------------------------------
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]

        when(App.user) {
            message.user ->
                return when (message.messageType) {
                    MessageType.TEXT -> TYPE_MESSAGE_SEND
                    MessageType.IMAGE -> TYPE_IMAGE_SEND
                    MessageType.FILE -> TYPE_FILE_SEND
                    MessageType.LOCATION -> TYPE_LOCATION_SEND
                    else -> throw IllegalArgumentException("Invalid type")
                }
            else ->
                return when(message.messageType) {
                    MessageType.TEXT -> TYPE_MESSAGE_RECEIVED
                    MessageType.IMAGE -> TYPE_IMAGE_RECEIVED
                    MessageType.FILE -> TYPE_FILE_RECEIVED
                    MessageType.LOCATION -> TYPE_LOCATION_RECEIVED
                    else -> throw IllegalArgumentException("Invalid type")
                }
        }
    }

    //------------------------------------------
    class MessageSendViewHolder(view: View) : MessageViewHolder(view) {

        private val messageContent = view.findViewById<TextView>(R.id.message_send)
        private val messageTime = view.findViewById<TextView>(R.id.time_message_send)


        override fun bind(message: MessageUI) {
            messageContent.text = message.content
            messageTime.text = DateUtils.fromMillisToTimeString(message.time)
        }
    }

    //------------------------------------------
    class MessageReceivedViewHolder(view: View) : MessageViewHolder(view) {

        // TODO -> bind the avatar icon to the view
        private val messageContent = view.findViewById<TextView>(R.id.message_received)
        private val messageTime = view.findViewById<TextView>(R.id.time_message_received)
        private val messageUser = view.findViewById<TextView>(R.id.user_message_received)
        //private val messageAvatar = view.findViewById<ImageView>(R.id.avatar_message_received)

        override fun bind(message: MessageUI) {
            messageContent.text = message.content
            messageTime.text = DateUtils.fromMillisToTimeString(message.time)
            messageUser.text = message.user
        }
    }

    //------------------------------------------
    class ImageSendViewHolder(view: View) : MessageViewHolder(view) {

        // TODO -> bind the imageContent to the view
        private val imageContent = view.findViewById<ImageView>(R.id.image_send)
        private val messageTime = view.findViewById<TextView>(R.id.time_image_send)

        override fun bind(message: MessageUI) {
            imageContent.setImageURI(Uri.parse(message.content))
            messageTime.text = DateUtils.fromMillisToTimeString(message.time)
        }
    }

    //------------------------------------------
    class ImageReceivedViewHolder(view: View) : MessageViewHolder(view) {
        // TODO -> bind the avatar icon to the view
        private val imageContent = view.findViewById<ImageView>(R.id.image_received)
        private val messageTime = view.findViewById<TextView>(R.id.time_image_received)
        private val messageUser = view.findViewById<TextView>(R.id.user_image_received)
        //private val messageAvatar = view.findViewById<ImageView>(R.id.avatar_image_received)

        override fun bind(message: MessageUI) {
            imageContent.setImageURI(Uri.parse(message.content))
            messageTime.text = DateUtils.fromMillisToTimeString(message.time)
            messageUser.text = message.user
        }
    }

    //------------------------------------------
    fun addMessage(message: MessageUI){
        messages.add(message)
        notifyDataSetChanged()
    }

    //------------------------------------------
    object DateUtils {
        fun fromMillisToTimeString(millis: Long) : String {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            return format.format(millis)
        }
    }
}

//------------------------------------------
open class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    open fun bind(message: MessageUI) {}
}

//------------------------------------------
class App: Application() {
    companion object {
        lateinit var user:String
    }
}